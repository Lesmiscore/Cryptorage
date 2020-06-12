package com.nao20010128nao.Cryptorage.internal.middle

import com.google.common.io.ByteSink
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.readOnly

internal class ReadOnlyFileSource(override val source: FileSource) : FileSource by source, MiddleFileSource {
    override val isReadOnly: Boolean = true

    override fun close() = source.close()
    override fun commit() = source.commit()

    override fun put(name: String): ByteSink = readOnly("FileSource")
    override fun delete(name: String) = readOnly("FileSource")
}