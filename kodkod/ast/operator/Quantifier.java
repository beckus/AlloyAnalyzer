package kodkod.ast.operator;

/**
 * Enumerates logical quantifiers.
 */
public enum Quantifier {
	/** Universal quantifier. */
    ALL  { public String toString() { return "all"; }},
    /** Existential quantifier. */
    SOME { public String toString() { return "some"; }}
}