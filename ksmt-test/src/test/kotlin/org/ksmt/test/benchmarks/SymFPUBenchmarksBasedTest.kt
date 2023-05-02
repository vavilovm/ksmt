package org.ksmt.test.benchmarks

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.solver.KSolver
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.writeLines
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.seconds

//:ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest" --no-daemon --continue -PrunBenchmarksBasedTests=true
@Execution(ExecutionMode.SAME_THREAD)
class SymFPUBenchmarksBasedTest : BenchmarksBasedTest() {

    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testAllSolvers(name: String, samplePath: Path) {
        testSolverZ3(name, samplePath)
        testSolverZ3Transformed(name, samplePath)

        if (!("QF" !in name || "N" in name)) {
            testSolverYicesTransformed(name, samplePath)
        }
        val bitwuzlaConditions = !("LIA" in name || "LRA" in name || "LIRA" in name) &&
            !("NIA" in name || "NRA" in name || "NIRA" in name)

        if (bitwuzlaConditions) {
            testSolverBitwuzlaTransformed(name, samplePath)
            testSolverBitwuzla(name, samplePath)
        }
    }

    private fun testSolverZ3Transformed(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "SymfpuZ3Solver", ::SymfpuZ3Solver
    )

    private fun testSolverZ3(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "Z3Solver", ::KZ3Solver
    )

    private fun testSolverYicesTransformed(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "SymfpuYicesSolver", ::SymfpuYicesSolver
    )

    private fun testSolverBitwuzlaTransformed(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "SymfpuBitwuzlaSolver", ::SymfpuBitwuzlaSolver
    )

    private fun testSolverBitwuzla(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "BitwuzlaSolver", ::KBitwuzlaSolver
    )


//./gradlew :ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest.testSolverZ3Transformed"
// --no-daemon --continue -PrunBenchmarksBasedTests=true


    private fun measureKsmtAssertionTime(
        name: String, samplePath: Path, solverName: String, solverConstructor: (ctx: KContext) -> KSolver<*>,
    ) = try {
        with(KContext()) {
            val assertions: List<KExpr<KBoolSort>> = KZ3SMTLibParser(this).parse(samplePath)
            solverConstructor(this).use { solver ->
                // force solver initialization
                solver.push()

                val assertAndCheck = measureNanoTime {
                    assertions.forEach { solver.assert(it) }
                    solver.check(TIMEOUT)
                }
                saveData(name, solverName, "$assertAndCheck")
            }
        }
    } catch (t: Throwable) {
        System.err.println("THROWS $solverName.$name: ${t.message}")
        System.err.println("$t")
        println("THROWS $solverName.$name: ${t.message}")
    }


    companion object {
        private val TIMEOUT = 5.seconds


        @JvmStatic
        fun symfpuTestData(): List<BenchmarkTestArguments> {
            return testData.filter {
                "FP" in it.name
            }.ensureNotEmpty()
        }


        private val data = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

        private fun saveData(sample: String, type: String, value: String) {
            data.getOrPut(sample) { ConcurrentHashMap() }[type] = value
        }

        @AfterAll
        @JvmStatic
        fun saveData() {
            val headerRow = data.values.firstOrNull()?.keys?.sorted() ?: return
            val columns = listOf("Sample name") + headerRow
            val orderedData = listOf(columns) + data.map { (name, sampleData) ->
                val row = headerRow.map { sampleData[it] ?: "" }
                listOf(name) + row
            }
            val csvData = orderedData.map { it.joinToString(separator = ",") }
            Path("data.csv")
                .writeLines(csvData)
        }
    }
}


class SymfpuZ3Solver(ctx: KContext) : SymfpuSolver<KZ3SolverConfiguration>(KZ3Solver(ctx), ctx)
class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)
class SymfpuBitwuzlaSolver(ctx: KContext) : SymfpuSolver<KBitwuzlaSolverConfiguration>(KBitwuzlaSolver(ctx), ctx)
