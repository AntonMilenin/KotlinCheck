package kotlincheck

import java.util.*
import kotlin.math.roundToInt
import kotlin.random.asKotlinRandom


class Test {
    companion object {
        var params: Parameters = Parameters(0)
    }

    fun <T> check(
        p: Prop<T>
    ) = check(params, p).print()

    fun <T> check(
        params: Parameters,
        p: Prop<T>
    ): Result {
        val iterations = Math.ceil(params.minSuccessfulTests / params.workers.toDouble())
        val sizeStep = (params.maxSize - params.minSize) / (iterations * params.workers)
        var stop = false

        val genPrms = if (params.seed == null) GenParams(Random().asKotlinRandom())
        else GenParams(Random(params.seed).asKotlinRandom())


        val workerFun: (Int) -> Test.Result = {
            var n = 0  // passed tests
            var d = 0  // discarded tests
            var res: Result? = null

            var isExhausted = d > params.minSuccessfulTests * params.maxDiscardRatio

            while (!stop && res == null && n < iterations && !isExhausted) {
                val size = params.minSize.toDouble() + (sizeStep * (it + (params.workers * (n + d))))
                val propRes = p.apply(GenParams(genPrms.rand, size.roundToInt()))
                val status = propRes.status;
                when (status) {
                    is Undecided -> {
                        d += 1
                        isExhausted = d > params.minSuccessfulTests * params.maxDiscardRatio
                    }
                    is True ->
                        n += 1
                    is Proof -> {
                        n += 1
                        res = Result(Proved(propRes.args), n, d)
                        stop = true
                    }
                    is False -> {
                        n += 1
                        res = Result(Failed(propRes.args), n, d)
                        stop = true
                    }
                    is Exception -> {
                        n += 1
                        res = Result(PropException(propRes.args, status.e), n, d)
                        stop = true
                    }


                }
            }
            res ?: if (isExhausted) Result(Exhausted(), n, d)
            else Result(Passed(), n, d)
        }

        val r = Platform.runWorkers(params, workerFun)
        return r

    }


    data class Parameters(
        val seed: Long?, val minSuccessfulTests: Int = 100

        , val minSize: Int = 0
        , val maxSize: Int = 100
        , val workers: Int = 1
        , val maxDiscardRatio: Double = 1.05
    )

    data class Result(
        val status: Status,
        val succeeded: Int,
        val discarded: Int,
        val time: Long = 0
    ) {

        fun print() {
            when (status) {
                is Proved<*>, is Passed ->
                    println("+ OK, passed $succeeded tests")
                is Exhausted, is Undecided ->
                    println("? Failed to falsify or prove")
                is PropException<*> -> {
                    println("! Failed after $succeeded passed tests with " + status.e::class.qualifiedName)
                    val builder = StringBuilder()
                    builder.append("Arguments: ")
                    status.args.map { builder.append(' ').append(it.toString()) }
                    println(builder)
                }
                is Failed<*> -> {
                    println("! Falsified after $succeeded passed tests")
                    val builder = StringBuilder()
                    builder.append("Arguments: ")
                    status.args.map { builder.append(' ').append(it.toString()) }
                    println(builder)
                }
            }
        }
    }

    class Exhausted : Status
    class Passed : Status
    data class Proved<T>(val args: List<T>) : Status

    data class Failed<T>(val args: List<T>) : Status

    data class PropException<T>(
        val args: List<T>, val e: Throwable
    ) : Status
}

