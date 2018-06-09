package com.nao20010128nao.Cryptorage

import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.cryptorage.CombinedCryptorage
import com.nao20010128nao.Cryptorage.cryptorage.CryptorageImplV1
import com.nao20010128nao.Cryptorage.cryptorage.CryptorageImplV2
import com.nao20010128nao.Cryptorage.cryptorage.ReadOnlyCryptorage
import com.nao20010128nao.Cryptorage.file.*
import com.nao20010128nao.Cryptorage.internal.Compressable
import java.io.File
import java.net.URL
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


typealias AesKey = ByteArray
typealias AesIv = ByteArray
typealias AesKeys = Pair<AesKey, AesIv>

/** Converts AesKey to SecretKeySpec for Cipher */
fun AesKey.toCryptoKey(): SecretKeySpec = SecretKeySpec(this, "AES")

/** Converts AesIv to IvParameterSpec for Cipher */
fun AesIv.toCryptoIv(): IvParameterSpec = IvParameterSpec(this)

fun AesKeys.forCrypto(): Pair<SecretKeySpec, IvParameterSpec> = first.toCryptoKey() to second.toCryptoIv()

/** Treats Web directory as FileSource */
fun URL.asFileSource(): FileSource = UrlFileSource(this)

/** Treats file system as FileSource */
fun File.asFileSource(): FileSource = DirectoryFileSource(this)

/** Makes a virtual FileSource on memory */
fun newMemoryFileSource(): FileSource = MemoryFileSource()

/** Makes a virtual FileSource on memory */
fun Map<String, ByteArray>.newMemoryFileSource(): FileSource = MemoryFileSource(toMutableMap())

/** Provides a light, easy-to-use Cryptorage */
fun FileSource.withV1Encryption(password: String): Cryptorage = CryptorageImplV1(this, password)

/** Provides a light, easy-to-use Cryptorage */
fun FileSource.withV1Encryption(keys: AesKeys): Cryptorage = CryptorageImplV1(this, keys)

/** Provides a heavier-but-hard-to-attack Cryptorage */
fun FileSource.withV2Encryption(password: String): Cryptorage = CryptorageImplV2(this, password)

/** Combines Cryptorages as one, not writable */
fun List<Cryptorage>.combine(): Cryptorage = CombinedCryptorage(this)

/** Combines Cryptorages as one, not writable */
operator fun Cryptorage.plus(other: Cryptorage): Cryptorage = CombinedCryptorage(this, other)

/** Converts to non-writable Cryptorage */
fun Cryptorage.asReadOnlyCryptorage(): Cryptorage = ReadOnlyCryptorage(this)

/** Converts to non-writable FileSource */
fun FileSource.asRealOnlyFileSource(): FileSource = ReadOnlyFileSource(this)

/** Copies everything from Cryptorage to Cryptorage */
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

/** Get or set split size for a file */
var Cryptorage.splitSize: Int?
    get() = meta(META_SPLIT_SIZE)?.toIntOrNull()
    set(value) = meta(META_SPLIT_SIZE, "$value")

/** Compresses Cryptorage if it's possible */
fun Cryptorage.compressIfPossible(commit: Boolean = false) {
    if (this is Compressable) {
        doCompress()
        if (commit) {
            commit()
        }
    }
}
