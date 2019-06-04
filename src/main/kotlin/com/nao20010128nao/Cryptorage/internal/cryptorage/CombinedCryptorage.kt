package com.nao20010128nao.Cryptorage.internal.cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.firstNonNull
import com.nao20010128nao.Cryptorage.internal.readOnly

internal class CombinedCryptorage(val cryptorages: List<Cryptorage>) : Cryptorage {
    constructor(vararg cs: Cryptorage) : this(cs.asList())

    override val isReadOnly: Boolean = true
    override fun list(): Array<String> = cryptorages.flatMap { it.list().asList() }.distinct().toTypedArray()
    override fun open(name: String, offset: Int): ByteSource = cryptorages.firstNonNull { it.open(name, offset) }!!
    override fun open(name: String): ByteSource = cryptorages.firstNonNull { it.open(name) }!!
    override fun has(name: String): Boolean = cryptorages.firstNonNull { it.has(name) } == true
    override fun lastModified(name: String): Long = cryptorages.firstNonNull {
        it.size(name).let { aa -> if (aa < 0) null else aa }
    } ?: -1

    override fun size(name: String): Long = cryptorages.firstNonNull {
        it.size(name).let { aa -> if (aa < 0) null else aa }
    } ?: -1

    override fun meta(key: String): String? = cryptorages.firstNonNull { it.meta(key) }
    override fun close() = cryptorages.forEach { it.close() }


    override fun put(name: String): ByteSink = readOnly("Cryptorage")
    override fun mv(from: String, to: String) = readOnly("Cryptorage")
    override fun delete(name: String) = readOnly("Cryptorage")
    override fun meta(key: String, value: String) = readOnly("Cryptorage")
    override fun gc() = readOnly("Cryptorage")
    override fun commit() = readOnly("Cryptorage")
}