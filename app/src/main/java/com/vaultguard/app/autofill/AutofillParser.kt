package com.vaultguard.app.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

data class AutofillNode(
    val autofillId: AutofillId,
    val viewId: String? = null,
    val hint: String? = null,
    val inputType: Int = 0,
    val textValue: String? = null,
    val hints: List<String> = emptyList()
)

data class ParsedForm(
    val packageName: String,
    val webDomain: String? = null,
    val isSignup: Boolean = false,
    val usernameField: AutofillNode? = null,
    val passwordField: AutofillNode? = null,
    val newPasswordField: AutofillNode? = null,
    val confirmPasswordField: AutofillNode? = null,
    val allFields: List<AutofillNode> = emptyList()
)

object AutofillParser {

    fun parseStructure(structure: AssistStructure): ParsedForm {
        val packageName = structure.activityComponent.packageName
        var webDomain: String? = null

        val allNodes = mutableListOf<AutofillNode>()
        val passwordNodes = mutableListOf<AutofillNode>()
        val newPasswordNodes = mutableListOf<AutofillNode>()
        val usernameNodes = mutableListOf<AutofillNode>()

        val nodeCount = structure.windowNodeCount
        for (i in 0 until nodeCount) {
            val rootNode = structure.getWindowNodeAt(i).rootViewNode
            traverseNode(rootNode) { node ->
                if (webDomain == null && !node.webDomain.isNullOrBlank()) {
                    webDomain = cleanDomain(node.webDomain!!)
                }

                val autofillId = node.autofillId ?: return@traverseNode
                val idEntry = node.idEntry
                val hint = node.hint?.toString()
                val inputType = node.inputType
                val autofillHints = node.autofillHints?.toList() ?: emptyList()
                val text = (if (node.autofillValue?.isText == true) node.autofillValue?.textValue?.toString() else null)
                    ?: node.text?.toString()

                val autofillNode = AutofillNode(
                    autofillId = autofillId,
                    viewId = idEntry,
                    hint = hint,
                    inputType = inputType,
                    textValue = text,
                    hints = autofillHints
                )
                allNodes.add(autofillNode)

                // Detect HTML attributes in web forms (Chrome, Firefox, WebView)
                var isHtmlPassword = false
                var isHtmlNewPassword = false
                var isHtmlUsername = false

                val htmlInfo = node.htmlInfo
                if (htmlInfo != null) {
                    val attrs = htmlInfo.attributes
                    if (attrs != null) {
                        for (attr in attrs) {
                            val attrKey = attr.first?.lowercase() ?: ""
                            val attrVal = attr.second?.lowercase() ?: ""
                            if (attrKey == "type" && attrVal == "password") {
                                isHtmlPassword = true
                            }
                            if (attrKey == "autocomplete") {
                                if (attrVal.contains("new-password")) isHtmlNewPassword = true
                                if (attrVal.contains("current-password")) isHtmlPassword = true
                                if (attrVal.contains("username") || attrVal.contains("email")) isHtmlUsername = true
                            }
                            if (attrKey == "name" || attrKey == "id") {
                                if (attrVal.contains("pass") || attrVal.contains("pwd")) isHtmlPassword = true
                                if (attrVal.contains("user") || attrVal.contains("email") || attrVal.contains("login")) isHtmlUsername = true
                            }
                        }
                    }
                }

                // Detect Field types
                val isNewPassword = isHtmlNewPassword ||
                        hasHint(autofillHints, "newPassword", "new_password") ||
                        containsAny(idEntry, "new_password", "newpassword", "signup_password") ||
                        containsAny(hint, "new password", "create password")

                val isConfirmPassword = containsAny(idEntry, "confirm_password", "confirmpassword", "repeat_password") ||
                        containsAny(hint, "confirm password", "repeat password", "re-enter password")

                val isPassword = isHtmlPassword ||
                        hasHint(autofillHints, View.AUTOFILL_HINT_PASSWORD) ||
                        isPasswordInputType(inputType) ||
                        containsAny(idEntry, "password", "passwd", "pwd") ||
                        containsAny(hint, "password", "passcode")

                val isUsername = isHtmlUsername ||
                        hasHint(autofillHints, View.AUTOFILL_HINT_USERNAME, View.AUTOFILL_HINT_EMAIL_ADDRESS) ||
                        isEmailInputType(inputType) ||
                        containsAny(idEntry, "username", "user", "login", "email") ||
                        containsAny(hint, "username", "email", "phone")

                when {
                    isNewPassword -> newPasswordNodes.add(autofillNode)
                    isConfirmPassword -> passwordNodes.add(autofillNode)
                    isPassword -> passwordNodes.add(autofillNode)
                    isUsername -> usernameNodes.add(autofillNode)
                }
            }
        }

        val isSignup = newPasswordNodes.isNotEmpty() || passwordNodes.size >= 2
        val chosenUsername = usernameNodes.firstOrNull()
        val chosenPassword = newPasswordNodes.firstOrNull() ?: passwordNodes.firstOrNull()
        val confirmPassword = if (passwordNodes.size >= 2) passwordNodes[1] else null

        return ParsedForm(
            packageName = packageName,
            webDomain = webDomain,
            isSignup = isSignup,
            usernameField = chosenUsername,
            passwordField = chosenPassword,
            newPasswordField = newPasswordNodes.firstOrNull(),
            confirmPasswordField = confirmPassword,
            allFields = allNodes
        )
    }

    private fun traverseNode(node: AssistStructure.ViewNode, action: (AssistStructure.ViewNode) -> Unit) {
        action(node)
        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), action)
        }
    }

    private fun hasHint(hints: List<String>, vararg targetHints: String): Boolean {
        return hints.any { hint -> targetHints.any { it.equals(hint, ignoreCase = true) } }
    }

    private fun containsAny(source: String?, vararg targets: String): Boolean {
        if (source.isNullOrBlank()) return false
        val lower = source.lowercase()
        return targets.any { lower.contains(it) }
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun isEmailInputType(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    private fun cleanDomain(domain: String): String {
        var clean = domain.trim().lowercase()
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        return clean.substringBefore("/").substringBefore(":")
    }
}
