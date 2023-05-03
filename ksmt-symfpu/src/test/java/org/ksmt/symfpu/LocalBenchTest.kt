package org.ksmt.symfpu

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.solver.runner.KSolverRunnerManager
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.solver.z3.KZ3SolverUniversalConfiguration
import org.ksmt.symfpu.operations.createContext
import org.ksmt.symfpu.solver.SymfpuSolver
import kotlin.time.Duration.Companion.seconds

typealias Solver = SymfpuYicesSolver
typealias UniversalConfig = KYicesSolverUniversalConfiguration
class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)
//class SymfpuZ3Solver(ctx: KContext) : SymfpuSolver<KZ3SolverConfiguration>(KZ3Solver(ctx), ctx)
//typealias Solver = SymfpuZ3Solver
//typealias UniversalConfig = KZ3SolverUniversalConfiguration

class LocalBenchTest {
    private val name = "QF_FP_rem-has-no-other-solution-100.smt2"
    private val content = LocalBenchTest::class.java.getResource("/$name")?.readText() ?: error("no file $name")

    @Test
    fun testFromBench() = with(createContext()) {
        val assertionsAll = KZ3SMTLibParser(this).parse(content)

        Solver(this).use { solver ->
            println("assertionsAll: ${assertionsAll.size}")
            assertionsAll.forEach {
                solver.assert(it)
            }
            println("check")
            println(solver.check(1.seconds))
        }
    }


    @Test
    fun testFromBenchWithRunner() = runBlocking {
        val ctx = createContext()
        KSolverRunnerManager(
            workerPoolSize = 1,
            hardTimeout = 15.seconds,
            workerProcessIdleTimeout = 15.seconds
        ).use { solverManager ->
            val ksmtAssertions = KZ3SMTLibParser(ctx).parse(content)

            solverManager.registerSolver(Solver::class, UniversalConfig::class)
            solverManager.createSolver(ctx, Solver::class).use { testSolver ->
                ksmtAssertions.forEach { testSolver.assertAsync(it) }
                println("check")
                val status = testSolver.checkAsync(1.seconds)
                println("status: $status")
            }
        }
    }
}

