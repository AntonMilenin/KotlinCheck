package kotlincheck

import kotlin.reflect.KClass

abstract class Prop<T> {
    abstract fun apply(params: GenParams): Result

    operator fun invoke(params: GenParams): Result = apply(params)

    fun classify(p: (T) -> Boolean, input: T?): Status {
        try {
            if (input == null)
                return Undecided()

            return if (p(input))
                True()
            else
                False()
        } catch (e: Throwable) {
            return Exception(e)
        }
    }

    companion object {
        fun <T> forAll(gen: Gen<T>, p: (T) -> Boolean) {
            Test().check(object : Prop<T>() {
                override fun apply(params: GenParams): Result {
                    val input: T? = gen(params)
                    val argList: List<Any> = if (input == null) emptyList() else listOf(input as Any)

                    return Result(classify(p, input), argList)
                }
            })
        }

        fun <T> exists(gen: Gen<T>, p: (T) -> Boolean) {
            Test().check(object : Prop<T>() {
                override fun apply(params: GenParams): Result {
                    val input: T? = gen(params)
                    val argList = if (input == null) emptyList() else listOf(input as Any)

                    val result = Result(classify(p, input), argList)
                    when (result.status) {
                        is True -> result.status = Proof()
                        is False -> result.status = Undecided()
                    }
                    return result
                }
            })
        }


        fun <T : Throwable, G> throws(err: KClass<T>, gen: Gen<G>, p: (G) -> Any) {

            Test().check(object : Prop<G>() {
                override fun apply(params: GenParams): Result {
                    val input: G? = gen(params)
                    val argList = if (input == null) emptyList() else listOf(input as Any)

                    if (input == null)
                        return Result(Undecided(), argList)
                    try {
                        p(input)
                    } catch (e: Throwable) {
                        return if (err.isInstance(e))
                            Result(Proof(), argList)
                        else
                            Result(Exception(e), argList)
                    }
                    return Result(Undecided(), argList)
                }
            })

        }
    }

}

data class Result(
    var status: Status, var args: List<Any> = emptyList(),
    val collected: Set<Any> = emptySet()

//    ,
//    val labels: Set<String> = emptySet()
)

data class Arg<T>(val label: String, val arg: T)

interface Status


class Proof : Status

class True : Status

class False : Status

class Undecided : Status

class Exception(val e: Throwable) : Status {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is Exception -> true
            else -> false
        }
}