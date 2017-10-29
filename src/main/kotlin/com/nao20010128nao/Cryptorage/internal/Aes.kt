package com.nao20010128nao.Cryptorage.internal

import com.google.common.io.ByteSource
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream

class AesDecryptorByteSource(private var source: ByteSource,private var keys: AesKeys): ByteSource() {
    override fun openStream(): InputStream {
        val cipher=Cipher.getInstance("AES/CBC/Pkcs7Padding")
        val (key,iv) = keys.forCrypto()
        cipher.init(Cipher.DECRYPT_MODE,key,iv)
        return CipherInputStream(source.openStream(),cipher)
    }
}
