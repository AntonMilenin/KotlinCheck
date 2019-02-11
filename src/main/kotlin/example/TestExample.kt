package example

import kotlincheck.Gen
import kotlincheck.Prop.Companion.exists
import kotlincheck.Prop.Companion.forAll
import kotlincheck.Prop.Companion.throws
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException

class TestExample {
    fun testForAll() {
        forAll(Gen.range(0, 50)) { s ->
            s > 0
        }
        forAll(Gen.range(0, 50)) { s ->
            s > 10
        }
        forAll(Gen.range(1, 50)) { s ->
            s > 0
        }
    }
    fun testExists() {
        exists(Gen.range(1, 50)) { s ->
            s > 25
        }
        exists(Gen.range(1, 50)) { s ->
            s > 50
        }
    }


    fun testThrows() {
        throws(ClassCastException::class, Gen.range(1, 50)) { s ->
            s >0
        }
        throws(NumberFormatException::class, Gen.range(1, 50)) {
            "sad".toInt()
        }
    }

    fun testException() {
        throws(ClassCastException::class, Gen.range(1, 50)) {
            throw IllegalArgumentException()
        }
    }
}