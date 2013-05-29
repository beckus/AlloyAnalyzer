package kodkod.ast.operator;

/**
 * Represents an intexpression 'cast' operator.
 */
public enum IntCastOperator {
	/** The Int cast operator Int[intExpr]. */
	INTCAST {
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		public String toString() { 
			return "Int";
		}
	}, 
	/** The Bitset cast operator Bits[intExpr]. */
	BITSETCAST {
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		public String toString() { 
			return "Bits";
		}
	};
}