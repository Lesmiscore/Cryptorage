package com.nao20010128nao.Cryptorage

import com.nao20010128nao.Cryptorage.cryptorage.CombinedCryptorage
import com.nao20010128nao.Cryptorage.cryptorage.CryptorageImplV1
import com.nao20010128nao.Cryptorage.cryptorage.CryptorageImplV2
import com.nao20010128nao.Cryptorage.cryptorage.ReadOnlyCryptorage
import com.nao20010128nao.Cryptorage.file.*
import java.io.File
import java.net.URL
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


typealias AesKey = ByteArray
typealias AesIv = ByteArray
typealias AesKeys = Pair<AesKey, AesIv>


fun AesKey.toCryptoKey(): SecretKeySpec = SecretKeySpec(this, "AES")
fun AesIv.toCryptoIv(): IvParameterSpec = IvParameterSpec(this)
fun AesKeys.forCrypto(): Pair<SecretKeySpec, IvParameterSpec> = first.toCryptoKey() to second.toCryptoIv()

fun URL.asFileSource(): FileSource = UrlFileSource(this)
fun File.asFileSource(): FileSource = DirectoryFileSource(this)
fun newMemoryFileSource(): FileSource = MemoryFileSource()

fun FileSource.withV1Encryption(password: String): Cryptorage = CryptorageImplV1(this, password)
fun FileSource.withV1Encryption(keys: AesKeys): Cryptorage = CryptorageImplV1(this, keys)

fun FileSource.withV2Encryption(password: String): Cryptorage = CryptorageImplV2(this, password)


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
