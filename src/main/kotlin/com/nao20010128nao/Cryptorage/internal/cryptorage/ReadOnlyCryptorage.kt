package com.nao20010128nao.Cryptorage.internal.cryptorage

import com.google.common.io.ByteSink
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.readOnly

internal class ReadOnlyCryptorage(private val base: Cryptorage) : Cryptorage by base {
    override val isReadOnly: Boolean = true
    override fun put(name: String): ByteSink = readOnly("Cryptorage")
    override fun mv(from: String, to: String) = readOnly("Cryptorage")
    override fun delete(name: String) = readOnly("Cryptorage")
    override fun meta(key: String, value: String) = readOnly("Cryptorage")
    override fun gc() = readOnly("Cryptorage")
    override fun commit() = readOnly("Cryptorage")
}