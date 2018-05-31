@file:Suppress("NOTHING_TO_INLINE")

package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSource
import com.google.common.io.CharSource
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Stream


internal inline fun CharSequence.utf8Bytes(): ByteArray =
        "$this".toByteArray(StandardCharsets.UTF_8)

internal inline fun ByteArray.leading(n: Int): ByteArray = crop(0, n)

internal inline fun ByteArray.trailing(n: Int): ByteArray = crop(size - n, n)

internal inline fun ByteArray.crop(off: Int, len: Int): ByteArray {
    val result = ByteArray(len)
    System.arraycopy(this, off, result, 0, len)
    return result
}

internal inline fun ByteSource.asCharSource(): CharSource = this.asCharSource(StandardCharsets.UTF_8)

/* Caution: this has no grantee not to be duplicate */
internal inline fun generateRandomName(): String =
        "${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "")


internal inline fun readOnly(what: String): Nothing = throw Error("This $what is read-only.")
internal inline fun unsupported(what: String, op: String): Nothing = throw Error("The $op operation is unsupported by this $what.")
internal inline fun <T, R> Iterable<T>.firstNonNull(func: (T) -> R?): R? {
    for (i in this) {
        return func(i) ?: continue
    }
    return null
}
