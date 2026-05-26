package com.fixit.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Separate DataStore for dispute response drafts — keyed by dispute ID. */
private val Context.draftsStore by preferencesDataStore(name = "fixit_dispute_drafts")

@Singleton
class DisputeDraftStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Returns the saved draft text for a given dispute, or null if none. */
    suspend fun loadDraft(disputeId: String): String? =
        context.draftsStore.data
            .map { it[stringPreferencesKey(disputeId)] }
            .first()

    /** Persists [text] as the draft for [disputeId]. */
    suspend fun saveDraft(disputeId: String, text: String) {
        context.draftsStore.edit { it[stringPreferencesKey(disputeId)] = text }
    }

    /** Clears the draft once the response has been successfully submitted. */
    suspend fun clearDraft(disputeId: String) {
        context.draftsStore.edit { it.remove(stringPreferencesKey(disputeId)) }
    }
}