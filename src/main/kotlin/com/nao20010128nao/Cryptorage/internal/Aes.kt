package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.forCrypto
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

private fun createCipher(keys: AesKeys, mode: Int): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/Pkcs5Padding")
    val (key, iv) = keys.forCrypto()
    cipher.init(mode, key, iv)
    return cipher
}

internal class AesDecryptorByteSource(private val source: ByteSource, private val keys: AesKeys) : ByteSource() {
    override fun openStream(): InputStream = CipherInputStream(source.openStream(), createCipher(keys, Cipher.DECRYPT_MODE))
}

internal class AesEncryptorByteSink(private val source: ByteSink, private val keys: AesKeys) : ByteSink() {
    override fun openStream(): OutputStream = CipherOutputStream(source.openStream(), createCipher(keys, Cipher.ENCRYPT_MODE))
}
