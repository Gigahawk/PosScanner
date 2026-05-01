package com.gigahawk.posscanner

enum class TriggerMode(val value: Int) {
  HOLD(0),
  PRESS(1),
  CONTINUOUS(2),
  PROMPT(value = 3);

  companion object {
    fun fromInt(value: Int) = entries.find { it.value == value } ?: HOLD
  }
}

enum class ScanBackend(val value: Int) {
  MLKIT(0),
  ZXING(1);

  companion object {
    fun fromInt(value: Int) = entries.find { it.value == value } ?: MLKIT
  }
}
