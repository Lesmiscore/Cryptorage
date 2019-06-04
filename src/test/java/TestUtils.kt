import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.nio.charset.StandardCharsets
import java.util.*

val utf8 = StandardCharsets.UTF_8!!

fun assertEquals(a: ByteArray, b: ByteArray) {
    require(Arrays.equals(a, b))
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

        override fun list(): Array<String> = this@fakeWrap.list()

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
