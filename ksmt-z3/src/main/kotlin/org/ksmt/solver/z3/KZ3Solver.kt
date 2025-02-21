package org.ksmt.solver.z3

import com.microsoft.z3.Solver
import com.microsoft.z3.Status
import com.microsoft.z3.Z3Exception
import com.microsoft.z3.solverAssert
import com.microsoft.z3.solverAssertAndTrack
import com.microsoft.z3.solverCheckAssumptions
import com.microsoft.z3.solverGetUnsatCore
import org.ksmt.KContext
import org.ksmt.decl.KConstDecl
import org.ksmt.expr.KExpr
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverException
import org.ksmt.solver.KSolverStatus
import org.ksmt.sort.KBoolSort
import org.ksmt.utils.NativeLibraryLoader
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.IdentityHashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit

open class KZ3Solver(private val ctx: KContext) : KSolver<KZ3SolverConfiguration> {
    private val z3Ctx = KZ3Context(ctx)
    private val solver = createSolver()
    private var lastCheckStatus = KSolverStatus.UNKNOWN
    private var currentScope: UInt = 0u

    @Suppress("LeakingThis")
    private val contextCleanupActionHandler = registerContextForCleanup(this, z3Ctx)

    private val exprInternalizer by lazy {
        createExprInternalizer(z3Ctx)
    }
    private val exprConverter by lazy {
        createExprConverter(z3Ctx)
    }

    open fun createExprInternalizer(z3Ctx: KZ3Context): KZ3ExprInternalizer = KZ3ExprInternalizer(ctx, z3Ctx)

    open fun createExprConverter(z3Ctx: KZ3Context) = KZ3ExprConverter(ctx, z3Ctx)

    private fun createSolver(): Solver = z3Ctx.nativeContext.mkSolver()

    override fun configure(configurator: KZ3SolverConfiguration.() -> Unit) {
        val params = z3Ctx.nativeContext.mkParams()
        KZ3SolverConfigurationImpl(params).configurator()
        solver.setParameters(params)
    }

    override fun push() {
        solver.push()
        z3Ctx.pushAssertionLevel()
        currentScope++
    }

    override fun pop(n: UInt) {
        require(n <= currentScope) {
            "Can not pop $n scope levels because current scope level is $currentScope"
        }
        if (n == 0u) return

        solver.pop(n.toInt())
        repeat(n.toInt()) { z3Ctx.popAssertionLevel() }

        currentScope -= n
    }

    override fun assert(expr: KExpr<KBoolSort>) = z3Try {
        ctx.ensureContextMatch(expr)

        val z3Expr = with(exprInternalizer) { expr.internalizeExpr() }
        solver.solverAssert(z3Expr)

        z3Ctx.assertPendingAxioms(solver)
    }

    override fun assertAndTrack(expr: KExpr<KBoolSort>, trackVar: KConstDecl<KBoolSort>) = z3Try {
        ctx.ensureContextMatch(expr, trackVar)

        val z3Expr = with(exprInternalizer) { expr.internalizeExpr() }
        val z3TrackVar = with(exprInternalizer) { trackVar.apply().internalizeExpr() }

        solver.solverAssertAndTrack(z3Expr, z3TrackVar)
    }

    override fun check(timeout: Duration): KSolverStatus = z3Try {
        solver.updateTimeout(timeout)
        solver.check().processCheckResult()
    }

    override fun checkWithAssumptions(
        assumptions: List<KExpr<KBoolSort>>,
        timeout: Duration
    ): KSolverStatus = z3Try {
        ctx.ensureContextMatch(assumptions)

        val z3Assumptions = with(exprInternalizer) {
            LongArray(assumptions.size) { assumptions[it].internalizeExpr() }
        }

        solver.updateTimeout(timeout)

        solver.solverCheckAssumptions(z3Assumptions).processCheckResult()
    }

    override fun model(): KModel = z3Try {
        require(lastCheckStatus == KSolverStatus.SAT) {
            "Model are only available after SAT checks, current solver status: $lastCheckStatus"
        }
        val model = solver.model

        KZ3Model(model, ctx, z3Ctx, exprInternalizer)
    }

    // TODO add mapping back from tracked variable into initial value
    override fun unsatCore(): List<KExpr<KBoolSort>> = z3Try {
        require(lastCheckStatus == KSolverStatus.UNSAT) { "Unsat cores are only available after UNSAT checks" }

        val unsatCore = solver.solverGetUnsatCore()

        with(exprConverter) {
            unsatCore.map { it.convertExpr() }
        }
    }

    override fun reasonOfUnknown(): String = z3Try {
        require(lastCheckStatus == KSolverStatus.UNKNOWN) { "Unknown reason is only available after UNKNOWN checks" }
        solver.reasonUnknown
    }

    override fun interrupt() = z3Try {
        solver.interrupt()
    }

    override fun close() {
        unregisterContextCleanup(contextCleanupActionHandler)
        z3Ctx.close()
    }

    private fun Status?.processCheckResult() = when (this) {
        Status.SATISFIABLE -> KSolverStatus.SAT
        Status.UNSATISFIABLE -> KSolverStatus.UNSAT
        Status.UNKNOWN -> KSolverStatus.UNKNOWN
        null -> KSolverStatus.UNKNOWN
    }.also { lastCheckStatus = it }

    private fun Solver.updateTimeout(timeout: Duration) {
        val z3Timeout = if (timeout == Duration.INFINITE) {
            UInt.MAX_VALUE.toInt()
        } else {
            timeout.toInt(DurationUnit.MILLISECONDS)
        }
        val params = z3Ctx.nativeContext.mkParams().apply {
            add("timeout", z3Timeout)
        }
        setParameters(params)
    }

    private inline fun <reified T> z3Try(body: () -> T): T = try {
        body()
    } catch (ex: Z3Exception) {
        throw KSolverException(ex)
    }

    companion object {
        init {
            System.setProperty("z3.skipLibraryLoad", "true")
            NativeLibraryLoader.load { os ->
                when (os) {
                    NativeLibraryLoader.OS.LINUX -> listOf("libz3", "libz3java")
                    NativeLibraryLoader.OS.MACOS -> listOf("libz3", "libz3java")
                    NativeLibraryLoader.OS.WINDOWS -> listOf("vcruntime140", "vcruntime140_1", "libz3", "libz3java")
                }
            }
        }

        private val cleanupHandlers = ReferenceQueue<KZ3Solver>()
        private val contextForCleanup = IdentityHashMap<PhantomReference<KZ3Solver>, KZ3Context>()

        /** Ensure Z3 native context is closed and all native memory is released.
         * */
        private fun registerContextForCleanup(solver: KZ3Solver, context: KZ3Context): PhantomReference<KZ3Solver> {
            cleanupStaleContexts()
            val cleanupHandler = PhantomReference(solver, cleanupHandlers)
            contextForCleanup[cleanupHandler] = context

            return cleanupHandler
        }

        private fun unregisterContextCleanup(handler: PhantomReference<KZ3Solver>) {
            contextForCleanup.remove(handler)
            handler.clear()
            cleanupStaleContexts()
        }

        private fun cleanupStaleContexts() {
            while (true) {
                val handler = cleanupHandlers.poll() ?: break
                contextForCleanup.remove(handler)?.close()
            }
        }
    }
}
