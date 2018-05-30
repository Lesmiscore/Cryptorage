@file:Suppress("NOTHING_TO_INLINE")

package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSource
import com.google.common.io.CharSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.cryptorage.CombinedCryptorage
import com.nao20010128nao.Cryptorage.cryptorage.CryptorageImplV1
import com.nao20010128nao.Cryptorage.cryptorage.ReadOnlyCryptorage
import com.nao20010128nao.Cryptorage.file.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Stream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


typealias AesKey = ByteArray
typealias AesIv = ByteArray
typealias AesKeys = Pair<AesKey, AesIv>

fun CharSequence.utf8Bytes(): ByteArray =
        "$this".toByteArray(StandardCharsets.UTF_8)

fun ByteArray.takePrimitive(n: Int): ByteArray =
        ByteArrayOutputStream().let {
            it.write(this, 0, n)
            it.toByteArray()
        }

fun ByteArray.tailPrimitive(n: Int): ByteArray =
        ByteArrayOutputStream().let {
            it.write(this, size - n, n)
            it.toByteArray()
        }

fun ByteArray.crop(off: Int, len: Int): ByteArray =
        ByteArrayOutputStream().let {
            it.write(this, off, len)
            it.toByteArray()
        }

fun AesKey.toCryptoKey(): SecretKeySpec = SecretKeySpec(this, "AES")
fun AesIv.toCryptoIv(): IvParameterSpec = IvParameterSpec(this)
fun AesKeys.forCrypto(): Pair<SecretKeySpec, IvParameterSpec> = Pair(first.toCryptoKey(), second.toCryptoIv())
fun ByteSource.asCharSource(): CharSource = this.asCharSource(StandardCharsets.UTF_8)

/* Caution: this has no grantee not to be duplicate */
fun generateRandomName(): String =
        UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "")

fun URL.asFileSource(): FileSource = UrlFileSource(this)
fun File.asFileSource(): FileSource = DirectoryFileSource(this)
fun newMemoryFileSource(): FileSource = MemoryFileSource()

fun FileSource.withV1Encryption(password: String): Cryptorage = CryptorageImplV1(this, password)
fun FileSource.withV1Encryption(keys: AesKeys): Cryptorage = CryptorageImplV1(this, keys)

fun <T> Iterator<T>.enumeration(): Enumeration<T> = object : Enumeration<T> {
    override fun hasMoreElements(): Boolean = hasNext()
    override fun nextElement(): T = next()
}

fun <T> Collection<T>.enumeration(): Enumeration<T> = iterator().enumeration()
fun <T> Stream<T>.enumeration(): Enumeration<T> = iterator().enumeration()

fun List<Cryptorage>.combine(): Cryptorage = CombinedCryptorage(this)
operator fun Cryptorage.plus(other: Cryptorage): Cryptorage = CombinedCryptorage(this, other)

fun Cryptorage.asReadOnlyCryptorage(): Cryptorage = ReadOnlyCryptorage(this)
fun FileSource.asRealOnlyFileSource(): FileSource = ReadOnlyFileSource(this)

fun Cryptorage.copyTo(to: Cryptorage): CopyResult {
    require(!to.isReadOnly)
    val files = list()
    var totalBytes: Long = 0
    files.forEach {
        totalBytes += open(it).copyTo(to.put(it))
    }
    return CopyResult(files.size.toLong(), totalBytes)
}

data class CopyResult(val files: Long, val totalBytes: Long)

internal inline fun readOnly(what: String): Nothing = throw Error("This $what is read-only.")
internal inline fun unsupported(what: String, op: String): Nothing = throw Error("The $op operation is unsupported by this $what.")
internal inline fun <T, R> Iterable<T>.firstNonNull(func: (T) -> R?): R? {
    for (i in this) {
        return func(i) ?: continue
    }
    return null
}
