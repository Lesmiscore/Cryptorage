import com.nao20010128nao.Cryptorage.combine
import com.nao20010128nao.Cryptorage.newMemoryFileSource
import org.junit.Test

class OtherCryptorageTests {
    @Test
    fun combineReadsSecondFile() {
        val datas = listOf(
                "a" to byteArrayOf(1),
                "b" to byteArrayOf(2),
                "c" to byteArrayOf(3),
                "d" to byteArrayOf(4),
                "e" to byteArrayOf(5),
                "f" to byteArrayOf(6)
        ).map { mapOf(it).newMemoryFileSource().fakeWrap() }.combine()
        assertEquals(datas.open("a").read(), byteArrayOf(1))
        assertEquals(datas.open("b").read(), byteArrayOf(2))
        assertEquals(datas.open("c").read(), byteArrayOf(3))
        assertEquals(datas.open("d").read(), byteArrayOf(4))
        assertEquals(datas.open("e").read(), byteArrayOf(5))
        assertEquals(datas.open("f").read(), byteArrayOf(6))
    }
}