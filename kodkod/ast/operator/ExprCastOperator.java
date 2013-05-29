package kodkod.ast.operator;

/**
 * Enumerates expression 'cast' operators.
 */
public enum ExprCastOperator {
    /** The cardinality operator (#). */
	CARDINALITY 		{ public String toString() { return "#"; } }, 
	/** The sum operator. */
	SUM 				{ public String toString() { return "sum"; } };

}