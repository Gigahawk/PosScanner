package com.gigahawk.posscanner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
  val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

  val TRIGGER_MODE = intPreferencesKey("trigger_mode")
  val SCAN_BACKEND = intPreferencesKey("scan_backend")
  val SCAN_SUFFIX = intPreferencesKey("scan_suffix")
  val SCAN_SUFFIX_CUSTOM = stringPreferencesKey("scan_suffix_custom")

  val TEXT_DECODE_ERROR_MODE = intPreferencesKey("text_decode_error_mode")

  val REPORT_DELAY_MS = intPreferencesKey("report_delay_ms")

  val MLKIT_BARCODE_FORMATS = stringSetPreferencesKey("mlkit_barcode_formats")
  val ZXING_BARCODE_FORMATS = stringSetPreferencesKey("zxing_barcode_formats")

  val OUTPUT_FORMAT = stringPreferencesKey("output_format")
}
