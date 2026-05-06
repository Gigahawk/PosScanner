package com.gigahawk.posscanner

import android.util.Log
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

fun decodeString(
    encoded: ByteArray,
    errorMode: CodecErrorMode = CodecErrorMode.IGNORE,
    charset: Charset = StandardCharsets.UTF_8,
): String {
  Log.d("Utils", "Decoding raw byte array ${encoded.joinToString(" ") { "%02x".format(it) }}")
  Log.d("Utils", "Using error mode $errorMode")
  val bb = ByteBuffer.wrap(encoded)

  return when (errorMode) {
    CodecErrorMode.STRICT -> {
      charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(bb)
          .toString()
    }
    CodecErrorMode.IGNORE -> {
      charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.IGNORE)
          .onUnmappableCharacter(CodingErrorAction.IGNORE)
          .decode(bb)
          .toString()
    }
    CodecErrorMode.REPLACE -> {
      charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE)
          .replaceWith("[?]")
          .decode(bb)
          .toString()
    }
    else -> {
      decodeCustom(encoded, errorMode, charset)
    }
  }
}

fun decodeCustom(
    encoded: ByteArray,
    errorMode: CodecErrorMode,
    charset: Charset = StandardCharsets.UTF_8,
): String {
  val sb = StringBuilder()
  val bb = ByteBuffer.wrap(encoded)
  val decoder =
      charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)

  val tempOut = CharBuffer.allocate(1024)

  while (bb.hasRemaining()) {
    val result = decoder.decode(bb, tempOut, false)
    tempOut.flip()
    sb.append(tempOut)
    tempOut.clear()

    if (result.isError) {
      val b = encoded[bb.position()].toInt() and 0xFF
      val replacement =
          when (errorMode) {
            CodecErrorMode.BACKSLASHREPLACE -> "\\x%02x".format(b)
            CodecErrorMode.SURROGATEESCAPE -> "U+DC%02x".format(b)
            // CodecErrorMode.XMLCHARREFREPLACE -> "&#$b;"
            // CodecErrorMode.NAMEREPLACE -> "\\N{"
            else -> ""
          }
      sb.append(replacement)

      bb.position(bb.position() + 1)
      decoder.reset()
    } else if (result.isUnderflow) {
      break
    }
  }

  decoder.decode(bb, tempOut, true)
  decoder.flush(tempOut)
  tempOut.flip()
  sb.append(tempOut)

  return sb.toString()
}

fun decodeString(input: String, errorMode: CodecErrorMode = CodecErrorMode.IGNORE): String {
  return decodeString(input.toByteArray(), errorMode)
}

fun decodeFormat06(input: String): Map<String, String>? {
  val out = mutableMapOf<String, String>()
  val dataStream = getFormat06DataStream(input) ?: return null
  val segments = dataStream.split("\u001d")
  segments.forEach { segment ->
    val firstAlphaIndex = segment.indexOfFirst { it.isLetter() }

    if (firstAlphaIndex != -1) {
      val key = segment.substring(0, firstAlphaIndex + 1)
      val value = segment.substring(firstAlphaIndex + 1)
      out[key] = value
    } else {
      out[segment] = ""
    }
  }

  return out
}

fun getFormat06DataStream(input: String): String? {
  val header = "[)>\u001e06\u001d"
  val trailer = "\u001e\u0004"
  if (!input.startsWith(header)) return null
  // DigiKey barcodes don't seem to contain the trailer???
  // if (!input.endsWith(trailer)) return null
  return input.substring(header.length).removeSuffix(trailer)
}
