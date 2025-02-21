package org.ksmt.expr.transformer

import org.ksmt.KContext
import org.ksmt.expr.KAddArithExpr
import org.ksmt.expr.KAndBinaryExpr
import org.ksmt.expr.KAndExpr
import org.ksmt.expr.KApp
import org.ksmt.expr.KArray2Lambda
import org.ksmt.expr.KArray2Select
import org.ksmt.expr.KArray2Store
import org.ksmt.expr.KArray3Lambda
import org.ksmt.expr.KArray3Select
import org.ksmt.expr.KArray3Store
import org.ksmt.expr.KArrayConst
import org.ksmt.expr.KArrayLambda
import org.ksmt.expr.KArrayLambdaBase
import org.ksmt.expr.KArrayNLambda
import org.ksmt.expr.KArrayNSelect
import org.ksmt.expr.KArrayNStore
import org.ksmt.expr.KArraySelect
import org.ksmt.expr.KArraySelectBase
import org.ksmt.expr.KArrayStore
import org.ksmt.expr.KArrayStoreBase
import org.ksmt.expr.KBitVec16Value
import org.ksmt.expr.KBitVec1Value
import org.ksmt.expr.KBitVec32Value
import org.ksmt.expr.KBitVec64Value
import org.ksmt.expr.KBitVec8Value
import org.ksmt.expr.KBitVecCustomValue
import org.ksmt.expr.KBitVecValue
import org.ksmt.expr.KBv2IntExpr
import org.ksmt.expr.KBvAddExpr
import org.ksmt.expr.KBvAddNoOverflowExpr
import org.ksmt.expr.KBvAddNoUnderflowExpr
import org.ksmt.expr.KBvAndExpr
import org.ksmt.expr.KBvArithShiftRightExpr
import org.ksmt.expr.KBvConcatExpr
import org.ksmt.expr.KBvDivNoOverflowExpr
import org.ksmt.expr.KBvExtractExpr
import org.ksmt.expr.KBvLogicalShiftRightExpr
import org.ksmt.expr.KBvMulExpr
import org.ksmt.expr.KBvMulNoOverflowExpr
import org.ksmt.expr.KBvMulNoUnderflowExpr
import org.ksmt.expr.KBvNAndExpr
import org.ksmt.expr.KBvNegNoOverflowExpr
import org.ksmt.expr.KBvNegationExpr
import org.ksmt.expr.KBvNorExpr
import org.ksmt.expr.KBvNotExpr
import org.ksmt.expr.KBvOrExpr
import org.ksmt.expr.KBvReductionAndExpr
import org.ksmt.expr.KBvReductionOrExpr
import org.ksmt.expr.KBvRepeatExpr
import org.ksmt.expr.KBvRotateLeftExpr
import org.ksmt.expr.KBvRotateLeftIndexedExpr
import org.ksmt.expr.KBvRotateRightExpr
import org.ksmt.expr.KBvRotateRightIndexedExpr
import org.ksmt.expr.KBvShiftLeftExpr
import org.ksmt.expr.KBvSignExtensionExpr
import org.ksmt.expr.KBvSignedDivExpr
import org.ksmt.expr.KBvSignedGreaterExpr
import org.ksmt.expr.KBvSignedGreaterOrEqualExpr
import org.ksmt.expr.KBvSignedLessExpr
import org.ksmt.expr.KBvSignedLessOrEqualExpr
import org.ksmt.expr.KBvSignedModExpr
import org.ksmt.expr.KBvSignedRemExpr
import org.ksmt.expr.KBvSubExpr
import org.ksmt.expr.KBvSubNoOverflowExpr
import org.ksmt.expr.KBvSubNoUnderflowExpr
import org.ksmt.expr.KBvToFpExpr
import org.ksmt.expr.KBvUnsignedDivExpr
import org.ksmt.expr.KBvUnsignedGreaterExpr
import org.ksmt.expr.KBvUnsignedGreaterOrEqualExpr
import org.ksmt.expr.KBvUnsignedLessExpr
import org.ksmt.expr.KBvUnsignedLessOrEqualExpr
import org.ksmt.expr.KBvUnsignedRemExpr
import org.ksmt.expr.KBvXNorExpr
import org.ksmt.expr.KBvXorExpr
import org.ksmt.expr.KBvZeroExtensionExpr
import org.ksmt.expr.KConst
import org.ksmt.expr.KDistinctExpr
import org.ksmt.expr.KDivArithExpr
import org.ksmt.expr.KEqExpr
import org.ksmt.expr.KExistentialQuantifier
import org.ksmt.expr.KExpr
import org.ksmt.expr.KFalse
import org.ksmt.expr.KFp128Value
import org.ksmt.expr.KFp16Value
import org.ksmt.expr.KFp32Value
import org.ksmt.expr.KFp64Value
import org.ksmt.expr.KFpAbsExpr
import org.ksmt.expr.KFpAddExpr
import org.ksmt.expr.KFpCustomSizeValue
import org.ksmt.expr.KFpDivExpr
import org.ksmt.expr.KFpEqualExpr
import org.ksmt.expr.KFpFromBvExpr
import org.ksmt.expr.KFpFusedMulAddExpr
import org.ksmt.expr.KFpGreaterExpr
import org.ksmt.expr.KFpGreaterOrEqualExpr
import org.ksmt.expr.KFpIsInfiniteExpr
import org.ksmt.expr.KFpIsNaNExpr
import org.ksmt.expr.KFpIsNegativeExpr
import org.ksmt.expr.KFpIsNormalExpr
import org.ksmt.expr.KFpIsPositiveExpr
import org.ksmt.expr.KFpIsSubnormalExpr
import org.ksmt.expr.KFpIsZeroExpr
import org.ksmt.expr.KFpLessExpr
import org.ksmt.expr.KFpLessOrEqualExpr
import org.ksmt.expr.KFpMaxExpr
import org.ksmt.expr.KFpMinExpr
import org.ksmt.expr.KFpMulExpr
import org.ksmt.expr.KFpNegationExpr
import org.ksmt.expr.KFpRemExpr
import org.ksmt.expr.KFpRoundToIntegralExpr
import org.ksmt.expr.KFpRoundingModeExpr
import org.ksmt.expr.KFpSqrtExpr
import org.ksmt.expr.KFpSubExpr
import org.ksmt.expr.KFpToBvExpr
import org.ksmt.expr.KFpToFpExpr
import org.ksmt.expr.KFpToIEEEBvExpr
import org.ksmt.expr.KFpToRealExpr
import org.ksmt.expr.KFpValue
import org.ksmt.expr.KFunctionApp
import org.ksmt.expr.KFunctionAsArray
import org.ksmt.expr.KGeArithExpr
import org.ksmt.expr.KGtArithExpr
import org.ksmt.expr.KImpliesExpr
import org.ksmt.expr.KInt32NumExpr
import org.ksmt.expr.KInt64NumExpr
import org.ksmt.expr.KIntBigNumExpr
import org.ksmt.expr.KIntNumExpr
import org.ksmt.expr.KInterpretedValue
import org.ksmt.expr.KIsIntRealExpr
import org.ksmt.expr.KIteExpr
import org.ksmt.expr.KLeArithExpr
import org.ksmt.expr.KLtArithExpr
import org.ksmt.expr.KModIntExpr
import org.ksmt.expr.KMulArithExpr
import org.ksmt.expr.KNotExpr
import org.ksmt.expr.KOrBinaryExpr
import org.ksmt.expr.KOrExpr
import org.ksmt.expr.KPowerArithExpr
import org.ksmt.expr.KRealNumExpr
import org.ksmt.expr.KRealToFpExpr
import org.ksmt.expr.KRemIntExpr
import org.ksmt.expr.KSubArithExpr
import org.ksmt.expr.KToIntRealExpr
import org.ksmt.expr.KToRealIntExpr
import org.ksmt.expr.KTrue
import org.ksmt.expr.KUnaryMinusArithExpr
import org.ksmt.expr.KUninterpretedSortValue
import org.ksmt.expr.KUniversalQuantifier
import org.ksmt.expr.KXorExpr
import org.ksmt.sort.KArithSort
import org.ksmt.sort.KArray2Sort
import org.ksmt.sort.KArray3Sort
import org.ksmt.sort.KArrayNSort
import org.ksmt.sort.KArraySort
import org.ksmt.sort.KArraySortBase
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KBv16Sort
import org.ksmt.sort.KBv1Sort
import org.ksmt.sort.KBv32Sort
import org.ksmt.sort.KBv64Sort
import org.ksmt.sort.KBv8Sort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFp128Sort
import org.ksmt.sort.KFp16Sort
import org.ksmt.sort.KFp32Sort
import org.ksmt.sort.KFp64Sort
import org.ksmt.sort.KFpRoundingModeSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KIntSort
import org.ksmt.sort.KRealSort
import org.ksmt.sort.KSort
import org.ksmt.sort.KUninterpretedSort


interface KTransformer : KTransformerBase {
    val ctx: KContext

    override fun transform(expr: KExpr<*>): Any = error("transformer is not implemented for expr $expr")

    fun <T : KSort> transformExpr(expr: KExpr<T>): KExpr<T> = expr

    // function transformers
    override fun <T : KSort> transform(expr: KFunctionApp<T>): KExpr<T> = transformApp(expr)
    override fun <T : KSort> transform(expr: KConst<T>): KExpr<T> = transform(expr as KFunctionApp<T>)
    fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> = with(ctx) {
        val args = expr.args.map { it.accept(this@KTransformer) }

        return if (args == expr.args) {
            transformExpr(expr)
        } else {
            transformExpr(mkApp(expr.decl, args))
        }
    }

    fun <T : KSort> transformValue(expr: KInterpretedValue<T>): KExpr<T> = transformApp(expr)

    // bool transformers
    override fun transform(expr: KAndExpr): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KAndBinaryExpr): KExpr<KBoolSort> = transform(expr as KAndExpr)
    override fun transform(expr: KOrExpr): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KOrBinaryExpr): KExpr<KBoolSort> = transform(expr as KOrExpr)
    override fun transform(expr: KNotExpr): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KImpliesExpr): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KXorExpr): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KTrue): KExpr<KBoolSort> = transformValue(expr)
    override fun transform(expr: KFalse): KExpr<KBoolSort> = transformValue(expr)
    override fun <T : KSort> transform(expr: KEqExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KSort> transform(expr: KDistinctExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KSort> transform(expr: KIteExpr<T>): KExpr<T> = transformApp(expr)

    // bit-vec transformers
    fun <T : KBvSort> transformBitVecValue(expr: KBitVecValue<T>): KExpr<T> = transformValue(expr)
    override fun transform(expr: KBitVec1Value): KExpr<KBv1Sort> = transformBitVecValue(expr)
    override fun transform(expr: KBitVec8Value): KExpr<KBv8Sort> = transformBitVecValue(expr)
    override fun transform(expr: KBitVec16Value): KExpr<KBv16Sort> = transformBitVecValue(expr)
    override fun transform(expr: KBitVec32Value): KExpr<KBv32Sort> = transformBitVecValue(expr)
    override fun transform(expr: KBitVec64Value): KExpr<KBv64Sort> = transformBitVecValue(expr)
    override fun transform(expr: KBitVecCustomValue): KExpr<KBvSort> = transformBitVecValue(expr)

    // bit-vec expressions transformers
    override fun <T : KBvSort> transform(expr: KBvNotExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvReductionAndExpr<T>): KExpr<KBv1Sort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvReductionOrExpr<T>): KExpr<KBv1Sort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvAddExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSubExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvMulExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KBvConcatExpr): KExpr<KBvSort> = transformApp(expr)
    override fun transform(expr: KBvExtractExpr): KExpr<KBvSort> = transformApp(expr)
    override fun transform(expr: KBvSignExtensionExpr): KExpr<KBvSort> = transformApp(expr)
    override fun transform(expr: KBvZeroExtensionExpr): KExpr<KBvSort> = transformApp(expr)
    override fun transform(expr: KBvRepeatExpr): KExpr<KBvSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvRotateLeftIndexedExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): KExpr<T> = transformApp(expr)
    override fun transform(expr: KBv2IntExpr): KExpr<KIntSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvAddNoOverflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvAddNoUnderflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSubNoOverflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvSubNoUnderflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvDivNoOverflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvNegNoOverflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvMulNoOverflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KBvSort> transform(expr: KBvMulNoUnderflowExpr<T>): KExpr<KBoolSort> = transformApp(expr)

    // fp value transformers
    fun <T : KFpSort> transformFpValue(expr: KFpValue<T>): KExpr<T> = transformValue(expr)
    override fun transform(expr: KFp16Value): KExpr<KFp16Sort> = transformFpValue(expr)
    override fun transform(expr: KFp32Value): KExpr<KFp32Sort> = transformFpValue(expr)
    override fun transform(expr: KFp64Value): KExpr<KFp64Sort> = transformFpValue(expr)
    override fun transform(expr: KFp128Value): KExpr<KFp128Sort> = transformFpValue(expr)
    override fun transform(expr: KFpCustomSizeValue): KExpr<KFpSort> = transformFpValue(expr)

    // fp rounding mode
    override fun transform(expr: KFpRoundingModeExpr): KExpr<KFpRoundingModeSort> = transformValue(expr)

    // fp operations tranformation
    override fun <T : KFpSort> transform(expr: KFpAbsExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpNegationExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpAddExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpSubExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpMulExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpDivExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpFusedMulAddExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpSqrtExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpRemExpr<T>): KExpr<T> = transformApp(expr)

    @Suppress("MaxLineLength")
    override fun <T : KFpSort> transform(expr: KFpRoundToIntegralExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpMinExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpMaxExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpLessOrEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpLessExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpGreaterOrEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpGreaterExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpEqualExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsNormalExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsSubnormalExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsZeroExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsInfiniteExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsNaNExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsNegativeExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpIsPositiveExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpToBvExpr<T>): KExpr<KBvSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpToRealExpr<T>): KExpr<KRealSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpToIEEEBvExpr<T>): KExpr<KBvSort> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpFromBvExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KFpToFpExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KRealToFpExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KFpSort> transform(expr: KBvToFpExpr<T>): KExpr<T> = transformApp(expr)

    // array transformers
    fun <A : KArraySortBase<R>, R : KSort> transformArrayStore(expr: KArrayStoreBase<A, R>): KExpr<A> =
        transformApp(expr)

    override fun <D : KSort, R : KSort> transform(expr: KArrayStore<D, R>): KExpr<KArraySort<D, R>> =
        transformArrayStore(expr)

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(
        expr: KArray2Store<D0, D1, R>
    ): KExpr<KArray2Sort<D0, D1, R>> = transformArrayStore(expr)

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Store<D0, D1, D2, R>
    ): KExpr<KArray3Sort<D0, D1, D2, R>> = transformArrayStore(expr)

    override fun <R : KSort> transform(expr: KArrayNStore<R>): KExpr<KArrayNSort<R>> =
        transformArrayStore(expr)

    fun <A : KArraySortBase<R>, R : KSort> transformArraySelect(expr: KArraySelectBase<A, R>): KExpr<R> =
        transformApp(expr)

    override fun <D : KSort, R : KSort> transform(expr: KArraySelect<D, R>): KExpr<R> =
        transformArraySelect(expr)

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(expr: KArray2Select<D0, D1, R>): KExpr<R> =
        transformArraySelect(expr)

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Select<D0, D1, D2, R>
    ): KExpr<R> = transformArraySelect(expr)

    override fun <R : KSort> transform(expr: KArrayNSelect<R>): KExpr<R> =
        transformArraySelect(expr)

    override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KArrayConst<A, R>): KExpr<A> = transformApp(expr)

    override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KFunctionAsArray<A, R>): KExpr<A> =
        transformExpr(expr)

    fun <A : KArraySortBase<R>, R : KSort> transformArrayLambda(expr: KArrayLambdaBase<A, R>): KExpr<A> =
        transformExpr(expr)

    override fun <D : KSort, R : KSort> transform(expr: KArrayLambda<D, R>): KExpr<KArraySort<D, R>> = with(ctx) {
        val body = expr.body.accept(this@KTransformer)
        if (body == expr.body) return transformArrayLambda(expr)
        return transformArrayLambda(mkArrayLambda(expr.indexVarDecl, body))
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(
        expr: KArray2Lambda<D0, D1, R>
    ): KExpr<KArray2Sort<D0, D1, R>> = with(ctx) {
        val body = expr.body.accept(this@KTransformer)
        if (body == expr.body) return transformArrayLambda(expr)
        return transformArrayLambda(mkArrayLambda(expr.indexVar0Decl, expr.indexVar1Decl, body))
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Lambda<D0, D1, D2, R>
    ): KExpr<KArray3Sort<D0, D1, D2, R>> = with(ctx) {
        val body = expr.body.accept(this@KTransformer)
        if (body == expr.body) return transformArrayLambda(expr)
        return transformArrayLambda(mkArrayLambda(expr.indexVar0Decl, expr.indexVar1Decl, expr.indexVar2Decl, body))
    }

    override fun <R : KSort> transform(expr: KArrayNLambda<R>): KExpr<KArrayNSort<R>> = with(ctx) {
        val body = expr.body.accept(this@KTransformer)
        if (body == expr.body) return transformArrayLambda(expr)
        return transformArrayLambda(mkArrayNLambda(expr.indexVarDeclarations, body))
    }

    // arith transformers
    override fun <T : KArithSort> transform(expr: KAddArithExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KMulArithExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KSubArithExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KUnaryMinusArithExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KDivArithExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KPowerArithExpr<T>): KExpr<T> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KLtArithExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KLeArithExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KGtArithExpr<T>): KExpr<KBoolSort> = transformApp(expr)
    override fun <T : KArithSort> transform(expr: KGeArithExpr<T>): KExpr<KBoolSort> = transformApp(expr)

    // integer transformers
    override fun transform(expr: KModIntExpr): KExpr<KIntSort> = transformApp(expr)
    override fun transform(expr: KRemIntExpr): KExpr<KIntSort> = transformApp(expr)
    override fun transform(expr: KToRealIntExpr): KExpr<KRealSort> = transformApp(expr)

    fun transformIntNum(expr: KIntNumExpr): KExpr<KIntSort> = transformValue(expr)
    override fun transform(expr: KInt32NumExpr): KExpr<KIntSort> = transformIntNum(expr)
    override fun transform(expr: KInt64NumExpr): KExpr<KIntSort> = transformIntNum(expr)
    override fun transform(expr: KIntBigNumExpr): KExpr<KIntSort> = transformIntNum(expr)

    // real transformers
    override fun transform(expr: KToIntRealExpr): KExpr<KIntSort> = transformApp(expr)
    override fun transform(expr: KIsIntRealExpr): KExpr<KBoolSort> = transformApp(expr)
    override fun transform(expr: KRealNumExpr): KExpr<KRealSort> = transformValue(expr)

    // quantifier transformers
    override fun transform(expr: KExistentialQuantifier): KExpr<KBoolSort> = with(ctx) {
        val body = expr.body.accept(this@KTransformer)

        return if (body == expr.body) {
            transformExpr(expr)
        } else {
            transformExpr(mkExistentialQuantifier(body, expr.bounds))
        }
    }

    override fun transform(expr: KUniversalQuantifier): KExpr<KBoolSort> = with(ctx) {
        val body = expr.body.accept(this@KTransformer)

        return if (body == expr.body) {
            transformExpr(expr)
        } else {
            transformExpr(mkUniversalQuantifier(body, expr.bounds))
        }
    }

    // uninterpreted sort value
    override fun transform(expr: KUninterpretedSortValue): KExpr<KUninterpretedSort> = transformValue(expr)
}
