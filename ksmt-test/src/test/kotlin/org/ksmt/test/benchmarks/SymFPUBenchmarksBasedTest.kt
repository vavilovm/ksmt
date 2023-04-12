package org.ksmt.test.benchmarks

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.symfpu.FpToBvSolverWrapper
import java.nio.file.Path


class SymFPUBenchmarksBasedTest : BenchmarksBasedTest() {

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolver(name: String, samplePath: Path) = testSolverWrapper(name, samplePath) { ctx ->
        FpToBvSolverWrapper(KZ3Solver(ctx), ctx)
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverZ3(name: String, samplePath: Path) = testSolverWrapper(name, samplePath) { ctx ->
        FpToBvSolverWrapper(KZ3Solver(ctx), ctx)
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverYices(name: String, samplePath: Path) = testSolverWrapper(name, samplePath) { ctx ->
        FpToBvSolverWrapper(KYicesSolver(ctx), ctx)
    }

//./gradlew :ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest.testConverter"
// --no-daemon --continue -PrunBenchmarksBasedTests=true

    companion object {
        @JvmStatic
        fun symfpuTestData(): List<BenchmarkTestArguments> {
            println("Running benchmarks for SymFPU")
            return testData.skipUnsupportedTheories().filter { it.name.contains("QF_FP") }
                .ensureNotEmpty().apply {
                    println("Running ${size} benchmarks")
                }
        }


        private fun List<BenchmarkTestArguments>.skipUnsupportedTheories() = filterNot {
            "LIA" in it.name || "LRA" in it.name || "LIRA" in it.name
        }.filterNot { "NIA" in it.name || "NRA" in it.name || "NIRA" in it.name }

    }
}
