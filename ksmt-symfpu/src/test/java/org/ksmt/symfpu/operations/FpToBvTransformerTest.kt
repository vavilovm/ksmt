package org.ksmt.symfpu.operations

import org.ksmt.KContext
import org.ksmt.expr.KFpRoundingMode
import org.ksmt.expr.printer.BvValuePrintMode
import org.ksmt.expr.printer.PrinterParams


internal fun createContext() = KContext(printerParams = PrinterParams(BvValuePrintMode.BINARY))
