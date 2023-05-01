package org.ksmt.symfpu.solver

import org.ksmt.KContext
import org.ksmt.decl.KDecl
import org.ksmt.expr.KApp
import org.ksmt.expr.KArrayConst
import org.ksmt.expr.KArrayLambdaBase
import org.ksmt.expr.KArrayStoreBase
import org.ksmt.expr.KConst
import org.ksmt.expr.KExpr
import org.ksmt.expr.KFunctionAsArray
import org.ksmt.expr.KUninterpretedSortValue
import org.ksmt.expr.transformer.KTransformer
import org.ksmt.solver.KModel
import org.ksmt.solver.model.KModelEvaluator
import org.ksmt.solver.model.KModelImpl
import org.ksmt.sort.KArraySortBase
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort
import org.ksmt.sort.KUninterpretedSort
import org.ksmt.symfpu.operations.pack
import org.ksmt.symfpu.solver.ArraysTransform.Companion.mkAnyArrayLambda
import org.ksmt.symfpu.solver.ArraysTransform.Companion.mkAnyArrayStore
import org.ksmt.utils.cast
import org.ksmt.utils.uncheckedCast

class SymFPUModel(private val kModel: KModel, val ctx: KContext, val transformer: FpToBvTransformer) : KModel {
    private val mapBvToFpDecls
        get() = transformer.mapFpToBvDecl.entries.associateBy({ it.value.decl }) { it.key }

    override val declarations: Set<KDecl<*>>
        get() = (kModel.declarations.map { mapBvToFpDecls[it] ?: it }).toSet()


    override val uninterpretedSorts: Set<KUninterpretedSort>
        get() = kModel.uninterpretedSorts


    private val evaluatorWithModelCompletion by lazy { KModelEvaluator(ctx, this, isComplete = true) }
    private val evaluatorWithoutModelCompletion by lazy { KModelEvaluator(ctx, this, isComplete = false) }
    private val interpretations: MutableMap<KDecl<*>, KModel.KFuncInterp<*>> = hashMapOf()

    override fun <T : KSort> eval(expr: KExpr<T>, isComplete: Boolean): KExpr<T> {
        ctx.ensureContextMatch(expr)

        val evaluator = if (isComplete) evaluatorWithModelCompletion else evaluatorWithoutModelCompletion
        return evaluator.apply(expr)
    }

    override fun <T : KSort> interpretation(decl: KDecl<T>): KModel.KFuncInterp<T>? = with(ctx) {
        ensureContextMatch(decl)
        return interpretations.getOrPut(decl) {
            if (!declContainsFp(decl)) {
                return@getOrPut kModel.interpretation<T>(decl) ?: return@with null
            }

            val const: KConst<*> = transformer.mapFpToBvDecl[decl] ?: return@with null
            val interpretation = kModel.interpretation(const.decl) ?: return null
            val vars: Map<KDecl<*>, KConst<*>> =
                interpretation.vars.zip(decl.argSorts) { v, sort: KSort ->
                    val newConst: KConst<KSort> = mkFreshConst("var", sort).cast()
                    v to newConst
                }.toMap()

            val default = interpretation.default?.let { getInterpretation(decl.sort, it, vars) }
            val entries: List<KModel.KFuncInterpEntry<T>> = interpretation.entries.map {
                val args = it.args.zip(decl.argSorts) { arg, sort ->
                    transformToFpSort(sort, arg.cast(), vars)
                }
                val newValue = transformToFpSort(decl.sort, it.value.cast(), vars)
                KModel.KFuncInterpEntry(args, newValue).cast()
            }

            KModel.KFuncInterp(decl, vars.values.map { it.decl }, entries, default)
        }.cast()
    }


    private fun transformArrayLambda(
        bvLambda: KArrayLambdaBase<*, *>, toSort: KArraySortBase<*>, vars: Map<KDecl<*>, KConst<*>>,
    ): KExpr<*> = with(ctx) {
        val fromSort = bvLambda.sort

        if (fromSort == toSort) {
            return@with bvLambda.uncheckedCast()
        }

        val indices: List<KConst<KSort>> = toSort.domainSorts.map {
            mkFreshConst("i", it).cast()
        }

        val targetFpSort = toSort.range
        val fpValue = transformToFpSort(targetFpSort, bvLambda.body.cast(), vars)

        val replacement: KExpr<KArraySortBase<*>> = mkAnyArrayLambda(
            indices.map { it.decl }, fpValue
        ).uncheckedCast()
        replacement
    }

    private fun <T : KSort> getInterpretation(
        sort: T, const: KExpr<*>, vars: Map<KDecl<*>, KConst<*>>,
    ): KExpr<T> {
        if (const is KApp<*, *>) {
            vars[const.decl]?.let { return it.cast() }
        }
        return when (sort) {
            is KFpSort -> {
                ctx.pack(const.cast(), sort).cast()
            }

            is KArraySortBase<*> -> {
                val array: KExpr<KArraySortBase<*>> = const.cast()
                transformToFpSort(sort, array, vars).cast()
            }

            else -> throw IllegalArgumentException("Unsupported sort: $sort")
        }
    }


    private fun <T : KSort> transformToFpSort(
        targetFpSort: T, bvExpr: KExpr<T>, vars: Map<KDecl<*>, KConst<*>>,
    ): KExpr<T> {
        if (bvExpr is KApp<*, *>) {
            vars[bvExpr.decl]?.let { return it.cast() }
        }
        return when {
            !sortContainsFP(targetFpSort) -> bvExpr

            targetFpSort is KFpSort -> {
                ctx.pack(bvExpr.cast(), targetFpSort.cast()).cast()
            }

            targetFpSort is KArraySortBase<*> -> {
                when (val array: KExpr<KArraySortBase<*>> = bvExpr.cast()) {
                    is KArrayConst<*, *> -> {
                        val transformedValue = transformToFpSort(targetFpSort.range, array.value.cast(), vars)
                        ctx.mkArrayConst(targetFpSort.cast(), transformedValue)
                    }

                    is KArrayStoreBase<*, *> -> {
                        val indices = array.indices.zip(targetFpSort.domainSorts) { bvIndex, fpSort ->
                            transformToFpSort(fpSort, bvIndex, vars)
                        }
                        val value = transformToFpSort(targetFpSort.range, array.value.cast(), vars)
                        val arrayInterpretation = transformToFpSort(targetFpSort, array.array.cast(), vars)
                        ctx.mkAnyArrayStore(arrayInterpretation.cast(), indices, value)
                    }

                    is KArrayLambdaBase<*, *> -> transformArrayLambda(array, targetFpSort, vars)

                    is KFunctionAsArray<*, *> -> {
                        val funcDecl = ctx.mkFreshFuncDecl("f", targetFpSort.range, targetFpSort.domainSorts)
                        ctx.mkFunctionAsArray(targetFpSort.cast(), funcDecl)
                    }

                    else -> throw IllegalArgumentException(
                        "Unsupported array. " +
                            "targetSort: $targetFpSort class: ${array.javaClass} array.sort ${array.sort}")
                }.cast()
            }

            else -> throw IllegalArgumentException("Unsupported sort: $targetFpSort")
        }
    }

    override fun uninterpretedSortUniverse(sort: KUninterpretedSort): Set<KUninterpretedSortValue>? {
        return kModel.uninterpretedSortUniverse(sort)
    }

    class AsArrayDeclInterpreter(override val ctx: KContext, private val model: KModel) : KTransformer {
        override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KFunctionAsArray<A, R>): KExpr<A> {
            model.interpretation(expr.function)
            return expr
        }
    }

    override fun detach(): KModel {
        val asArrayDeclInterpreter = AsArrayDeclInterpreter(ctx, this)
        declarations.forEach { decl ->
            interpretation(decl)?.apply {
                entries.forEach { it.value.accept(asArrayDeclInterpreter) }
                default?.accept(asArrayDeclInterpreter)
            }
        }

        val uninterpretedSortsUniverses = uninterpretedSorts.associateWith {
            uninterpretedSortUniverse(it) ?: error("missed sort universe for $it")
        }

        return KModelImpl(ctx, interpretations.toMap(), uninterpretedSortsUniverses)
    }

    companion object {
        fun sortContainsFP(curSort: KSort): Boolean {
            return when (curSort) {
                is KFpSort -> true
                is KArraySortBase<*> -> curSort.domainSorts.any { sortContainsFP(it) } || sortContainsFP(curSort.range)
                else -> false
            }
        }

        fun <T : KSort> declContainsFp(decl: KDecl<T>) =
            sortContainsFP(decl.sort) || decl.argSorts.any { sortContainsFP(it) }
    }
}
