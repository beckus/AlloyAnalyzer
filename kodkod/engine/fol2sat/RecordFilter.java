package kodkod.engine.fol2sat;

import java.util.Map;

import kodkod.ast.Formula;
import kodkod.ast.Node;
import kodkod.ast.Variable;
import kodkod.instance.TupleSet;

/**
 * A filter for TranslationRecords, based on the value of a record's node and literal fields.
 **/
public interface RecordFilter {
	/**
	 * Returns true if the records with the given node,  formula derived from the node, literal, and environment
	 * should be returned by iterators produced by the {@linkplain TranslationLog#replay()} method.
	 * @return true if the records with the given node,  formula derived from the node, literal, and environment
	 * should be returned by iterators produced by {@linkplain TranslationLog#replay()}.
	 */
	public abstract boolean accept(Node node, Formula translated, int literal, Map<Variable,TupleSet> env);
	
	/**
	 * A record filter that accepts all records.
	 */
	public static RecordFilter ALL = new RecordFilter() {
		/**
		 * Returns true.
		 * @return true
		 */
		public boolean accept(Node node, Formula translated, int literal, Map<Variable,TupleSet> env) {
			return true;
		}
	};
	
}