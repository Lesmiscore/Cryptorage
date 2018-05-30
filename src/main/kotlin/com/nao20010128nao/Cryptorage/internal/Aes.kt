package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

internal class AesDecryptorByteSource(private val source: ByteSource, private val keys: AesKeys) : ByteSource() {
    override fun openStream(): InputStream {
        val cipher = Cipher.getInstance("AES/CBC/Pkcs5Padding")
        val (key, iv) = keys.forCrypto()
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return CipherInputStream(source.openStream(), cipher)
    }
}

internal class AesEncryptorByteSink(private val source: ByteSink, private val keys: AesKeys) : ByteSink() {
    override fun openStream(): OutputStream {
        val cipher = Cipher.getInstance("AES/CBC/Pkcs5Padding")
        val (key, iv) = keys.forCrypto()
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return CipherOutputStream(source.openStream(), cipher)
    }
}
