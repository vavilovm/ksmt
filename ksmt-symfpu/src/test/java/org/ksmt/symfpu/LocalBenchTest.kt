package org.ksmt.symfpu

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.expr.KFunctionAsArray
import org.ksmt.expr.transformer.KTransformer
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.runner.KSolverRunnerManager
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.sort.KArraySortBase
import org.ksmt.sort.KSort
import org.ksmt.symfpu.operations.createContext
import org.ksmt.symfpu.solver.SymfpuSolver
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocalBenchTest {
    class SymfpuYicesSolver(ctx: KContext) : SymfpuSolver<KYicesSolverConfiguration>(KYicesSolver(ctx), ctx)

    private fun checkAsArrayDeclsPresentInModel(ctx: KContext, model: KModel) {
        val checker = AsArrayDeclChecker(ctx, model)
        model.declarations.forEach { decl ->
            model.interpretation(decl)?.let { interpretation ->
                interpretation.entries.forEach { it.value.accept(checker) }
                interpretation.default?.accept(checker)
            }
        }
    }

    class AsArrayDeclChecker(override val ctx: KContext, private val model: KModel) : KTransformer {
        override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KFunctionAsArray<A, R>): KExpr<A> {
            println("in transform KFunctionAsArray: $expr")
            val interp = model.interpretation(expr.function)
            println("got interp: $interp")
            assertNotNull(interp, "no interpretation for as-array: $expr")
            return expr
        }
    }

    @Test
    fun testFromBench() = with(createContext()) {
        val name = "QF_ABVFP_query.00817.smt2"
        val content = LocalBenchTest::class.java.getResource("/$name")?.readText() ?: error("no file $name")

        val assertionsAll = KZ3SMTLibParser(this).parse(content)

        SymfpuYicesSolver(this).use { solver ->
            assertionsAll.forEach {
                println("assert")
                solver.assert(it)
            }
            println("check")
            solver.check()
            println("get model")
            val model1 = solver.model()
            println("detach model")
            val model = model1.detach()


            println("check as-array decls")
            checkAsArrayDeclsPresentInModel(this, model)

            println("eval results")
            val res = assertionsAll.map { model.eval(it, true) }
            println("check results")
            res.forEach { assertEquals(trueExpr, it) }
        }
    }


    @Test
    fun testFromBenchWithRunner() = runBlocking {
        with(createContext()) {
            val ctx = this
            KSolverRunnerManager(
                workerPoolSize = 4,
                hardTimeout = 15.seconds,
                workerProcessIdleTimeout = 10.minutes
            ).use { solverManager ->

                val name = "QF_ABVFP_query.00817.smt2"
                val content = LocalBenchTest::class.java.getResource("/$name")?.readText() ?: error("no file $name")

                val ksmtAssertions = KZ3SMTLibParser(this).parse(content)

                solverManager.registerSolver(SymfpuYicesSolver::class, KYicesSolverUniversalConfiguration::class)
                val model = solverManager.createSolver(ctx, SymfpuYicesSolver::class).use { testSolver ->
                    ksmtAssertions.forEach { testSolver.assertAsync(it) }

                    val status = testSolver.checkAsync(15.seconds)

                    assertEquals(KSolverStatus.SAT, status)

                    testSolver.modelAsync()
                }

                println("check as-array decls")
                checkAsArrayDeclsPresentInModel(this, model)

                println("eval results")
                val res = ksmtAssertions.map { model.eval(it, true) }
                println("check results")
                res.forEach { assertEquals(trueExpr, it) }

            }
        }
    }
}

