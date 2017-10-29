package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSource
import com.google.common.io.CharSource
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

typealias AesKey = ByteArray
typealias AesIv = ByteArray
typealias AesKeys = Pair<AesKey,AesIv>

fun CharSequence.utf8Bytes():ByteArray=
        "$this".toByteArray(StandardCharsets.UTF_8)
fun ByteArray.takePrimitive(n:Int):ByteArray=
        ByteArrayOutputStream().let {
            it.write(this,0,n)
            it.toByteArray()
        }
fun ByteArray.tailPrimitive(n:Int):ByteArray=
        ByteArrayOutputStream().let {
            it.write(this,size-n,n)
            it.toByteArray()
        }
fun AesKey.toCryptoKey():SecretKeySpec = SecretKeySpec(this,"AES")
fun AesIv.toCryptoIv(): IvParameterSpec = IvParameterSpec(this)
fun AesKeys.forCrypto(): Pair<SecretKeySpec,IvParameterSpec> = Pair(first.toCryptoKey(),second.toCryptoIv())
fun ByteSource.asCharSource(): CharSource = this.asCharSource(StandardCharsets.UTF_8)
