package com.goodusestudios.shell.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.shellStateDataStore by preferencesDataStore(name = "shell_state")

sealed interface ShellGate {
    data object FullOnboarding : ShellGate
    data object LegalUpdate : ShellGate
    data object Ready : ShellGate
}

class ShellStateStore(private val context: Context) {
    fun gate(legalVersion: Int): Flow<ShellGate> = context.shellStateDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            resolveShellGate(
                onboardingComplete = preferences[ONBOARDING_COMPLETE] == true,
                acceptedLegalVersion = preferences[ACCEPTED_LEGAL_VERSION],
                requiredLegalVersion = legalVersion,
            )
        }

    suspend fun completeOnboarding(legalVersion: Int) {
        context.shellStateDataStore.edit {
            it[ONBOARDING_COMPLETE] = true
            it[ACCEPTED_LEGAL_VERSION] = legalVersion
        }
    }

    suspend fun acceptLegalUpdate(legalVersion: Int) {
        context.shellStateDataStore.edit { it[ACCEPTED_LEGAL_VERSION] = legalVersion }
    }

    suspend fun resetOnboarding() {
        context.shellStateDataStore.edit {
            it[ONBOARDING_COMPLETE] = false
            it.remove(ACCEPTED_LEGAL_VERSION)
        }
    }

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val ACCEPTED_LEGAL_VERSION = intPreferencesKey("accepted_legal_version")
    }
}

fun resolveShellGate(
    onboardingComplete: Boolean,
    acceptedLegalVersion: Int?,
    requiredLegalVersion: Int,
): ShellGate = when {
    !onboardingComplete -> ShellGate.FullOnboarding
    acceptedLegalVersion != requiredLegalVersion -> ShellGate.LegalUpdate
    else -> ShellGate.Ready
}
