package com.gigahawk.posscanner

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gigahawk.posscanner.ScanBackend.MLKIT
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat as ZXingFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val STATE_STOP_TIMEOUT_MS: Long = 5000

fun <T, R> DataStore<Preferences>.asStateFlow(
    scope: CoroutineScope,
    key: Preferences.Key<T>,
    defaultValue: T,
    transform: (T) -> R,
): StateFlow<R> {
  return this.data
      .map { transform(it[key] ?: defaultValue) }
      .stateIn(
          scope = scope,
          started = SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MS),
          initialValue = transform(defaultValue),
      )
}

fun <T> DataStore<Preferences>.asStateFlow(
    scope: CoroutineScope,
    key: Preferences.Key<T>,
    defaultValue: T,
): StateFlow<T> {
  return this.asStateFlow(scope, key, defaultValue) { it }
}

open class SettingsViewModel(application: Application) : AndroidViewModel(application) {
  private val dataStore = getApplication<Application>().dataStore

  val triggerMode =
      dataStore.asStateFlow(viewModelScope, PreferencesKeys.TRIGGER_MODE, TriggerMode.HOLD.value) {
        TriggerMode.fromInt(it)
      }

  val scanBackend =
      dataStore.asStateFlow(
          viewModelScope,
          PreferencesKeys.SCAN_BACKEND,
          ScanBackend.MLKIT.value,
      ) {
        ScanBackend.fromInt(it)
      }

  val scanSuffix =
      dataStore.asStateFlow(
          viewModelScope,
          PreferencesKeys.SCAN_SUFFIX,
          ScanSuffix.NONE.value,
      ) {
        ScanSuffix.fromInt(it)
      }

  val scanSuffixCustom =
      dataStore.asStateFlow(
          viewModelScope,
          PreferencesKeys.SCAN_SUFFIX_CUSTOM,
          "",
      )

  val reportDelayMs =
      dataStore.asStateFlow(
          viewModelScope,
          PreferencesKeys.REPORT_DELAY_MS,
          10,
      )

  val mlkitBarcodeFormats =
      dataStore.asStateFlow(
          viewModelScope,
          key = PreferencesKeys.MLKIT_BARCODE_FORMATS,
          defaultValue = MlKitBarcodeFormat.entries.map { e -> e.displayName }.toSet(),
      ) {
        it.map { value -> MlKitBarcodeFormat.fromString(value) }.toSet()
      }

  val zxingBarcodeFormats =
      dataStore.asStateFlow(
          viewModelScope,
          key = PreferencesKeys.ZXING_BARCODE_FORMATS,
          defaultValue = ZxingBarcodeFormat.entries.map { e -> e.displayName }.toSet(),
      ) {
        it.map { value -> ZxingBarcodeFormat.fromString(value) }.toSet()
      }

  val outputFormat =
      dataStore.asStateFlow(
          viewModelScope,
          PreferencesKeys.OUTPUT_FORMAT,
          "$(OUT)",
      )
}

enum class TriggerMode(val value: Int, val displayName: String, val description: String) {
  HOLD(0, "Hold", "Scan only when trigger is held down"),
  PRESS(1, "Press", "Scan once when trigger is pressed"),
  CONTINUOUS(2, "Continuous", "Scan continuously"),
  PROMPT(value = 3, "Prompt", "Scan continuously, prompt before sending data");

  val needsTrigger: Boolean
    get() = this == HOLD || this == PRESS

  companion object {
    fun fromInt(value: Int) = entries.find { it.value == value } ?: HOLD
  }
}

enum class ScanBackend(val value: Int, val displayName: String) {
  MLKIT(0, "ML Kit"),
  ZXING(1, "ZXing");

  companion object {
    fun fromInt(value: Int) = entries.find { it.value == value } ?: MLKIT
  }
}

enum class ScanSuffix(val value: Int, val displayName: String, val description: String = "") {
  NONE(0, "None", "No suffix"),
  LF(1, "LF", "Line Feed"),
  CR(2, "CR", "Carriage Return"),
  CRLF(3, "CRLF", "Carriage Return followed by Line Feed"),
  SPACE(4, "Space", ""),
  ENTER(5, "Enter", ""),
  CUSTOM(6, "Custom", "Set your own value");

  companion object {
    fun fromInt(value: Int) = ScanSuffix.entries.find { it.value == value } ?: NONE
  }
}

enum class MlKitBarcodeFormat(val value: Int, val displayName: String) {
  CODE_128(Barcode.FORMAT_CODE_128, "Code 128"),
  CODE_39(Barcode.FORMAT_CODE_39, "Code 39"),
  CODE_93(Barcode.FORMAT_CODE_93, "Code 93"),
  CODABAR(Barcode.FORMAT_CODABAR, "Codabar"),
  DATA_MATRIX(Barcode.FORMAT_DATA_MATRIX, "Data Matrix"),
  EAN_13(Barcode.FORMAT_EAN_13, "EAN-13"),
  EAN_8(Barcode.FORMAT_EAN_8, "EAN-8"),
  ITF(Barcode.FORMAT_ITF, "ITF"),
  QR_CODE(Barcode.FORMAT_QR_CODE, "QR Code"),
  UPC_A(Barcode.FORMAT_UPC_A, "UPC-A"),
  UPC_E(Barcode.FORMAT_UPC_E, "UPC-E"),
  PDF417(Barcode.FORMAT_PDF417, "PDF417"),
  AZTEC(Barcode.FORMAT_AZTEC, "Aztec");

  companion object {
    fun fromString(value: String) = entries.find { it.displayName == value } ?: QR_CODE

    fun toFormats(values: Set<MlKitBarcodeFormat>): Set<Int> = values.map { it.value }.toSet()
  }
}

enum class ZxingBarcodeFormat(val displayName: String, val zxingValue: ZXingFormat) {

  AZTEC("Aztec", ZXingFormat.AZTEC),
  CODABAR("Codabar", ZXingFormat.CODABAR),
  CODE_39("Code 39", ZXingFormat.CODE_39),
  CODE_93("Code 93", ZXingFormat.CODE_93),
  CODE_128("Code 128", ZXingFormat.CODE_128),
  DATA_MATRIX("Data Matrix", ZXingFormat.DATA_MATRIX),
  EAN_8("EAN-8", ZXingFormat.EAN_8),
  EAN_13("EAN-13", ZXingFormat.EAN_13),
  ITF("ITF", ZXingFormat.ITF),
  PDF_417("PDF 417", ZXingFormat.PDF_417),
  QR_CODE("QR Code", ZXingFormat.QR_CODE),
  UPC_A("UPC-A", ZXingFormat.UPC_A),
  UPC_E("UPC-E", ZXingFormat.UPC_E),
  MAXICODE("MaxiCode", ZXingFormat.MAXICODE),
  RSS_14("RSS 14", ZXingFormat.RSS_14),
  RSS_EXPANDED("RSS Expanded", ZXingFormat.RSS_EXPANDED);

  companion object {
    fun fromString(value: String) =
        ZxingBarcodeFormat.entries.find { it.displayName == value } ?: ZxingBarcodeFormat.QR_CODE

    fun toFormats(values: Set<ZxingBarcodeFormat>): Set<ZXingFormat> =
        values.map { it.zxingValue }.toSet()
  }
}
