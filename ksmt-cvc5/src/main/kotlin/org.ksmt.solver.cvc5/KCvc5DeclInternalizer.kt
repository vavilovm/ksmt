package org.ksmt.solver.cvc5

import io.github.cvc5.Term
import org.ksmt.decl.KConstDecl
import org.ksmt.decl.KDeclVisitor
import org.ksmt.decl.KFuncDecl
import org.ksmt.sort.KSort

open class KCvc5DeclInternalizer(
    private val cvc5Ctx: KCvc5Context,
    private val sortInternalizer: KCvc5SortInternalizer
) : KDeclVisitor<Term> {

    override fun <S : KSort> visit(decl: KFuncDecl<S>): Term = cvc5Ctx.internalizeDecl(decl) {
        // declarations incremental collection optimization
        cvc5Ctx.addDeclaration(decl)

        val domainSorts = decl.argSorts.map { it.accept(sortInternalizer) }
        val rangeSort = decl.sort.accept(sortInternalizer)

        cvc5Ctx.nativeSolver.declareFun(
            decl.name,
            domainSorts.toTypedArray(),
            rangeSort
        )
    }

    override fun <S : KSort> visit(decl: KConstDecl<S>): Term = cvc5Ctx.internalizeDecl(decl) {
        cvc5Ctx.addDeclaration(decl)

        val sort = decl.sort.accept(sortInternalizer)
        cvc5Ctx.nativeSolver.mkConst(sort, decl.name)
    }
}
