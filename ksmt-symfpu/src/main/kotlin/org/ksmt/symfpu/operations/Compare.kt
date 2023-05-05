package org.ksmt.symfpu.operations

import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KFpSort
import org.ksmt.symfpu.operations.UnpackedFp.Companion.iteOp

// doesn't matter if significand is normalized or not
internal fun <Fp : KFpSort> KContext.less(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
): KExpr<KBoolSort> {
    val infCase = (left.isNegativeInfinity and !right.isNegativeInfinity) or
        (!left.isPositiveInfinity and right.isPositiveInfinity)

    val zeroCase =
        (left.isZero and !right.isZero and !right.isNegative) or (!left.isZero and left.isNegative and right.isZero)


    return lessHelper(
        left, right, infCase, zeroCase, positiveCaseSignificandComparison = mkBvUnsignedLessExpr(
        left.getSignificand(), right.getSignificand()
    ), negativeCaseSignificandComparison = mkBvUnsignedLessExpr(
        right.getSignificand(), left.getSignificand()
    )
    )
}


internal fun <Fp : KFpSort> KContext.lessOrEqual(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
): KExpr<KBoolSort> {
    val infCase = (left.isInf and right.isInf and (left.isNegative eq right.isNegative)) or
        left.isNegativeInfinity or right.isPositiveInfinity


    val zeroCase = (left.isZero and right.isZero) or
        (left.isZero and right.isNegative.not()) or (left.isNegative and right.isZero)


    return lessHelper(
        left,
        right,
        infCase,
        zeroCase,
        mkBvUnsignedLessOrEqualExpr(left.getSignificand(), right.getSignificand()),
        mkBvUnsignedLessOrEqualExpr(right.getSignificand(), left.getSignificand()),
    )
}

// common logic for less and lessOrEqual
@Suppress("LongParameterList")
private fun <Fp : KFpSort> KContext.lessHelper(
    left: UnpackedFp<Fp>,
    right: UnpackedFp<Fp>,
    infCase: KExpr<KBoolSort>,
    zeroCase: KExpr<KBoolSort>,
    positiveCaseSignificandComparison: KExpr<KBoolSort> = mkBvUnsignedLessExpr(
        left.getSignificand(), right.getSignificand()
    ),
    negativeCaseSignificandComparison: KExpr<KBoolSort> = mkBvUnsignedLessExpr(
        right.getSignificand(), left.getSignificand()
    ),
): KExpr<KBoolSort> {
    val neitherNan = !left.isNaN and !right.isNaN

    // Infinities are bigger than everything but themselves
    val eitherInf = left.isInf or right.isInf

    // Both zero are equal
    val eitherZero = left.isZero or right.isZero

    // Normal and subnormal
    val negativeLessThanPositive = left.isNegative and !right.isNegative
    val positiveCase = !left.isNegative and !right.isNegative and (mkBvSignedLessExpr(
        left.getExponent(), right.getExponent()
    ) or (left.getExponent() eq right.getExponent() and positiveCaseSignificandComparison))


    val negativeCase = left.isNegative and right.isNegative and (mkBvSignedLessExpr(
        right.getExponent(), left.getExponent()
    ) or (left.getExponent() eq right.getExponent() and negativeCaseSignificandComparison))


    return neitherNan and mkIte(
        eitherInf, infCase, mkIte(
        eitherZero, zeroCase, negativeLessThanPositive or positiveCase or negativeCase
    )
    )
}

internal fun <Fp : KFpSort> KContext.greater(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
): KExpr<KBoolSort> = less(right, left)


internal fun <Fp : KFpSort> KContext.greaterOrEqual(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
) = lessOrEqual(right, left)

internal fun <Fp : KFpSort> KContext.equal(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
): KExpr<KBoolSort> {
    // All comparison with NaN are false
    val neitherNan = !left.isNaN and !right.isNaN

    val bothZero = left.isZero and right.isZero
    val neitherZero = !left.isZero and !right.isZero

    val flagsAndExponent = neitherNan and (bothZero or
        (neitherZero and (left.isInf eq right.isInf and (left.sign eq right.sign)
            and (left.unbiasedExponent eq right.unbiasedExponent))))


    return flagsAndExponent and (left.normalizedSignificand eq right.normalizedSignificand)
}

internal fun <Fp : KFpSort> KContext.min(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
): KExpr<Fp> {
    return iteOp(right.isNaN or less(left, right), left, right)
}


internal fun <Fp : KFpSort> KContext.max(
    left: UnpackedFp<Fp>, right: UnpackedFp<Fp>,
): KExpr<Fp> = iteOp(right.isNaN or greater(left, right), left, right)
