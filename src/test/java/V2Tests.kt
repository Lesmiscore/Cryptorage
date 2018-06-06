import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.cryptorage.CryptorageImplV2
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.digest
import com.nao20010128nao.Cryptorage.internal.utf8Bytes
import com.nao20010128nao.Cryptorage.newMemoryFileSource
import com.nao20010128nao.Cryptorage.withV2Encryption
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class V2Tests {
    private fun FileSource.openForTest(): Cryptorage = withV2Encryption("test")

    @Test
    fun testDeriveKeys() {
        // this code should derive:
        // Key: 494BF360145433D921CAEB533AE8F84E
        // IV:  C585A6E85E0F81640D733FA65039F4F1
        val sha = "Senbonzakura".utf8Bytes().digest()
        val keys = CryptorageImplV2.deriveKeys(sha, 120)
        println(keys.first.toHex())
        println(keys.second.toHex())
        require(keys.first.toHex() == "494BF360145433D921CAEB533AE8F84E")
        require(keys.second.toHex() == "C585A6E85E0F81640D733FA65039F4F1")
    }

    @Test
    fun testSimpleWriting() {
        val cryptorage = newMemoryFileSource().openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testSimpleWriting2() {
        val cryptorage = newMemoryFileSource().openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        val dest = cryptorage.put("test").openBufferedStream()
        for (i in 0..99999)
            dest.write(test)
        dest.close()

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        for (i in 0..99999) {
            var error: Throwable? = null
            try {
                Arrays.fill(test, 0.toByte())
                ByteStreams.readFully(`is`, test)
            } catch (e: Throwable) {
                error = e
            }

            println("$i: ${test.toString(utf8)}")
            if (error != null) {
                throw error
            }
            assertEquals(test, payload.toByteArray())
        }
        `is`.close()
    }

    @Test
    fun testWriteSize() {
        val cryptorage = newMemoryFileSource().openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        val dest = cryptorage.put("test").openBufferedStream()
        for (i in 0..9999)
            dest.write(test)
        dest.close()

        assertEquals((payload.length * 10000).toLong(), cryptorage.size("test"))
    }

    @Test
    fun testOverWrite() {
        val cryptorage = newMemoryFileSource().openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        var dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        sr.nextBytes(test)
        dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testSkip() {
        val cryptorage = newMemoryFileSource().openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        md.update(test, 1000000, test.size - 1000000)
        val hashed = md.digest()

        val `is` = cryptorage.open("test", 1000000).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testReopen() {
        val memory = newMemoryFileSource()
        val cryptorage = memory.openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        var dest = cryptorage.put("file1").openBufferedStream()
        dest.write(test)
        dest.close()
        dest = cryptorage.put("file2").openBufferedStream()
        dest.write(test)
        dest.write(test)
        dest.close()
        assertTrue(cryptorage.list().contains("file1"))
        assertTrue(cryptorage.list().contains("file2"))
        cryptorage.close()

        val cryptorageReopen = memory.openForTest()
        assertTrue(cryptorageReopen.list().contains("file1"))
        assertTrue(cryptorageReopen.list().contains("file2"))
    }
}
