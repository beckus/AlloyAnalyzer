package kodkod.ast.operator;

/**
 * Enumerates binary comparison operators:  =, < , >, <=, >=.
 */
public enum IntCompOperator {
	/** `=' operator */
	EQ 	{ public String toString() { return "="; } },
	/** `!=' operator */
	NEQ { public String toString() { return "!="; } },
	/** `<' operator */
	LT 	{ public String toString() { return "<"; } },
	/** `<=' operator */
	LTE	{ public String toString() { return "<="; } },
	/** `>' operator */
	GT 	{ public String toString() { return ">"; } },
	/** `>=' operator */
	GTE { public String toString() { return ">="; } };
}