package org.ksmt.symfpu

import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.expr.*
import org.ksmt.expr.transformer.KNonRecursiveTransformer
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.cvc5.KCvc5Solver
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.sort.*
import org.ksmt.utils.cast
import org.ksmt.utils.getValue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

//typealias Fp = KFp16Sort
typealias Fp = KFp32Sort

class FpToBvTransformerTest {
    private fun KContext.createTwoFp32Variables(): Pair<KApp<Fp, *>, KApp<Fp, *>> {
        val a by mkFp32Sort()
        val b by mkFp32Sort()
        return Pair(a, b)
    }

    private fun KContext.zero() = mkFpZero(false, Fp(this))
    private fun KContext.negativeZero() = mkFpZero(true, Fp(this))
    private inline fun <R> withContextAndFp32Variables(block: KContext.(KApp<KFp32Sort, *>, KApp<KFp32Sort, *>) -> R): R =
        with(KContext()) {
            val (a, b) = createTwoFp32Variables()
            block(a, b)
        }

    private inline fun <R> withContextAndFp128Variables(block: KContext.(KApp<KFp128Sort, *>, KApp<KFp128Sort, *>) -> R): R =
        with(KContext(simplificationMode = KContext.SimplificationMode.SIMPLIFY)) {
            val a by mkFp128Sort()
            val b by mkFp128Sort()
            block(a, b)
        }

    private inline fun <R> withContextAndFp64Variables(block: KContext.(KApp<KFp64Sort, *>, KApp<KFp64Sort, *>) -> R): R =
        with(KContext(simplificationMode = KContext.SimplificationMode.SIMPLIFY)) {
            val a by mkFp64Sort()
            val b by mkFp64Sort()
            block(a, b)
        }



    @Test
    fun testFpToRealExpr() = with(KContext()) {
        val a by mkFp16Sort()
        testFpExpr(
            mkFpToRealExpr(a),
            mapOf("a" to a),
        ) { _, _ ->
            trueExpr
        }
    }


    private fun <T : KSort> KContext.checkTransformer(
        transformer: FpToBvTransformer,
        solver: KSolver<*>,
        exprToTransform: KExpr<T>,
        printVars: Map<String, KApp<*, *>>,
        extraAssert: ((KExpr<T>, KExpr<T>) -> KExpr<KBoolSort>)
    ) {

        val applied = transformer.apply(exprToTransform)
        val transformedExpr: KExpr<T> = ((applied as? UnpackedFp<*>)?.toFp() ?: applied).cast()

        val testTransformer = TestTransformerUseBvs(this, transformer.mapFpToBv)
        val toCompare = testTransformer.apply(exprToTransform)


        solver.assert(transformedExpr neq toCompare.cast())

        // check assertions satisfiability with timeout
        println("checking satisfiability...")
        val status =
            solver.checkWithAssumptions(
                listOf(testTransformer.apply(extraAssert(transformedExpr, toCompare))),
                timeout = 200.seconds
            )
        println("status: $status")
        if (status == KSolverStatus.SAT) {
            val model = solver.model()
            val transformed = model.eval(transformedExpr)
            val baseExpr = model.eval(toCompare)
            println("notequal = ${transformed neq baseExpr}")
            val neqEval = model.eval(transformedExpr neq toCompare)
            println("neqEval = $neqEval")

            println("transformed: ${unpackedString(transformed, model)}")
            println("exprToTrans: ${unpackedString(baseExpr, model)}")
            for ((name, expr) in printVars) {
                val bv = transformer.mapFpToBv[expr.cast()]
                val evalUnpacked = unpackedString(fromPackedBv(bv!!, expr.sort.cast()), model)
                println("$name :: $evalUnpacked")
            }
        } else if (status == KSolverStatus.UNKNOWN) {
            println("STATUS == UNKNOWN")
            println(solver.reasonOfUnknown())
        }
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun testFpToBvRoundToIntegralExpr() = with(KContext()) {
        val a by mkFp32Sort()
        val roundingModes = KFpRoundingMode.values()

        roundingModes.forEach {
            testFpExpr(
                mkFpRoundToIntegralExpr(mkFpRoundingModeExpr(it), a),
                mapOf("a" to a),
            )
        }
    }

    private fun KContext.unpackedString(value: KExpr<*>, model: KModel) = if (value.sort is KFpSort) {
        val sb = StringBuilder()
        val fpExpr: KExpr<KFpSort> = value.cast()
        val fpValue = model.eval(fpExpr) as KFpValue
        with(unpack(fpExpr.sort, mkFpToIEEEBvExpr(fpExpr.cast()))) {
            sb.append("uFP sign ")
            model.eval(sign).print(sb)
            sb.append(" ")
            model.eval(unbiasedExponent).print(sb)
            sb.append(" ")
            model.eval(normalizedSignificand).print(sb)

            //nan, inf, zero
            sb.append(" nan=")
            model.eval(isNaN).print(sb)
            sb.append(" inf=")
            model.eval(isInf).print(sb)
            sb.append(" zero=")
            model.eval(isZero).print(sb)


            sb.append("\ntoFp: ")
            model.eval(toFp()).print(sb)

            val packedFloat = mkFpToIEEEBvExpr(fpExpr)
            val pWidth = packedFloat.sort.sizeBits.toInt()
            val exWidth = sort.exponentBits.toInt()

            // Extract
            val packedSignificand = mkBvExtractExpr(pWidth - exWidth - 2, 0, packedFloat)
            val packedExponent = mkBvExtractExpr(pWidth - 2, pWidth - exWidth - 1, packedFloat)
            val sign = bvToBool(mkBvExtractExpr(pWidth - 1, pWidth - 1, packedFloat))
            sb.append("\nFP sign ")
            model.eval(sign).print(sb)
            sb.append(" ")
            model.eval(packedExponent).print(sb)
            sb.append(" ")
            model.eval(packedSignificand).print(sb)
            sb.append("\nbv: ")
            model.eval(packedFloat).print(sb)

            sb.append(" \nactually ${(fpValue as? KFp32Value)?.value ?: (fpValue as? KFp16Value)?.value}}")
//            val unpacked = unpack(fp32Sort, packedFloat)
// vs my ${(model.eval(unpacked.toFp()) as? KFp32Value)?.value}
            sb.toString()
        }
    } else {
        "${model.eval(value)}"
    }

    private fun <T : KSort> KContext.testFpExpr(
        exprToTransform: KExpr<T>,
        printVars: Map<String, KApp<*, *>> = emptyMap(),
        extraAssert: ((KExpr<T>, KExpr<T>) -> KExpr<KBoolSort>) = { _, _ -> trueExpr }
    ) {
        val transformer = FpToBvTransformer(this)

        if (System.getProperty("os.name") == "Mac OS X") {
            KZ3Solver(this)
        } else {
            KCvc5Solver(this)
        }.use { solver ->
            checkTransformer(transformer, solver, exprToTransform, printVars, extraAssert)
        }
    }
}


fun <T : KFpSort> KContext.fromPackedBv(it: KExpr<KBvSort>, sort: T): KExpr<T> {
    return unpack(sort, it).toFp()
}

class TestTransformerUseBvs(ctx: KContext, private val mapFpToBv: Map<KExpr<KFpSort>, KExpr<KBvSort>>) :
    KNonRecursiveTransformer(ctx) {
    override fun <T : KSort> transform(expr: KConst<T>): KExpr<T> = with(ctx) {
        return if (expr.sort is KFpSort) {
            val asFp: KConst<KFpSort> = expr.cast()
            val bv = mapFpToBv[asFp]!!
            unpack(asFp.sort, bv).toFp().cast()
        } else expr
    }
}
