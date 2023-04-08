package org.ksmt.symfpu

import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.utils.getValue

class FpRemTest {
    @Test
    fun testFpToBvRem16Expr() = with(KContext()) {
        val a by mkFp16Sort()
        val b by mkFp16Sort()
        testFpExpr(
            mkFpRemExpr(a, b),
            mapOf("a" to a, "b" to b),
        ) { t, e ->
            !mkFpIsNaNExpr(t) and !mkFpIsNaNExpr(e)
        }
    }
}