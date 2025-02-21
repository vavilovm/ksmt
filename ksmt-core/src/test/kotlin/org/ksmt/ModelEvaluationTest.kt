package org.ksmt

import org.ksmt.solver.KModel
import org.ksmt.solver.model.KModelImpl
import org.ksmt.utils.getValue
import org.ksmt.utils.sampleValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelEvaluationTest {

    @Test
    fun testModelEvalSimple(): Unit = with(KContext()) {
        val arraySort = mkArraySort(bv32Sort, bv32Sort)
        val array by arraySort
        val idx = 17.toBv(bv32Sort)
        val value = 42.toBv(bv32Sort)
        val baseValue = 137.toBv(bv32Sort)

        val arrayInterp = mkArrayConst(arraySort, baseValue).store(idx, value)
        val exprIdx = array.select(idx)
        val exprBase = array.select(99.toBv(bv32Sort))

        val model = KModelImpl(
            this,
            interpretations = mapOf(
                array.decl to KModel.KFuncInterp(
                    decl = array.decl,
                    vars = emptyList(),
                    entries = emptyList(),
                    default = arrayInterp
                ),
            ),
            uninterpretedSortsUniverses = emptyMap()
        )

        assertEquals(value, model.eval(exprIdx))
        assertEquals(baseValue, model.eval(exprBase))
    }

    @Test
    fun testModelEvalPartialArray(): Unit = with(KContext()) {
        val arraySort = mkArraySort(bv32Sort, bv32Sort)
        val array by arraySort
        val idx = 17.toBv(bv32Sort)
        val value = 42.toBv(bv32Sort)

        val exprIdx = array.select(idx)
        val exprBase = array.select(99.toBv(bv32Sort))

        val tmpDecl = mkFreshFuncDecl("array", bv32Sort, listOf(bv32Sort))
        val tmpInterp = KModel.KFuncInterp(
            decl = tmpDecl,
            vars = listOf(mkFreshConstDecl("idx", bv32Sort)),
            entries = listOf(
                KModel.KFuncInterpEntry(
                    args = listOf(idx),
                    value = value
                )
            ),
            default = null
        )

        val arrayInterp = mkFunctionAsArray(arraySort, tmpDecl)


        val model = KModelImpl(
            this,
            interpretations = mapOf(
                array.decl to KModel.KFuncInterp(
                    decl = array.decl,
                    vars = emptyList(),
                    entries = listOf(),
                    default = arrayInterp
                ),
                tmpDecl to tmpInterp
            ),
            uninterpretedSortsUniverses = emptyMap()
        )

        assertEquals(value, model.eval(exprIdx))

        val defaultBaseValue = bv32Sort.sampleValue()
        assertEquals(defaultBaseValue, model.eval(exprBase))
    }

    @Test
    fun testModelEvalPartialArrayEquality(): Unit = with(KContext()) {
        val arraySort = mkArraySort(bv32Sort, bv32Sort)
        val array1 by arraySort
        val array2 by arraySort
        val idx = 17.toBv(bv32Sort)
        val value = 42.toBv(bv32Sort)

        val arrayEquality = array1 eq array2

        val tmpDecl1 = mkFreshFuncDecl("array1", bv32Sort, listOf(bv32Sort))
        val tmpInterp1 = KModel.KFuncInterp(
            decl = tmpDecl1,
            vars = listOf(mkFreshConstDecl("x", bv32Sort)),
            entries = listOf(
                KModel.KFuncInterpEntry(
                    args = listOf(idx),
                    value = value
                )
            ),
            default = null
        )

        val tmpDecl2 = mkFreshFuncDecl("array2", bv32Sort, listOf(bv32Sort))
        val tmpInterp2 = KModel.KFuncInterp(
            decl = tmpDecl2,
            vars = listOf(mkFreshConstDecl("x", bv32Sort)),
            entries = listOf(
                KModel.KFuncInterpEntry(
                    args = listOf(idx),
                    value = value
                )
            ),
            default = null
        )

        val array1Interp = mkFunctionAsArray(arraySort, tmpDecl1)
        val array2Interp = mkFunctionAsArray(arraySort, tmpDecl2)


        val model = KModelImpl(
            this,
            interpretations = mapOf(
                array1.decl to KModel.KFuncInterp(
                    decl = array1.decl,
                    vars = emptyList(),
                    entries = listOf(),
                    default = array1Interp
                ),
                array2.decl to KModel.KFuncInterp(
                    decl = array2.decl,
                    vars = emptyList(),
                    entries = listOf(),
                    default = array2Interp
                ),
                tmpDecl1 to tmpInterp1,
                tmpDecl2 to tmpInterp2,
            ),
            uninterpretedSortsUniverses = emptyMap()
        )

        assertEquals(trueExpr, model.eval(arrayEquality))
    }

}
