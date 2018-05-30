package com.nao20010128nao.Cryptorage.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.readOnly

internal class ReadOnlyFileSource(private val base: FileSource) : FileSource {
    override val isReadOnly: Boolean = true

    override fun list(): Array<String> = base.list()
    override fun open(name: String, offset: Int): ByteSource = base.open(name, offset)

    override fun put(name: String): ByteSink = readOnly("FileSource")
    override fun delete(name: String) = readOnly("FileSource")
}