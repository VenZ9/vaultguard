package com.vaultguard.app.autofill

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.vaultguard.app.R
import com.vaultguard.app.data.local.DatabaseManager
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem
import com.vaultguard.app.domain.repository.VaultRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class VaultGuardAutofillService : AutofillService() {

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var databaseManager: DatabaseManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val parsedForm = AutofillParser.parseStructure(structure)
                Timber.d("Autofill fill request for package: %s, domain: %s", parsedForm.packageName, parsedForm.webDomain)

                // If DB is locked or no password field, evaluate SaveInfo only
                if (!databaseManager.isUnlocked.value) {
                    val response = buildSaveOnlyResponse(parsedForm)
                    callback.onSuccess(response)
                    return@launch
                }

                val targetQuery = parsedForm.webDomain ?: parsedForm.packageName
                val matchingItems = vaultRepository.findMatchingItemsForAutofill(targetQuery).toMutableList()

                // Check custom API key field mappings across all items
                val allItems = vaultRepository.getAllItemsSnapshot()
                val apiMatches = mutableListOf<Pair<VaultItem, AutofillNode>>()
                for (item in allItems) {
                    if (item.type == SecretType.API_KEY && item.apiKey.isNotBlank()) {
                        for (mapping in item.customFieldMappings) {
                            val matchedNode = parsedForm.allFields.find { node ->
                                (mapping.viewId.isNotBlank() && node.viewId == mapping.viewId) ||
                                (mapping.hint.isNotBlank() && node.hint?.contains(mapping.hint, ignoreCase = true) == true)
                            }
                            if (matchedNode != null) {
                                apiMatches.add(item to matchedNode)
                            }
                        }
                    }
                }

                if (matchingItems.isEmpty() && apiMatches.isEmpty()) {
                    val response = buildSaveOnlyResponse(parsedForm)
                    callback.onSuccess(response)
                    return@launch
                }

                val fillResponseBuilder = FillResponse.Builder()

                // 1. Add Login/Password datasets
                for (item in matchingItems) {
                    val remoteView = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.autofill_title, item.name)
                        setTextViewText(
                            R.id.autofill_subtitle,
                            if (item.username.isNotBlank()) item.username else "Saved Credential"
                        )
                    }

                    val datasetBuilder = Dataset.Builder(remoteView)
                    var hasValue = false

                    if (parsedForm.usernameField != null && item.username.isNotBlank()) {
                        datasetBuilder.setValue(
                            parsedForm.usernameField.autofillId,
                            AutofillValue.forText(item.username),
                            remoteView
                        )
                        hasValue = true
                    }

                    if (parsedForm.passwordField != null && item.password.isNotBlank()) {
                        datasetBuilder.setValue(
                            parsedForm.passwordField.autofillId,
                            AutofillValue.forText(item.password),
                            remoteView
                        )
                        hasValue = true
                    }

                    if (hasValue) {
                        fillResponseBuilder.addDataset(datasetBuilder.build())
                    }
                }

                // 2. Add API Key datasets
                for ((item, node) in apiMatches) {
                    val remoteView = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.autofill_title, item.name)
                        setTextViewText(R.id.autofill_subtitle, "Fill API Key")
                    }

                    val dataset = Dataset.Builder(remoteView)
                        .setValue(
                            node.autofillId,
                            AutofillValue.forText(item.apiKey),
                            remoteView
                        )
                        .build()

                    fillResponseBuilder.addDataset(dataset)
                }

                // Attach SaveInfo for capturing new or updated credentials
                attachSaveInfo(fillResponseBuilder, parsedForm)

                callback.onSuccess(fillResponseBuilder.build())
            } catch (e: Exception) {
                Timber.e(e, "Error processing autofill fill request")
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                val parsedForm = AutofillParser.parseStructure(structure)
                val username = parsedForm.usernameField?.textValue ?: ""
                val password = parsedForm.passwordField?.textValue
                    ?: parsedForm.newPasswordField?.textValue
                    ?: ""

                if (password.isNotBlank()) {
                    val serviceName = parsedForm.webDomain?.substringBefore(".")?.replaceFirstChar { it.uppercase() }
                        ?: parsedForm.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }

                    val intent = Intent(this@VaultGuardAutofillService, SaveCredentialActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(SaveCredentialActivity.EXTRA_SERVICE_NAME, serviceName)
                        putExtra(SaveCredentialActivity.EXTRA_USERNAME, username)
                        putExtra(SaveCredentialActivity.EXTRA_PASSWORD, password)
                        putExtra(SaveCredentialActivity.EXTRA_URL_OR_PACKAGE, parsedForm.webDomain ?: parsedForm.packageName)
                    }
                    startActivity(intent)
                    Timber.d("Launched SaveCredentialActivity pop-up for %s", serviceName)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch save dialog from autofill save request")
            } finally {
                callback.onSuccess()
            }
        }
    }

    private fun attachSaveInfo(builder: FillResponse.Builder, parsedForm: ParsedForm) {
        val passwordId = parsedForm.passwordField?.autofillId
            ?: parsedForm.newPasswordField?.autofillId
            ?: return

        val requiredIds = arrayOf(passwordId)
        val saveType = if (parsedForm.isSignup) {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD
        } else {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
        }

        val saveInfoBuilder = SaveInfo.Builder(saveType, requiredIds)
        saveInfoBuilder.setDescription(getString(R.string.autofill_save_prompt))
        parsedForm.usernameField?.autofillId?.let { usernameId ->
            saveInfoBuilder.setOptionalIds(arrayOf(usernameId))
        }

        builder.setSaveInfo(saveInfoBuilder.build())
    }

    private fun buildSaveOnlyResponse(parsedForm: ParsedForm): FillResponse? {
        val passwordId = parsedForm.passwordField?.autofillId
            ?: parsedForm.newPasswordField?.autofillId
            ?: return null

        val builder = FillResponse.Builder()
        attachSaveInfo(builder, parsedForm)
        return builder.build()
    }
}
