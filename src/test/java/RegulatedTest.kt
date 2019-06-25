import com.nao20010128nao.Cryptorage.internal.regulated
import org.junit.Test
import java.io.FilterOutputStream

class RegulatedTest {
    @Test
    fun testWriteLength1() {
        lengthTest(intArrayOf(8192, 8192, 20))
    }

    @Test
    fun testWriteLength2() {
        lengthTest(intArrayOf(8192, 8192))
    }

    @Test
    fun testWriteLength3() {
        lengthTest(intArrayOf(16383))
    }

    fun lengthTest(expectedLengths: IntArray) {
        val iter = expectedLengths.iterator()
        val base = object : FilterOutputStream(nullOutputStream()) {
            override fun write(b: ByteArray?, off: Int, len: Int) {
                println(len)
                require(iter.nextInt() == len)
            }
        }
        val regulated = base.regulated()
        regulated.write(ByteArray(expectedLengths.sum()))
        require(!iter.hasNext())
    }
}