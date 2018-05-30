package com.nao20010128nao.Cryptorage.cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.readOnly

internal class ReadOnlyCryptorage(private val base: Cryptorage) : Cryptorage {
    override val isReadOnly: Boolean = true

    override fun list(): Array<String> = base.list()
    override fun open(name: String, offset: Int): ByteSource =base. open(name, offset)
    override fun lastModified(name: String): Long = base.lastModified(name)
    override fun size(name: String): Long = base.size(name)
    override fun meta(key: String): String? = base.meta(key)


    override fun put(name: String): ByteSink = readOnly("Cryptorage")
    override fun mv(from: String, to: String) = readOnly("Cryptorage")
    override fun delete(name: String) = readOnly("Cryptorage")
    override fun meta(key: String, value: String) = readOnly("Cryptorage")
    override fun gc() = readOnly("Cryptorage")
}