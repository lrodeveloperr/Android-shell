package com.goodusestudios.shell.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

data class ShellPersistentState(
    val successfulActionIds: Set<String> = emptySet(),
    val entitledProductIds: Set<String> = emptySet(),
    val entitlementVerifiedAtEpochMillis: Long = 0,
)

class ShellStateStore(private val context: Context) {
    private val data = context.shellStateDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val state: Flow<ShellPersistentState> = data.map { preferences ->
        ShellPersistentState(
            successfulActionIds = preferences[SUCCESSFUL_ACTION_IDS].orEmpty(),
            entitledProductIds = preferences[ENTITLED_PRODUCT_IDS].orEmpty(),
            entitlementVerifiedAtEpochMillis = preferences[ENTITLEMENT_VERIFIED_AT] ?: 0,
        )
    }

    fun gate(legalVersion: Int): Flow<ShellGate> = data.map { preferences ->
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

    /** Count only unique domain operations after they have completed successfully. */
    suspend fun recordSuccessfulAction(actionId: String, cap: Int) {
        require(actionId.isNotBlank()) { "A stable action ID is required" }
        if (cap < 1) return
        context.shellStateDataStore.edit { preferences ->
            val current = preferences[SUCCESSFUL_ACTION_IDS].orEmpty()
            preferences[SUCCESSFUL_ACTION_IDS] = nextSuccessfulActionIds(current, actionId, cap)
        }
    }

    suspend fun replaceEntitlements(productIds: Set<String>, verifiedAtEpochMillis: Long) {
        context.shellStateDataStore.edit { preferences ->
            if (productIds.isEmpty()) {
                preferences.remove(ENTITLED_PRODUCT_IDS)
                preferences.remove(ENTITLEMENT_VERIFIED_AT)
            } else {
                preferences[ENTITLED_PRODUCT_IDS] = productIds
                preferences[ENTITLEMENT_VERIFIED_AT] = verifiedAtEpochMillis
            }
        }
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
        val SUCCESSFUL_ACTION_IDS = stringSetPreferencesKey("successful_action_ids")
        val ENTITLED_PRODUCT_IDS = stringSetPreferencesKey("entitled_product_ids")
        val ENTITLEMENT_VERIFIED_AT = longPreferencesKey("entitlement_verified_at")
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
