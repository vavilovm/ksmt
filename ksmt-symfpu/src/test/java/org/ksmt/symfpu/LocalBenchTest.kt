package org.ksmt.symfpu

import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.symfpu.operations.createContext
import org.ksmt.symfpu.solver.SymfpuSolver
import kotlin.time.Duration.Companion.seconds

typealias Solver = SymfpuYicesSolver
class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)
//class SymfpuZ3Solver(ctx: KContext) : SymfpuSolver<KZ3SolverConfiguration>(KZ3Solver(ctx), ctx)
//typealias Solver = SymfpuZ3Solver


class LocalBenchTest {
    private val name = "QF_FP_rem-has-no-other-solution-100.smt2"
    private val content = LocalBenchTest::class.java.getResource("/$name")?.readText() ?: error("no file $name")

    @Test
    fun testFromBench() = with(createContext()) {
        val assertionsAll = KZ3SMTLibParser(this).parse(content)

        Solver(this).use { solver ->
            println("assertionsAll: ${assertionsAll.size}")
            assertionsAll.forEach {
                println("\nassert: $it")
                solver.assert(it)
            }
            println("check")
            println(solver.check(1.seconds))
        }
    }

}

