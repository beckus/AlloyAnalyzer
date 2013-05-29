/* 
 * Kodkod -- Copyright (c) 2005-2011, Emina Torlak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package kodkod.engine.fol2sat;

import static kodkod.engine.bool.Operator.AND;
import kodkod.engine.bool.BooleanFormula;
import kodkod.engine.bool.BooleanVariable;
import kodkod.engine.bool.BooleanVisitor;
import kodkod.engine.bool.ITEGate;
import kodkod.engine.bool.MultiGate;
import kodkod.engine.bool.NotGate;
import kodkod.engine.bool.Operator;
import kodkod.engine.satlab.SATFactory;
import kodkod.engine.satlab.SATSolver;
import kodkod.util.ints.IntSet;
import kodkod.util.ints.Ints;

/**
 * Transforms a boolean circuit into a formula in conjunctive
 * normal form.
 * 
 * @author Emina Torlak
 */
final class Bool2CNFTranslator implements BooleanVisitor<int[], Object> {

	/**
	 * Creates a new instance of SATSolver using the provided factory
	 * and uses it to translate the given circuit into conjunctive normal form
	 * using the <i>definitional translation algorithm</i>.
	 * The third parameter is required to contain the number of primary variables
	 * allocated during translation from FOL to boolean.
	 * @return a SATSolver instance returned by the given factory and initialized
	 * to contain the CNF translation of the given circuit.
	 */
	static SATSolver translate(BooleanFormula circuit, SATFactory factory, int numPrimaryVariables) {
		final SATSolver solver = factory.instance();
		final Bool2CNFTranslator translator = new Bool2CNFTranslator(solver, numPrimaryVariables, circuit);
//		System.out.println("--------------transls2-------------");
		if (circuit.op()==Operator.AND) { 
			for(BooleanFormula input : circuit) { 
//				System.out.println(input);
//				solver.addClause(input.accept(translator,null));
				input.accept(translator, null);
			}
			for(BooleanFormula input : circuit) { 
				translator.unaryClause[0] = input.label();
				solver.addClause(translator.unaryClause);
			}
		} else {
			solver.addClause(circuit.accept(translator,null));
		}
		return solver;
	}

	/**
	 * Helper visitor that performs <i> definitional translation to cnf </i>.
	 * @specfield root: BooleanFormula // the translated circuit
	 */

	private final SATSolver solver;
	private final IntSet visited;
	private final PolarityDetector pdetector;
	private final int[] unaryClause = new int[1];
	private final int[] binaryClause = new int[2];
	private final int[] ternaryClause = new int[3];
	
	/**
	 * Constructs a translator for the given circuit.
	 * @ensures this.root' = circuit
	 */
	private Bool2CNFTranslator(SATSolver solver, int numPrimaryVars, BooleanFormula circuit) {
		final int maxLiteral = StrictMath.abs(circuit.label());
		this.solver = solver;
		this.solver.addVariables(StrictMath.max(numPrimaryVars, maxLiteral));
		this.pdetector = (new PolarityDetector(numPrimaryVars, maxLiteral)).apply(circuit);
		this.visited = Ints.bestSet(pdetector.offset, StrictMath.max(pdetector.offset, maxLiteral));
	}

	/** @return 0->lit */
	final int[] clause(int lit) { 
		unaryClause[0] = lit;
		return unaryClause;
	}
	/** @return 0->lit0 + 1->lit1 */
	final int[] clause(int lit0, int lit1) { 
		binaryClause[0] = lit0; binaryClause[1] = lit1;
		return binaryClause;
	}
	/** @return 0->lit0 + 1->lit1 + 2->lit2 */
	final int[] clause(int lit0, int lit1, int lit2) { 
		ternaryClause[0] = lit0; ternaryClause[1] = lit1; ternaryClause[2] = lit2;
		return ternaryClause;
	}
	
	/**
	 * Adds translation clauses to the solver and returns an array containing the
	 * gate's literal. The CNF clauses are generated according to the standard SAT to CNF translation:
	 * o = AND(i1, i2, ... ik) ---> (i1 | !o) & (i2 | !o) & ... & (ik | !o) & (!i1 | !i2 | ... | !ik | o),
	 * o = OR(i1, i2, ... ik)  ---> (!i1 | o) & (!i2 | o) & ... & (!ik | o) & (i1 | i2 | ... | ik | !o).
	 * @return o: int[] | o.length = 1 && o.[0] = multigate.literal
	 * @ensures if the multigate has not yet been visited, its children are visited
	 * and the clauses are added to the solver connecting the multigate's literal to
	 * its input literal, as described above.
	 */
	public int[] visit(MultiGate multigate, Object arg) {  
		final int oLit = multigate.label();
		if (visited.add(oLit)) { 
			final int sgn; final boolean p, n;
			if (multigate.op()==AND) {
				sgn = 1; p = pdetector.positive(oLit); n = pdetector.negative(oLit);
			} else { // multigate.op()==OR
				sgn = -1; n = pdetector.positive(oLit); p = pdetector.negative(oLit);
			}
			final int[] lastClause = n ? new int[multigate.size()+1] : null;
			final int output = oLit * -sgn;
			int i = 0;
			for(BooleanFormula input : multigate) {
				int iLit = input.accept(this, arg)[0];
				if (p) {
					solver.addClause(clause(iLit * sgn, output));
				}
				if (n) { 
					lastClause[i++] = iLit * -sgn;
				}
			}
			if (n) {
				lastClause[i] = oLit * sgn;
				solver.addClause(lastClause);
			}
		}
		return clause(oLit);        
	}

	/**
	 * Adds translation clauses to the solver and returns an array containing the
	 * gate's literal. The CNF clauses are generated according to the standard SAT to CNF translation:
	 * o = ITE(i, t, e) ---> (!i | !t | o) & (!i | t | !o) & (i | !e | o) & (i | e | !o)
	 * @return o: int[] | o.length = 1 && o.[0] = itegate.literal
	 * @ensures if the itegate has not yet been visited, its children are visited
	 * and the clauses are added to the solver connecting the multigate's literal to
	 * its input literal, as described above.
	 */
	public int[] visit(ITEGate itegate, Object arg) {
		final int oLit = itegate.label();
		if (visited.add(oLit)) {
			final int i = itegate.input(0).accept(this, arg)[0];
			final int t = itegate.input(1).accept(this, arg)[0];
			final int e = itegate.input(2).accept(this, arg)[0];
			final boolean p = pdetector.positive(oLit), n = pdetector.negative(oLit);
			if (p) {
				solver.addClause(clause(-i, t, -oLit));
				solver.addClause(clause(i, e, -oLit));
				// redundant clause that strengthens unit propagation
				solver.addClause(clause(t, e, -oLit));
			}
			if (n) {
				solver.addClause(clause(-i, -t, oLit));	
				solver.addClause(clause(i, -e, oLit));
				// redundant clause that strengthens unit propagation
				solver.addClause(clause(-t, -e, oLit));
			}	
		}
		return clause(oLit);
	}

	/** 
	 * Returns the negation of the result of visiting negation.input, wrapped in
	 * an array.
	 * @return o: int[] | o.length = 1 && o[0] = - translate(negation.inputs)[0]
	 *  */
	public int[] visit(NotGate negation, Object arg) {
		return clause(-negation.input(0).accept(this, arg)[0]);
	}

	/**
	 * Returns the variable's literal wrapped in a an array.
	 * @return o: int[] | o.length = 1 && o[0] = variable.literal
	 */
	public int[] visit(BooleanVariable variable, Object arg) {
		return clause(variable.label());
	}


	/**
	 * Helper visitor that detects pdetector of subformulas.
	 * @specfield root: BooleanFormula // the root of the DAG for whose components we are storing pdetector information
	 */
	private static final class PolarityDetector implements BooleanVisitor<Object, Integer> {
		final int offset;
		/**
		 * @invariant all i : [0..polarity.length) | 
		 *   pdetector[i] = 0 <=> formula with label offset + i has not been visited,
		 *   pdetector[i] = 1 <=> formula with label offset + i has been visited with positive pdetector only,
		 *   pdetector[i] = 2 <=> formula with label offset + i has been visited with negative pdetector only,
		 *   pdetector[i] = 3 <=> formula with label offset + i has been visited with both polarities
		 */
		private final int[] polarity;
		private final Integer[] ints = { Integer.valueOf(3), Integer.valueOf(1), Integer.valueOf(2) };

		/**
		 * Creates a new pdetector detector and applies it to the given circuit.
		 * @requires maxLiteral = |root.label()|
		 */
		PolarityDetector(int numPrimaryVars, int maxLiteral) {
			this.offset = numPrimaryVars+1;
			this.polarity = new int[StrictMath.max(0, maxLiteral-numPrimaryVars)];
		}

		/**
		 * Applies this detector to the given formula, and returns this.
		 * @requires this.root = root
		 * @ensures this.visit(root)
		 * @return this
		 */
		PolarityDetector apply(BooleanFormula root) {
			root.accept(this, ints[1]);
			return this;
		}

		/**
		 * Returns true if the formula with the given label occurs positively in this.root.  
		 * @requires this visitor has been applied to this.root
		 * @requires label in (MultiGate + ITEGate).label
		 * @return true if the formula with the given label occurs positively in this.root.  
		 */
		boolean positive(int label) {
			return (polarity[label-offset] & 1) > 0;
		}

		/**
		 * Returns true if the formula with the given label occurs negatively in this.root.  
		 * @requires this visitor has been applied to this.root
		 * @requires label in (MultiGate + ITEGate).label
		 * @return true if the formula with the given label occurs negatively in this.root.  
		 */
		boolean negative(int label) {
			return (polarity[label-offset] & 2) > 0;
		}

		/**
		 * Returns true if the given formula has been visited with the specified
		 * pdetector (1 = positive, 2 = negative, 3 = both).  Otherwise records the visit and returns false.
		 * @requires formula in (MultiGate + ITEGate)
		 * @requires pdetector in this.ints
		 * @return true if the given formula has been visited with the specified
		 * pdetector.  Otherwise records the visit and returns false.
		 */
		private boolean visited(BooleanFormula formula, Integer polarity) {
			final int index = formula.label() - offset;
			final int value = this.polarity[index];
			return (this.polarity[index] = value | polarity) == value;
		}

		public Object visit(MultiGate multigate, Integer arg) {
			if (!visited(multigate, arg)) {
				for(BooleanFormula input : multigate) {
					input.accept(this, arg);
				}
			}
			return null;
		}

		public Object visit(ITEGate ite, Integer arg) {
			if (!visited(ite, arg)) {
				// the condition occurs both positively and negative in an ITE gate
				ite.input(0).accept(this, ints[0]);
				ite.input(1).accept(this, arg);
				ite.input(2).accept(this, arg);
			}
			return null;
		}

		public Object visit(NotGate negation, Integer arg) {
			return negation.input(0).accept(this, ints[3-arg]);
		}

		public Object visit(BooleanVariable variable, Integer arg) {
			return null; // nothing to do
		}
		
	}

}
