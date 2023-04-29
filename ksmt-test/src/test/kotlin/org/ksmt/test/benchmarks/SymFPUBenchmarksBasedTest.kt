package org.ksmt.test.benchmarks

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.KContext
import org.ksmt.solver.bitwuzla.KBitwuzlaSolver
import org.ksmt.solver.bitwuzla.KBitwuzlaSolverConfiguration
import org.ksmt.solver.bitwuzla.KBitwuzlaSolverUniversalConfiguration
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.solver.z3.KZ3SolverUniversalConfiguration
import org.ksmt.symfpu.solver.SymfpuSolver
import java.nio.file.Path

class SymFPUBenchmarksBasedTest : BenchmarksBasedTest() {
    @ParameterizedTest(name = "{0}")
    @Execution(ExecutionMode.CONCURRENT)
    @MethodSource("symfpuTestData")
    fun testSolverZ3Transformed(name: String, samplePath: Path) = testSolver(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(SymfpuZ3Solver::class, KZ3SolverUniversalConfiguration::class)
            createSolver(ctx, SymfpuZ3Solver::class)
        }
    }

    @ParameterizedTest(name = "{0}")
    @Execution(ExecutionMode.CONCURRENT)
    @MethodSource("yicesTestData")
    fun testModelZ3Transformed(name: String, samplePath: Path) = testModelConversion(name, samplePath) { ctx ->
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
    @MethodSource("yicesTestData")
    fun testSolverYicesTransformed(name: String, samplePath: Path) = testSolver(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(SymfpuYicesSolver::class, KYicesSolverUniversalConfiguration::class)
            createSolver(ctx, SymfpuYicesSolver::class)
        }
    }

    @ParameterizedTest(name = "{0}")
    @Execution(ExecutionMode.CONCURRENT)
    @MethodSource("yicesTestData")
    fun testModelYicesTransformed(name: String, samplePath: Path) = testModelConversion(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(SymfpuYicesSolver::class, KYicesSolverUniversalConfiguration::class)
            createSolver(ctx, SymfpuYicesSolver::class)
        }
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverBitwuzlaTransformed(name: String, samplePath: Path) = testSolver(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(SymfpuBitwuzlaSolver::class, KBitwuzlaSolverUniversalConfiguration::class)
            createSolver(ctx, SymfpuBitwuzlaSolver::class)
        }
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverBitwuzla(name: String, samplePath: Path) = testSolver(name, samplePath, KBitwuzlaSolver::class)


//./gradlew :ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest.testSolverZ3Transformed"
// --no-daemon --continue -PrunBenchmarksBasedTests=true

    companion object {


        @JvmStatic
        fun symfpuTestData(): List<BenchmarkTestArguments> {
            return testData.filter {
                "FP" in it.name
            }.ensureNotEmpty()
        }


        @JvmStatic
        fun yicesTestData() = symfpuTestData()
            .filter { "QF_ABVFP" in it.name && "00817" in it.name }
            .ensureNotEmpty()
    }
}


class SymfpuZ3Solver(ctx: KContext) : SymfpuSolver<KZ3SolverConfiguration>(KZ3Solver(ctx), ctx)
class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)
class SymfpuBitwuzlaSolver(ctx: KContext) : SymfpuSolver<KBitwuzlaSolverConfiguration>(KBitwuzlaSolver(ctx), ctx)
