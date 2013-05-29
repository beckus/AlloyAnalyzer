package kodkod.ast.operator;

/**
 * Enumerates relational comparison operators.
 */
public enum ExprCompOperator {
	/** Subset operator (in). */
    SUBSET { public String toString() { return "in"; } },
    /** Equality operator (=). */
    EQUALS { public String toString() { return "="; } };
}