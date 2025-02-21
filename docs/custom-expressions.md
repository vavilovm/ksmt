# Custom expressions

In some cases, it is convenient to extend the KSMT expression 
system with user defined (custom) expressions.
Here we will describe the key points of creating custom expressions.

See [CustomExpressions.kt](/examples/src/main/kotlin/CustomExpressions.kt) for a complete example. 

### Expressions definition

As an example, we will consider the following custom expressions structure: 
a base `CustomExpr` expression and two custom expressions. 
The  `CustomAndExpr` expression  that acts like `n-ary logical and` 
and `CustomBvAddExpr` that acts like `n-ary bvadd`.

#### Base expression and transformer

The important thing is that we must not only define our new custom expression, 
but also define an extended `KTransformer`, because KSMT transformers are 
unaware of our custom expressions.

```kotlin
interface CustomTransformer : KTransformer {
    fun transform(expr: CustomAndExpr): KExpr<KBoolSort>
    fun <T : KBvSort> transform(expr: CustomBvAddExpr<T>): KExpr<T>
}
```

For the base expression, we can override accept with our `CustomTransformer`, 
since all of our custom expressions can only be correctly transformed  with this transformer.

```kotlin
abstract class CustomExpr<T : KSort>(ctx: KContext) : KExpr<T>(ctx) {
    override fun accept(transformer: KTransformerBase): KExpr<T> {
        /**
         * Since KSMT transformers are unaware of our custom expressions
         * we must use our own extended transformer.
         *
         * Note: it's a good idea to throw an exception when our
         * custom expression is visited by non our transformer,
         * because this usually means that our custom expression
         * has leaked into KSMT core and will be processed incorrectly.
         * */
        transformer as? CustomTransformer ?: error("Leaked custom expression")
        return accept(transformer)
    }

    abstract fun accept(transformer: CustomTransformer): KExpr<T>
}
```

#### Custom expressions

Important notes on defining custom expressions are provided as comments in the examples.
Also, see [expression management](#expressions-management) for the details about `equals`, `hashCode`, 
`internEquals` and `internHashCode`.

```kotlin
class CustomAndExpr(
    val args: List<KExpr<KBoolSort>>,
    ctx: KContext
) : CustomExpr<KBoolSort>(ctx) {
    override val sort: KBoolSort
        get() = ctx.boolSort

    /**
     * Expression printer, suitable for deeply nested expressions.
     * Mainly used in toString.
     * */
    override fun print(printer: ExpressionPrinter) {
        printer.append("(customAnd")
        args.forEach {
            printer.append(" ")

            /**
             * Note the use of append with KExpr argument.
             * */
            printer.append(it)
        }
        printer.append(")")
    }

    /**
     * Analogues of equals and hashCode.
     * */
    override fun internHashCode(): Int = hash(args)

    /**
     * Note the use of [structurallyEqual] utility, which check
     * that types are the same and all fields specified in `{ }` are equal.
     * */
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { args }

    /**
     * The expression is visited by the transformer.
     * Normally, we should invoke the corresponding transform
     * method of the transformer and return the invocation result.
     *
     * It is usually a bad idea to return something without transform
     * invocation, or to invoke transform on some other expression.
     * It is better to perform any expression transformation with the
     * corresponding transformer.
     * */
    override fun accept(transformer: CustomTransformer): KExpr<KBoolSort> =
        transformer.transform(this)
}
```

The definition of `CustomBvAddExpr` is actually the same as for `CustomAndExpr`.

```kotlin
class CustomBvAddExpr<T : KBvSort>(
    val args: List<KExpr<T>>,
    ctx: KContext
) : CustomExpr<T>(ctx) {

    /**
     * The sort of this expression is parametric and
     * depends on the sorts of arguments.
     * */
    override val sort: T by lazy { args.first().sort }

    override fun print(printer: ExpressionPrinter) {
        printer.append("(customBvAdd")
        args.forEach {
            printer.append(" ")
            printer.append(it)
        }
        printer.append(")")
    }

    override fun internHashCode(): Int = hash(args)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { args }

    override fun accept(transformer: CustomTransformer): KExpr<T> =
        transformer.transform(this)
}
```

### Expressions rewriting

Since KSMT is unaware of our custom expressions, 
we must ensure that the expressions that will be processed 
by the various KSMT facilities do not contain any custom parts.
For such purposes we can define a transformer for our custom expressions that rewrites 
them with the appropriate KSMT expressions.

```kotlin
class CustomExprRewriter(
    override val ctx: KContext
) : KNonRecursiveTransformer(ctx), CustomTransformer {
    /**
     * Here we use [transformExprAfterTransformed] function of [KNonRecursiveTransformer]
     * that implements bottom-up transformation (transform arguments first).
     * */
    override fun <T : KBvSort> transform(expr: CustomBvAddExpr<T>): KExpr<T> =
        transformExprAfterTransformed(expr, expr.args) { transformedArgs ->
            transformedArgs.reduce { acc, e -> ctx.mkBvAddExpr(acc, e) }
        }

    override fun transform(expr: CustomAndExpr): KExpr<KBoolSort> =
        transformExprAfterTransformed(expr, expr.args) { transformedArgs ->
            ctx.mkAnd(transformedArgs)
        }
}

```

Note the use of `KNonRecursiveTransformer` that allows transformation of deeply nested expressions without recursion.

### Expressions management

In KSMT we use expression interning and guarantee that any 
two equal expressions are equal by reference (same object).
We use `internEquals` and `internHashCode` as a replacement for `equals` and `hashCode`,
and we use reference equality for the `equals` method.
To apply interning for custom expressions you need to create an interner and ensure that
all created expressions are managed by this interner.
For such purposes it is convenient to define an extended context that manages all custom expressions.

```kotlin
class CustomContext : KContext() {
    // Interners for custom expressions.
    private val customAndInterner = mkAstInterner<CustomAndExpr>()
    private val customBvInterner = mkAstInterner<CustomBvAddExpr<*>>()

    // Expression builder, that performs interning of created expression
    fun mkCustomAnd(args: List<KExpr<KBoolSort>>): CustomAndExpr =
        customAndInterner.createIfContextActive {
            CustomAndExpr(args, this)
        }

    fun <T : KBvSort> mkCustomBvAdd(args: List<KExpr<T>>): CustomBvAddExpr<T> =
        customBvInterner.createIfContextActive {
            CustomBvAddExpr(args, this)
        }.uncheckedCast() // Unchecked cast is used here because [customBvInterner] has erased sort.
}
```

### Misc: wrappers

Since it is required to eliminate all our custom expressions 
before interaction with any KSMT feature, 
it is convenient to create wrappers.

For example, we can wrap `KSolver` and eliminate custom expressions on each query.

```kotlin
class CustomSolver<C : KSolverConfiguration>(
    private val solver: KSolver<C>,
    private val transformer: CustomExprRewriter
) : KSolver<C> {

    // expr can contain custom expressions -> rewrite
    override fun assert(expr: KExpr<KBoolSort>) =
        solver.assert(transformer.apply(expr))

    // expr can contain custom expressions -> rewrite
    override fun assertAndTrack(expr: KExpr<KBoolSort>, trackVar: KConstDecl<KBoolSort>) =
        solver.assertAndTrack(transformer.apply(expr), trackVar)

    // assumptions can contain custom expressions -> rewrite
    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus =
        solver.checkWithAssumptions(assumptions.map { transformer.apply(it) }, timeout)

    // wrap model for correct handling of eval method
    override fun model(): KModel = CustomModel(solver.model(), transformer)

    // Other methods don't suffer from custom expressions

    override fun check(timeout: Duration): KSolverStatus =
        solver.check(timeout)

    override fun pop(n: UInt) = solver.pop(n)
    override fun push() = solver.push()

    override fun reasonOfUnknown(): String = solver.reasonOfUnknown()
    override fun unsatCore(): List<KExpr<KBoolSort>> = solver.unsatCore()

    override fun close() = solver.close()
    override fun configure(configurator: C.() -> Unit) = solver.configure(configurator)
    override fun interrupt() = solver.interrupt()
}
```

Also, we can wrap `KModel` and eliminate custom expressions before eval.

```kotlin
class CustomModel(
    private val model: KModel,
    private val transformer: CustomExprRewriter
) : KModel {
    // expr can contain custom expressions -> rewrite
    override fun <T : KSort> eval(expr: KExpr<T>, isComplete: Boolean): KExpr<T> =
        model.eval(transformer.apply(expr), isComplete)

    // Other methods don't suffer from custom expressions

    override val declarations: Set<KDecl<*>>
        get() = model.declarations

    override val uninterpretedSorts: Set<KUninterpretedSort>
        get() = model.uninterpretedSorts

    override fun <T : KSort> interpretation(decl: KDecl<T>): KModel.KFuncInterp<T>? =
        model.interpretation(decl)

    override fun uninterpretedSortUniverse(sort: KUninterpretedSort): Set<KExpr<KUninterpretedSort>>? =
        model.uninterpretedSortUniverse(sort)

    override fun detach(): KModel = CustomModel(model.detach(), transformer)
}
```
