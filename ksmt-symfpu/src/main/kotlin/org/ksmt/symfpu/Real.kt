package org.ksmt.symfpu

import org.ksmt.expr.KExpr
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KIntSort
import org.ksmt.sort.KRealSort


fun <T : KFpSort> fpToReal(unpackedFp: UnpackedFp<T>): KExpr<KRealSort> = with(unpackedFp.ctx) {
    // Real value is unspecified for NaN and Inf

    val numeratorSig = conditionalNegate(unpackedFp.sign, unpackedFp.getSignificand()
        .extendUnsigned(1)).toIntExpr(true)
    val denominatorSig = mkIntNum(1 shl (unpackedFp.significandWidth().toInt() - 1))
    val exp = unpackedFp.getExponent().toIntExpr(true)

    val exponent = unpackedFp.getExponent()
    val exponentNegative = mkBvSignedLessExpr(exponent, mkBv(0, exponent.sort.sizeBits))


    val numerator = mkIte(exponentNegative,
        numeratorSig,
        numeratorSig * 2.expr.power(exp)
    )
    val denominator = mkIte(exponentNegative,
        denominatorSig * 2.expr.power(-exp),
        denominatorSig
    )
    val res = numerator.toRealExpr() / denominator.toRealExpr()

    val isUnspecified = unpackedFp.isNaN or unpackedFp.isInf
    val isZero = unpackedFp.isZero

    val unspecifiedReal = mkRealNum(0, 0)
    mkIte(isUnspecified, unspecifiedReal, mkIte(isZero, mkRealNum(0), res))
}


fun KExpr<KBvSort>.toIntExpr(signed: Boolean): KExpr<KIntSort> {
    return ctx.mkBv2IntExpr(this, signed)
}


