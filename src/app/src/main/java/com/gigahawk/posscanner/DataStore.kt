package com.gigahawk.posscanner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
  val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
  val TRIGGER_MODE = intPreferencesKey("trigger_mode")
  val SCAN_BACKEND = intPreferencesKey("scan_backend")
}
