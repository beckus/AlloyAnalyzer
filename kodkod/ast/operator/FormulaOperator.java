package kodkod.ast.operator;



/**
 * Enumerates binary (&&, ||, =>, <=>) and nary (&&, ||) logical operators. 
 * @specfield op: (int->lone Formula) -> Formula
 * @invariant all args: seq Formula, out: Formula | args->out in op => (out.children = args && out.op = this)
 */
public enum FormulaOperator {
    /** Logical AND operator. */      
    AND  		{ public String toString() { return "&&"; } },
    /** Logical OR operator. */      
    OR 		{ public String toString() { return "||"; } },
    /** Logical bi-implication operator. */
    IFF 		{ public String toString() { return "<=>"; } },
    /** Logical implication operator. */      
    IMPLIES 	{ public String toString() { return "=>"; } };
  
    static final int nary = (1<<AND.ordinal()) | (1<<OR.ordinal());
    
    /**
     * Returns true if this is an nary operator.
     * @return true if this is an nary operator
     */
    public final boolean nary() { return (nary & (1<<ordinal()))!=0; }
    

}