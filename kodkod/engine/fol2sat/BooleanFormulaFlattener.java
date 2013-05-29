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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import kodkod.engine.bool.BooleanAccumulator;
import kodkod.engine.bool.BooleanFactory;
import kodkod.engine.bool.BooleanFormula;
import kodkod.engine.bool.BooleanValue;
import kodkod.engine.bool.BooleanVariable;
import kodkod.engine.bool.BooleanVisitor;
import kodkod.engine.bool.ITEGate;
import kodkod.engine.bool.MultiGate;
import kodkod.engine.bool.NotGate;
import kodkod.engine.bool.Operator;
import kodkod.util.ints.IntSet;
import kodkod.util.ints.Ints;

/**
 * <p>Given a {@link kodkod.engine.bool.BooleanValue boolean value}, v, and a 
 * {@link kodkod.engine.bool.BooleanFactory factory}, F, 
 * a BooleanFormulaFlattener eliminates as many 
 * intermediate gates as possible from v, and stores the flattened tree of v in F.  
 * An intermediate gate's inputs are absorbed into the parent's iff the 
 * the gate's fanout is 1 and the gate and its parent are MultiGates with the same operator.  
 * For example, suppose that the root corresponds to the formula
 * ((1 || 2) || 3 || !(4 || 5) || (6 & (7 & 8))), and that the components of this formula are
 * assigned the following labels:  (1 || 2) ---> 9, (4 || 5) ---> 10, !(4 || 5) ---> -10, (7 & 8) ---> 11, 
 * (6 & (7 & 8)) ---> 12, and ((1 || 2) || 3 || !(4 || 5) || (6 & 7 & 8)) ---> 13.  
 * Calling this.flatten(root) will flatten the root to (1 || 2 || 3 || !(4 || 5) || (6 & 7 & 8)), 
 * re-assigning the labels as follows: (4 || 5) ---> 9, !(4 || 5) ---> -9, (6 & 7 & 8) ---> 10, and 
 * (1 || 2 || 3 || !(4 || 5) || (6 & 7 & 8)) ---> 11.  
 * 
 * @author Emina Torlak
 */
final class BooleanFormulaFlattener implements BooleanVisitor<BooleanValue, BooleanAccumulator> {



	/**
	 * Flattens the given value using f and returns it.
	 * The method assumes that all variables at the leaves of
	 * the root are components of f.
	 * @requires root.*inputs in f.components
	 * @ensures f.components' = f.components & Variable + flatRoot.*inputs
	 * @return {flatRoot : BooleanValue | [[flatRoot]] = [[root]] && 
	 *           no d, p: flatRoot.*inputs & MultiGate | d in p.inputs && d.op = p.op && inputs.d != p }  
	 */
	static final BooleanValue flatten(BooleanFormula root, BooleanFactory f) {
		final int oldCompDepth = f.comparisonDepth();
		f.setComparisonDepth(1);
		f.clear(); // remove everything but the variables from the factory
		final BooleanFormulaFlattener flattener = new BooleanFormulaFlattener(root, f);
		final BooleanValue flatRoot = root.accept(flattener, null);
		f.setComparisonDepth(oldCompDepth);
		return flatRoot;
	}

	private final BooleanFactory factory;
	private final IntSet flattenable;
	private final Map<MultiGate,BooleanValue> cache;

	/**
	 * Constructs a new FlatteningVisitor.  The returned visitor can only be applied to the specified 
	 * root value.  All the variables at the leaves of the given root must have been created by the 
	 * given factory.
	 * @requires (root.*inputs & Variable) in factory.components
	 */
	private BooleanFormulaFlattener(BooleanFormula root, BooleanFactory factory) {
		this.factory = factory;
		final FlatteningDataGatherer dataGatherer = new FlatteningDataGatherer(root);
		root.accept(dataGatherer, null);
		this.flattenable = dataGatherer.flattenable;
		dataGatherer.visited.removeAll(flattenable); 
		this.cache = new IdentityHashMap<MultiGate,BooleanValue>(dataGatherer.visited.size());
	}

	/**
	 * If p is null, returns v.  Otherwise, adds v to p and
	 * returns the result.
	 */
	private final BooleanValue addToParent(BooleanValue v, BooleanAccumulator parent) {
		return parent==null ? v : parent.add(v);
	}

	/**
	 * Flattens the given multigate into its parent, if they have the multigate is not shared
	 * and it has the same operator as the parent.
	 * @return flattened gate
	 * @see kodkod.engine.bool.BooleanVisitor#visit(kodkod.engine.bool.MultiGate, java.lang.Object)
	 */
	public BooleanValue visit(MultiGate multigate, BooleanAccumulator parent) {
		final Operator.Nary op = multigate.op();
		if (flattenable.contains(multigate.label())) { // multigate's inputs are absorbed into its parent's inputs
//			System.out.println("Flattenable: " + multigate);
			for(Iterator<BooleanFormula> inputs = multigate.iterator(); inputs.hasNext();) {
				if (inputs.next().accept(this, parent)==op.shortCircuit())
					return op.shortCircuit();
			}
			return parent;
		} else { // construct a gate that corresponds to the multigate
//			System.out.println("Unflattenable: " + multigate);
			BooleanValue replacement = cache.get(multigate);

			if (replacement == null) {
				final BooleanAccumulator newGate = BooleanAccumulator.treeGate(op);
				for(Iterator<BooleanFormula> inputs = multigate.iterator(); inputs.hasNext();) {
					if (inputs.next().accept(this,newGate)==op.shortCircuit()) {
						return op.shortCircuit();
					}
				}
				replacement = factory.accumulate(newGate);
				cache.put(multigate, replacement);
			}

			return addToParent(replacement, parent);
		}
	}

	/**
	 * Returns the ite gate.
	 * @return itegate
	 * @see kodkod.engine.bool.BooleanVisitor#visit(kodkod.engine.bool.ITEGate, java.lang.Object)
	 */
	public BooleanValue visit(ITEGate itegate, BooleanAccumulator parent) {
		return addToParent(factory.ite(itegate.input(0).accept(this,null), itegate.input(1).accept(this,null),
				itegate.input(2).accept(this,null)), parent);
	}

	/**
	 * Returns the not gate.
	 * @return negation
	 * @see kodkod.engine.bool.BooleanVisitor#visit(kodkod.engine.bool.NotGate, java.lang.Object)
	 */
	public BooleanValue visit(NotGate negation, BooleanAccumulator parent) {
		return addToParent(factory.not(negation.input(0).accept(this,null)), parent);
	}

	/**
	 * Returns the variable.
	 * @see kodkod.engine.bool.BooleanVisitor#visit(kodkod.engine.bool.BooleanVariable, java.lang.Object)
	 */
	public BooleanValue visit(BooleanVariable variable, BooleanAccumulator parent) {
		return addToParent(variable, parent);
	}
	

	
	/**
	 * A visitor that determins which gates can be flattened.  Specifically, when 
	 * applied to a given root, the flattenable field of the visitor contains the
	 * labels of all m such that m is a MultiGate descendent of the root and
	 * #inputs.m = 1 && (inputs.m).op = m.op => s.contains(m.label).  That is,
	 * flattenable = {i: int | some m: root.^inputs & MultiGate | #inputs.m = 1 && (inputs.m).op = m.op && m.label = i}
	 */
	private static final class FlatteningDataGatherer implements BooleanVisitor<Object, Operator> {
		/* contains the labels of all the flattenable multi gates */
		final IntSet flattenable;
		/* contains the labels of all the visited multi gates */
		final IntSet visited;

		/**
		 * Constructs a new flattenning data gatherer.   The returned visitor can only be
		 * applied to the specified root value.
		 */
		private FlatteningDataGatherer(BooleanFormula root) {
			final int maxLit = StrictMath.abs(root.label());
			this.flattenable = Ints.bestSet(maxLit+1);
			this.visited = Ints.bestSet(maxLit+1);
		}

		public Object visit(MultiGate multigate, Operator parentOp) {
			final int label = multigate.label();
			if (visited.contains(label)) { // we've seen this node already
				flattenable.remove(label);
			} else { // haven't seen it yet
				visited.add(label);
				if (parentOp == multigate.op()) flattenable.add(label);
				// visit children
				for(Iterator<BooleanFormula> inputs = multigate.iterator(); inputs.hasNext();) {
					inputs.next().accept(this, multigate.op());
				}
			}

			return null;
		}

		public Object visit(ITEGate itegate, Operator parentOp) {
			if (visited.add(itegate.label())) { // not visited
				itegate.input(0).accept(this,null);
				itegate.input(1).accept(this,null);
				itegate.input(2).accept(this,null);
			}
			return null;
		}

		public Object visit(NotGate negation, Operator parentOp) {
			negation.input(0).accept(this,null);
			return null;
		}

		public Object visit(BooleanVariable variable, Operator arg) {
			return null;
		}
		

	}
}
