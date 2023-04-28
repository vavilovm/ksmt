package org.ksmt.symfpu

import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.expr.KFunctionAsArray
import org.ksmt.expr.transformer.KTransformer
import org.ksmt.solver.KModel
import org.ksmt.solver.yices.KYicesSolver
import org.ksmt.solver.yices.KYicesSolverConfiguration
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.z3.KZ3SolverConfiguration
import org.ksmt.sort.KArraySortBase
import org.ksmt.sort.KSort
import org.ksmt.symfpu.operations.createContext
import org.ksmt.symfpu.solver.SymfpuSolver
import org.ksmt.utils.getValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertNotNull

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

    class AsArrayDeclChecker(override val ctx: KContext, val model: KModel) : KTransformer {
        override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KFunctionAsArray<A, R>): KExpr<A> {
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
//        ArraySymfpuTest.SymfpuZ3Solver(this).use { solver ->
            assertionsAll.forEach {
                println(it)
                solver.assert(it)
            }
            solver.check()
            val model = solver.model()

            val res = assertionsAll.map { model.eval(it, true) }
            println(res)
            assert(res.all { it == trueExpr })
            checkAsArrayDeclsPresentInModel(this, model)
        }
    }

    @Test
    fun testFromBench2() = with(createContext()) {
        val rm1 by mkFpRoundingModeSort()
        val rm2 by mkFpRoundingModeSort()
        val lhs = mkRealToFpExpr(fp32Sort, rm1, mkRealNum(0, 1))
        val rhs = mkBvToFpExpr(fp32Sort, rm2, mkBv(0, 32u), true)
        val expr = !(lhs eq rhs)


        ArraySymfpuTest.SymfpuZ3Solver(this).use { solver ->
            solver.assert(expr)
            solver.check()
            val model = solver.model()

            val res = model.eval(expr, true)
            println(res)
            assert(expr == trueExpr)
        }
    }
}
