import com.nao20010128nao.Cryptorage.internal.sr
import com.nao20010128nao.Cryptorage.internal.toBigInteger
import com.nao20010128nao.Cryptorage.internal.xor
import org.junit.Test

class XorTest {
    @Test
    fun testXorByteArrayBigInteger() {
        val num = ByteArray(32).also {
            sr.nextBytes(it)
        }.toBigInteger()
        val test = ByteArray(32).also {
            sr.nextBytes(it)
        }
        assertNotEquals(test, test xor num)
    }
}
