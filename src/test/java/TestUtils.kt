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
