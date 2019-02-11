package kotlincheck

import kotlin.random.Random


abstract class Gen<T> {
    abstract fun apply(params: GenParams): T?
    operator fun invoke(params: GenParams): T? = apply(params)

    fun pureApply(params: GenParams, retries: Int = 100): T? {
        if (retries == 0)
            return null

        return apply(params) ?: pureApply(params, retries - 1)
    }

    fun <U> map(f: (T) -> (U)): Gen<U> = object : Gen<U>() {
        override fun apply(params: GenParams): U? {
            val supperResult: T = this@Gen.apply(params) ?: return null
            return f(supperResult)
        }
    }

    private fun filter(predicate: (T) -> Boolean): Gen<T> = object : Gen<T>() {
        override fun apply(params: GenParams): T? {
            val supperResult: T = this@Gen.apply(params) ?: return null
            return if (predicate(supperResult))
                supperResult
            else
                null
        }
    }

    fun take(n: Int): Gen<T> = object : Gen<T>() {
        val values: ArrayList<T?> = ArrayList(n)

        fun fillValues(params: GenParams, num: Int) {
            if (num == 0)
                return
            values.add(this@Gen.apply(params))
            fillValues(params, num - 1)
        }


        override fun apply(params: GenParams): T? {
            if (n <= 0)
                return null
            if (values.isEmpty())
                fillValues(params, n)

            return values.random(params.rand)
        }
    }


    companion object {
        fun <T> oneOf(values: Collection<T>): Gen<T> {
            return object : Gen<T>() {
                override fun apply(params: GenParams): T? {
                    if (values.isEmpty())
                        return null
                    return values.random(params.rand)
                }
            }
        }

        fun range(low: Int, high: Int): Gen<Int> {
            return object : Gen<Int>() {
                override fun apply(params: GenParams): Int? {
                    return params.rand.nextInt(low, high)
                }
            }
        }

        fun range(low: Long, high: Long): Gen<Long> {
            return object : Gen<Long>() {
                override fun apply(params: GenParams): Long? {
                    return params.rand.nextLong(low, high)
                }
            }
        }

        fun range(low: Double, high: Double): Gen<Double> {
            return object : Gen<Double>() {
                override fun apply(params: GenParams): Double? {
                    return params.rand.nextDouble(low, high)
                }
            }
        }
    }
}

data class GenParams(val rand: Random, val size: Int = 100)