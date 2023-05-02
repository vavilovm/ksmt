package org.ksmt.test.benchmarks

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.KContext
import org.ksmt.solver.KSolver
import org.ksmt.solver.bitwuzla.KBitwuzlaSolver
import org.ksmt.solver.bitwuzla.KBitwuzlaSolverConfiguration
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.symfpu.solver.SymfpuSolver
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.writeLines
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.seconds

@Execution(ExecutionMode.SAME_THREAD)
@Timeout(10, unit = TimeUnit.SECONDS)
class SymFPUBenchmarksBasedTest : BenchmarksBasedTest() {
    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverZ3Transformed(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "SymfpuZ3Solver", ::SymfpuZ3Solver
    )


    @ParameterizedTest(name = "{0}")
    @MethodSource("symfpuTestData")
    fun testSolverZ3(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "Z3Solver", ::KZ3Solver
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("yicesTestData")
    fun testSolverYicesTransformed(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "SymfpuYicesSolver", ::SymfpuYicesSolver
    )


    @ParameterizedTest(name = "{0}")
    @MethodSource("bitwuzlaTestData")
    fun testSolverBitwuzlaTransformed(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "SymfpuBitwuzlaSolver", ::SymfpuBitwuzlaSolver
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("bitwuzlaTestData")
    fun testSolverBitwuzla(name: String, samplePath: Path) = measureKsmtAssertionTime(
        name, samplePath, "BitwuzlaSolver", ::KBitwuzlaSolver
    )


//./gradlew :ksmt-test:test --tests "org.ksmt.test.benchmarks.SymFPUBenchmarksBasedTest.testSolverZ3Transformed"
// --no-daemon --continue -PrunBenchmarksBasedTests=true

    private inline fun ignoreExceptions(block: () -> Unit) = try {
        block()
    } catch (t: Throwable) {
        System.err.println(t.toString())
    }


    private fun measureKsmtAssertionTime(
        name: String, samplePath: Path, solverName: String, solverConstructor: (ctx: KContext) -> KSolver<*>,
    ) = ignoreExceptions {
        with(KContext()) {
            val assertions = KZ3SMTLibParser(this).parse(samplePath)
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
    }


    companion object {
        private val TIMEOUT = 5.seconds


        @JvmStatic
        fun symfpuTestData(): List<BenchmarkTestArguments> {
            return testData.filter {
                "FP" in it.name
            }.ensureNotEmpty()
        }


        @JvmStatic
        fun yicesTestData() = symfpuTestData()
            .filterNot { "QF" !in it.name || "N" in it.name }
            .ensureNotEmpty()

        @JvmStatic
        fun bitwuzlaTestData() = symfpuTestData()
            .filterNot { "LIA" in it.name || "LRA" in it.name || "LIRA" in it.name }
            .filterNot { "NIA" in it.name || "NRA" in it.name || "NIRA" in it.name }
            .ensureNotEmpty()


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
