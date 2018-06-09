@file:Suppress("NOTHING_TO_INLINE")

package com.nao20010128nao.Cryptorage.internal

import com.google.common.collect.Multimap
import com.google.common.io.ByteSource
import com.google.common.io.CharSource
import java.io.FileNotFoundException
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*


internal inline fun String.utf8Bytes(): ByteArray = toByteArray(StandardCharsets.UTF_8)

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

internal inline fun ByteArray.digest(algo: String = "sha-256"): ByteArray = MessageDigest.getInstance(algo).digest(this)
internal inline fun ByteArray.toUUIDHashed(): String = UUID.nameUUIDFromBytes(this).toString()
internal inline fun ByteArray.toBigInteger(): BigInteger = BigInteger(this)

internal inline fun readOnly(what: String): Nothing = throw Error("This $what is read-only.")
internal inline fun unsupported(what: String, op: String): Nothing = throw Error("The $op operation is unsupported by this $what.")
internal inline fun closed(what: String): Nothing = throw Error("This $what is already closed.")
internal inline fun fileNotFound(what: String): Nothing = throw FileNotFoundException(what)

internal inline fun <T, R> Iterable<T>.firstNonNull(func: (T) -> R?): R? {
    for (i in this) {
        return try {
            func(i)
        } catch (e: Throwable) {
            null
        } ?: continue
    }
    return null
}

internal inline fun <K, V> createSizeLimitedMap(size: Int): MutableMap<K, V> {
    return object : LinkedHashMap<K, V>(size) {
        override fun removeEldestEntry(p0: MutableMap.MutableEntry<K, V>?): Boolean = this.size > size
    }
}

internal inline fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal inline fun InputStream.digest(algo: String = "sha-256"): String {
    val md = MessageDigest.getInstance(algo)
    try {
        val buf = ByteArray(8192)
        var r: Int
        while (true) {
            r = read(buf)
            if (r <= 0) {
                break
            }
            md.update(buf, 0, r)
        }
    } finally {
        close()
    }
    return md.digest().toHex()
}

internal inline operator fun <K, V> Multimap<K, V>.set(k: K, v: V) {
    put(k, v)
}
