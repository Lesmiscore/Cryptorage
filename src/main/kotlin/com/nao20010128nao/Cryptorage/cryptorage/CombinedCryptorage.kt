package com.nao20010128nao.Cryptorage.cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.firstNonNull
import com.nao20010128nao.Cryptorage.internal.readOnly

internal class CombinedCryptorage(val cryptorages: List<Cryptorage>) : Cryptorage {
    constructor(vararg cs: Cryptorage) : this(cs.asList())

    override val isReadOnly: Boolean = true
    override fun list(): Array<String> = cryptorages.flatMap { it.list().asList() }.toTypedArray()
    override fun open(name: String, offset: Int): ByteSource = cryptorages.firstNonNull { it.open(name, offset) }!!
    override fun open(name: String): ByteSource = cryptorages.firstNonNull { it.open(name) }!!
    override fun has(name: String): Boolean = cryptorages.firstOrNull { it.has(name) }?.let { true } == true
    override fun lastModified(name: String): Long = cryptorages.firstNonNull { it.lastModified(name) }!!
    override fun size(name: String): Long = cryptorages.firstNonNull { it.size(name) }!!
    override fun meta(key: String): String? = cryptorages.firstNonNull { it.meta(key) }


    override fun put(name: String): ByteSink = readOnly("Cryptorage")
    override fun mv(from: String, to: String) = readOnly("Cryptorage")
    override fun delete(name: String) = readOnly("Cryptorage")
    override fun meta(key: String, value: String) = readOnly("Cryptorage")
    override fun gc() = readOnly("Cryptorage")
}