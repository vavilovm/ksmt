package org.ksmt.test.benchmarks

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.KContext
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.solver.z3.KZ3SolverUniversalConfiguration
import org.ksmt.symfpu.SymfpuSolver
import java.nio.file.Path

class SymfpuZ3Solver(ctx: KContext) : SymfpuSolver<KZ3SolverConfiguration>(KZ3Solver(ctx), ctx)
class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)

class SymFPUBenchmarksBasedTest : BenchmarksBasedTest() {

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverZ3Transformed(name: String, samplePath: Path) = testSolver(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(SymfpuZ3Solver::class, KZ3SolverUniversalConfiguration::class)
            createSolver(ctx, SymfpuZ3Solver::class)
        }
    }


    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverZ3(name: String, samplePath: Path) = testSolver(name, samplePath, KZ3Solver::class)


    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverYices(name: String, samplePath: Path) = testSolver(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(SymfpuYicesSolver::class, KYicesSolverUniversalConfiguration::class)
            createSolver(ctx, SymfpuYicesSolver::class)
        }
    }


//./gradlew :ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest.testConverter"
// --no-daemon --continue -PrunBenchmarksBasedTests=true

    companion object {


        @JvmStatic
        fun symfpuTestData(): List<BenchmarkTestArguments> {
            println("Running benchmarks for SymFPU")
            return testData.skipUnsupportedTheories().filter {
                it.name.contains("QF_FP")
            }.ensureNotEmpty().apply {
                println("Running $size benchmarks")
            }
        }


        private fun List<BenchmarkTestArguments>.skipUnsupportedTheories() = filterNot {
            "LIA" in it.name || "LRA" in it.name || "LIRA" in it.name
        }.filterNot { "NIA" in it.name || "NRA" in it.name || "NIRA" in it.name }
            .filterNot { "ABVFP" in it.name }

    }
}
