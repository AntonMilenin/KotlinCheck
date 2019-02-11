package kotlincheck

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlincheck.Test.Result
import kotlincheck.Test.Parameters

class Platform {
    companion object {
        fun runWorkers(params: Parameters, workerFun: (Int) -> Result): Test.Result {
            if (params.workers < 2) return workerFun(0)

            var res = Test.Result(Test.Passed(), 0, 0)
            val channel = Channel<Test.Result>()

            fun mergeResult(r1: Result, r2: Result): Test.Result {

                val (st1, s1, d1, _) = r1
                val (st2, s2, d2, _) = r2
                if (st1 !is Test.Passed && st1 !is Test.Exhausted)
                    return Result(st1, s1 + s2, d1 + d2)
                else if (st2 !is Test.Passed && st2 !is Test.Exhausted)
                    return Result(st2, s1 + s2, d1 + d2)
                else {
                    return if (s1 + s2 >= params.minSuccessfulTests && params.maxDiscardRatio * s1 + s2 >= d1 + d2)
                        Result(Test.Passed(), s1 + s2, d1 + d2)
                    else
                        Result(Test.Exhausted(), s1 + s2, d1 + d2)
                }
            }

            runBlocking<Unit> {
                repeat(params.workers) {
                    async {
                        val result = workerFun(it)
                        channel.send(result)
                    }
                }
                repeat(params.workers) { res = mergeResult(res, channel.receive()) }
            }
            return res
        }
    }

}