package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

class AesDecryptorByteSource(private var source: ByteSource,private var keys: AesKeys): ByteSource() {
    override fun openStream(): InputStream {
        val cipher=Cipher.getInstance("AES/CBC/Pkcs5Padding")
        val (key,iv) = keys.forCrypto()
        cipher.init(Cipher.DECRYPT_MODE,key,iv)
        return CipherInputStream(source.openStream(),cipher)
    }
}

class AesEncryptorByteSink(private var source: ByteSink, private var keys: AesKeys): ByteSink() {
    override fun openStream(): OutputStream {
        val cipher=Cipher.getInstance("AES/CBC/Pkcs5Padding")
        val (key,iv) = keys.forCrypto()
        cipher.init(Cipher.ENCRYPT_MODE,key,iv)
        return CipherOutputStream(source.openStream(),cipher)
    }
}
