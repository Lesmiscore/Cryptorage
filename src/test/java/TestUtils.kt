import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.FileSource
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

val utf8: Charset = StandardCharsets.UTF_8

fun assertEquals(a: ByteArray, b: ByteArray) {
    require(a.contentEquals(b))
}

fun assertEquals(a: Long, b: Long) {
    require(a == b)
}

fun assertTrue(value: Boolean) {
    require(value)
}

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }


fun FileSource.fakeWrap(): Cryptorage {
    return object : Cryptorage {
        override val isReadOnly: Boolean
            get() = this@fakeWrap.isReadOnly

        override fun close() {
            this@fakeWrap.close()
        }

        override fun commit() {
            this@fakeWrap.commit()
        }

        override fun delete(name: String) {
            this@fakeWrap.delete(name)
        }

        override fun gc() {
        }

        override fun lastModified(name: String): Long = this@fakeWrap.lastModified(name)

        override fun list(): List<String> = this@fakeWrap.list()

        override fun meta(key: String): String? = null

        override fun meta(key: String, value: String) {
        }

        override fun mv(from: String, to: String) {
        }

        override fun open(name: String, offset: Int): ByteSource = this@fakeWrap.open(name, offset)

        override fun put(name: String): ByteSink = this@fakeWrap.put(name)

        override fun size(name: String): Long = this@fakeWrap.size(name)
    }
}

fun checkFromIndexSize(offset: Int, size: Int, length: Int): Int {
    if (length or offset or size < 0 || size > length - offset)
        throw IndexOutOfBoundsException( "offset: $offset, size: $size, length: $length")
    return offset
}
fun nullOutputStream(): OutputStream {
    return object : OutputStream() {
        @Volatile
        private var closed: Boolean = false

        @Throws(IOException::class)
        private fun ensureOpen() {
            if (closed) {
                throw IOException("Stream closed")
            }
        }

        @Throws(IOException::class)
        override fun write(b: Int) {
            ensureOpen()
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray, off: Int, len: Int) {
            checkFromIndexSize(off, len, b.size)
            ensureOpen()
        }

        override fun close() {
            closed = true
        }
    }
}