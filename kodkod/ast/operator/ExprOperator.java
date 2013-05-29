package kodkod.ast.operator;




/**
 * Enumerates unary (~, ^, *), binary (+, &, ++, ->, -, .) and nary (+, &, ++, ->) expression operators.
 * @specfield op: (int->lone Expression) -> Expression
 * @invariant all args: seq Expression, out: Expression | args->out in op => (out.children = args && out.op = this)
 */
public enum ExprOperator {
	/** Relational union (+) operator. */
	UNION 				{ public String toString() { return "+"; } },
	/** Relational intersection (&) operator. */
	INTERSECTION		{ public String toString() { return "&"; } },
	/** Relational override (++) operator. */
	OVERRIDE			{ public String toString() { return "++"; } },
	/** Relational product (->) operator. */
	PRODUCT				{ public String toString() { return "->"; } },
	/** Relational difference (-) operator. */
	DIFFERENCE			{ public String toString() { return "-"; } },
	/** Relational join (.) operator. */
	JOIN 				{ public String toString() { return "."; } },
	/** Transpose (~) operator. */
    TRANSPOSE 			{ public String toString() { return "~";} },
    /** Transitive closure (^) operator. */
    CLOSURE 			{ public String toString() { return "^";} },
    /** Reflexive transitive closure (*) operator. */
    REFLEXIVE_CLOSURE 	{ public String toString() { return "*";} };
  	
 
    static final int unary = TRANSPOSE.index() | CLOSURE.index() | REFLEXIVE_CLOSURE.index();
    
    static final int binary = ~unary;
    
    static final int nary = UNION.index() | INTERSECTION.index() | OVERRIDE.index() | PRODUCT.index();
    
    private final int index() { return 1<<ordinal(); }
    
    /**
     * Returns true if this is a unary operator.
     * @return true if this is a unary operator.
     */
    public final boolean unary() { return (unary & index())!=0; }
    
    /**
     * Returns true if this is a binary operator.
     * @return true if this is a binary operator.
     */
    public final boolean binary() { return (binary & index())!=0; }
    
    /**
     * Returns true if this is an nary operator.
     * @return true if this is an nary operator.
     */
    public final boolean nary() { return (nary & index())!=0; }
    
}