package com.vaultguard.app.di

import com.vaultguard.app.data.repository.SettingsRepositoryImpl
import com.vaultguard.app.data.repository.VaultRepositoryImpl
import com.vaultguard.app.domain.repository.SettingsRepository
import com.vaultguard.app.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        vaultRepositoryImpl: VaultRepositoryImpl
    ): VaultRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
