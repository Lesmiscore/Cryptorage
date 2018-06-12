package com.nao20010128nao.Cryptorage.internal.middle

import com.nao20010128nao.Cryptorage.internal.file.FileSource

internal interface MiddleFileSource : FileSource {
    val source: FileSource
    override fun close() {
        source.close()
    }

    override fun commit() {
        source.close()
    }
}