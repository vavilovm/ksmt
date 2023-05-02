package org.ksmt.test.benchmarks

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.KContext
import kotlin.io.path.Path
import org.ksmt.expr.KExpr
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.bitwuzla.KBitwuzlaSolver
import org.ksmt.solver.bitwuzla.KBitwuzlaSolverConfiguration
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.sort.KBoolSort
import org.ksmt.symfpu.solver.SymfpuSolver
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

//:ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest" --no-daemon --continue -PrunBenchmarksBasedTests=true
@Execution(ExecutionMode.SAME_THREAD)
class SymFPUBenchmarksBasedTest : BenchmarksBasedTest() {


    @ParameterizedTest(name = "{0}")
    @MethodSource("testData")
    fun testSolver(name: String, samplePath: Path) {
        val solverName = System.getenv("solver") ?: throw IllegalStateException("solver environment variable is not set")
        val solver = mapSolvers[solverName] ?: throw IllegalStateException("solver $solverName is not supported")
        if (testSupported(name, solverName)) {
//            sample name | theory | solver | assert time  | check  time | time | status
            measureKsmtAssertionTime(name, samplePath, solverName, solver)
        }
    }

    private val mapSolvers = mapOf(
        "SymfpuZ3" to ::SymfpuZ3Solver,
        "SymfpuYices" to ::SymfpuYicesSolver,
        "SymfpuBitwuzla" to ::SymfpuBitwuzlaSolver,
        "Z3" to ::KZ3Solver,
        "Bitwuzla" to ::KBitwuzlaSolver,
    )

    private fun testSupported(name: String, solver: String) = when (solver) {
        "SymfpuYices" -> !("QF" !in name || "N" in name)
        "Bitwuzla", "SymfpuBitwuzla" -> !("LIA" in name || "LRA" in name || "LIRA" in name) &&
            !("NIA" in name || "NRA" in name || "NIRA" in name)

        else -> true
    }

    private fun getTheory(name: String) = when {
        "QF_FP" in name -> "QF_FP"
        "QF_BVFP" in name -> "QF_BVFP"
        "QF_ABVFP" in name -> "QF_ABVFP"
        else -> throw IllegalStateException("unknown theory for $name")
    }


//./gradlew :ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest" --no-daemon --continue -PrunBenchmarksBasedTests=true -Psolver=Z3
// --no-daemon --continue -PrunBenchmarksBasedTests=true -Psolver=Z3

    @OptIn(ExperimentalTime::class)
    private fun measureKsmtAssertionTime(
        sampleName: String, samplePath: Path, solverName: String,
        solverConstructor: (ctx: KContext) -> KSolver<*>,
    ) = repeat(5) {
        try {
            println("go $solverName.$sampleName")
            with(KContext()) {
                val assertions: List<KExpr<KBoolSort>> = KZ3SMTLibParser(this).parse(samplePath)
                solverConstructor(this).use { solver ->
                    // force solver initialization
                    solver.push()

                    println("assert")
                    val assertTime = measureNanoTime {
                        assertions.forEach { solver.assert(it) }
                    }
                    println("check")
                    val (status, duration) = measureTimedValue {
                        solver.check(TIMEOUT)
                    }
                    val checkTime = duration.inWholeNanoseconds
                    saveData(sampleName, getTheory(sampleName), solverName, assertTime, checkTime, assertTime + checkTime, status)
                }
            }
        } catch (t: Throwable) {
            System.err.println("THROWS $solverName.$sampleName: ${t.message}")
        }
    }

    private fun saveData(
        sampleName: String, theory: String,
        solverName: String, assertTime: Long,
        checkTime: Long, totalTime: Long,
        status: KSolverStatus,
    ) {
        val data = "$sampleName | $theory | $solverName | $assertTime | $checkTime | $totalTime | $status"
        println(data)
        Path("data.csv").appendText("$data\n")
    }


    companion object {
        private val TIMEOUT = 5.seconds

        @JvmStatic
        fun testData() = testData.filter {
            it.name.startsWith("QF_FP") || it.name.startsWith("QF_BVFP") || it.name.startsWith("QF_ABVFP")
        }.ensureNotEmpty().also { println("QF_FPTestData: ${it.size}") } // 68907

        @BeforeAll
        @JvmStatic
        fun createData() {
            Path("data.csv").createFile()
        }
    }
}


class SymfpuZ3Solver(ctx: KContext) : SymfpuSolver<KZ3SolverConfiguration>(KZ3Solver(ctx), ctx)
class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)
class SymfpuBitwuzlaSolver(ctx: KContext) : SymfpuSolver<KBitwuzlaSolverConfiguration>(KBitwuzlaSolver(ctx), ctx)
