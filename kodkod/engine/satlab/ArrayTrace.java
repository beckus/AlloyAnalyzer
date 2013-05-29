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
package kodkod.engine.satlab;

import java.util.Iterator;
import java.util.NoSuchElementException;

import kodkod.util.collections.Containers;
import kodkod.util.ints.IntBitSet;
import kodkod.util.ints.IntIterator;
import kodkod.util.ints.IntSet;
import kodkod.util.ints.Ints;

/**
 * An array-backed implementation of the {@linkplain ResolutionTrace} interface.
 * @author Emina Torlak
 */
final class ArrayTrace implements ResolutionTrace {
	/* The trace array encodes the resolution trace as follows.
	 * The first <tt>axioms</tt> entries in the trace array contain
	 * the literals of the clauses added to the prover, in the order
	 * in which they were added.  The remaining entries encode the
	 * resolvents as follows.   Let i >= <tt>axioms</tt>
	 * represent the ith resolvent.  Then trace[i][0] contains the number of the resolvent's
	 * antecedents; trace[i][1..trace[i][0]] contains the indices of 
	 * the resolvent's antecedents in the trace; and trace[i][trace[i][0]+1..trace[i].length-1]
	 * contains the literals of the ith resolvent.  All literals are sorted in the 
	 * increasing order of absolute values. All antecedents of a given resolvent  precede it in the trace,
	 * and the conflict clause should be the last trace element.
	 */
	private final int[][] trace;
	private final int axioms;
	private final IntSet core;
	
	/**
	 * Constructs a resolution trace view for the given raw trace.
	 * The first <tt>axioms</tt> entries in the trace array should
	 * contain the literals of the clauses added to the prover, in
	 * the order in which they were added.  The literals should be 
	 * sorted in the increasing order of absolute values.  The remaining entries
	 * should encode the resolvents as follows.  Let i be the index
	 * of a resolvent in the raw trace.  Then, for all 0 <= j < trace[i].length, 
	 * trace[i][j] is the index of the resolvent's jth antecedents in the trace array.
	 * All antecedents of a given resolvent should precede it in the trace,
	 * and the conflict clause should be the last trace element.
	 * 
     * <p><b>Note: </b> the given array's contents must not be modified while
     * in use by this resolution trace object.</p>
	 */
	ArrayTrace(int[][] trace, int axioms) {
		this.axioms = axioms;
		this.trace = computeResolventLiterals(trace, axioms, axioms);
		this.core = Ints.unmodifiableIntSet(core(trace,axioms));
	}
	
	/**
	 * Constructs a resolution trace from the given subtrace and partial
	 * trace. This constructor assumes that <tt>partial</tt> is the result 
	 * of solving the subtrace of the <tt>original</tt> trace that is given by the
	 * specified set of indices.  The first indices.size() of the partial
	 * trace are assumed to represent the clauses given by original.trace[indices],
	 * in the increasing order of indices; the remaining entries should encode
	 * the resolvents computed from original.trace[indices], as specified by 
	 * {@linkplain #ArrayTrace(int[][], int)}. The given subtrace of the original 
	 * trace must be self-contained, i.e. original.reachable(indices).equals(indices). 
	 * 
	 * <p><b>Note: </b> the given array's contents must not be modified while
     * in use by this resolution trace object.</p>
	 */
	ArrayTrace(ArrayTrace original, IntSet indices, int[][] partial) {
		
		int axiomCount = indices.size();
		{ // fill the partial[0..indices.size()-1] with the corresponding clauses from original.trace[indices]
			final int[][] originalTrace = original.trace;	
			final int[] position = new int[originalTrace.length];
			
			IntIterator itr = indices.iterator();
			for(int i = 0, length = indices.size(); i < length; i++) {
				int index = itr.next();
				position[index] = i;
				if (original.axiom(index)) { // just set the ith pointer to original literals
					partial[i] = originalTrace[index];
				} else { // copy the resolvent and adjust copy's antecedent indices
					int resLength = originalTrace[index].length;
					int[] resolvent = new int[resLength];
					System.arraycopy(originalTrace[index], 0, resolvent, 0, resLength);
					for(int j = 1, lastAnte = resolvent[0]; j <= lastAnte; j++) {
						resolvent[j] = position[resolvent[j]];
					}
					partial[i] = resolvent;
					axiomCount--;
				}
			}
		}
		this.axioms = axiomCount;
		this.trace = computeResolventLiterals(partial, axioms, indices.size());
		this.core = Ints.unmodifiableIntSet(core(trace,axioms));
	}

	
	/**
	 * Computes literals for the resolvents at indices [computeFrom..trace.length) in the given trace.
	 * Arrays at indices [0..computeFrom) are assumed to encode clauses as specified by {@linkplain #trace}.  
	 * The resolvents at indices [computeFrom..trace.length) are assumed to encode the remaining resolvents,
	 * as specified by the {@linkplain #ArrayTrace(int[][], int)} constructor.  
	 * @requires axioms <= computeFrom < trace.length
	 * @ensures modifies trace to conform to the specification of {@linkplain #trace}
	 * @return modified trace
	 */
	private static int[][] computeResolventLiterals(int[][] trace, int axioms, int computeFrom) {
		for(int i = computeFrom, length = trace.length; i < length; i++) {
			int[] ante = trace[i];
			int[] lits = resolve(trace[ante[0]], ante[0]<axioms, trace[ante[1]], ante[1]<axioms);
			for(int j = 2; j < ante.length; j++) {
				lits = resolve(lits, true, trace[ante[j]], ante[j]<axioms);
			}
			int[] resolvent = new int[ante.length + lits.length + 1];
			resolvent[0] = ante.length;
			System.arraycopy(ante, 0, resolvent, 1, ante.length);
			System.arraycopy(lits, 0, resolvent, ante.length+1, lits.length);
			trace[i] = resolvent;
		}
		return trace;
	}
	
	/**
	 * Returns an array of integers representing the result of 
	 * resolving the clauses c1 and c2, sorted in the increasing order
	 * of absolute values.  The parameters axiom1 and axiom2 specify 
	 * whether c1 and c2 encode axioms or resolvents.  In particular, 
	 * if axiom1 (resp. axiom2) is true, then all integers in c1 (resp. c2) 
	 * are assumed to be literals, sorted in the increasing order of absolute 
	 * values.  If axiom1 (resp. axiom2) is false, then the integers starting 
	 * at c1[0]+1 (resp. c2[0]+1) are assumed to be literals, sorted in the increasing
	 * order of absolute values.   
	 * @requires let off1 = axiom1 ? 0 : c1[0] + 1, off2 = axiom2 ? 0 : c2[0]+1 | 
	 *  (all i: [off1..c1.length), j: [off1..c1.length) | i < j => abs(c1[i]) < abs(c1[j])) and
	 *  (all i: [off2..c2.length), j: [off2..c2.length) | i < j => abs(c2[i]) < abs(c2[j])) and
	 *  (one i: [off1..c1.length), j: [off2..c2.length) | c1[i] = -c2[j])
	 * @return an array of integers representing the result of 
	 * resolving the clauses c1 and c2, sorted in the increasing order of absolute values
	 */
	private static int[] resolve(int[] c1, boolean axiom1, int[] c2, boolean axiom2) {
		final int len1 = c1.length, len2 = c2.length;
		int i = axiom1 ? 0 : c1[0] + 1;
		int j = axiom2 ? 0 : c2[0] + 1;
		int k = 0;
		
		final int[] tmp = new int[(len1-i + len2-j) - 2];
		
		while(i < len1 && j < len2) {
			int lit1 = c1[i], lit2 = c2[j];
			int var1 = StrictMath.abs(lit1), var2 = StrictMath.abs(lit2);
			if (var1==var2) {
				if (lit1==lit2) {
					tmp[k++] = lit1;
				}
				i++;
				j++;
			} else if (var1 < var2) {
				tmp[k++] = lit1;
				i++;
			} else { // var1 > var2
				tmp[k++] = lit2;
				j++;
			}
		}
		if (i<len1) {
			final int rem = len1 - i;
			System.arraycopy(c1, i, tmp, k, rem);
			k += rem;
		} 
		if (j<len2) {
			final int rem = len2 - j;
			System.arraycopy(c2, j, tmp, k, rem);
			k += rem;
		}
		if (k==tmp.length) { 
			return tmp;
		} else {
			final int[] ret = new int[k];
			System.arraycopy(tmp, 0, ret, 0, k);
			return ret;
		}
	}
	
	
	/**
	 * Returns the indices of the core clauses in the given trace.
	 * The trace and axioms argument are assumed to have the 
	 * properties specified by the {@linkplain ArrayTrace#ArrayResolution(int[][], int)}
	 * constructor.
	 * @return the indices of the core clauses in the given trace
	 */
	private static IntSet core(int[][] trace, int axioms) {
		final IntSet core = new IntBitSet(axioms);
		final IntSet reachable = new IntBitSet(trace.length);
		reachable.add(trace.length-1);
		for(int i = trace.length-1; i >= axioms; i--) {
			if (reachable.contains(i)) {
				int[] resolvent = trace[i];
				for(int j = 1, antes = resolvent[0]; j <= antes; j++) {
					reachable.add(resolvent[j]);
				}
			}
		}
		
		for(IntIterator itr = reachable.iterator(0, axioms-1); itr.hasNext(); ) {
			core.add(itr.next());
		}
		
		return core;
	}
	
	/**
	 * Returns true if the clause at the given index is an axiom.
	 * @return index < this.axioms
	 */
	private boolean axiom(int index) {
		return index < axioms;
	}
	
	/**
	 * Returns the offset in this.trace[index] array where literal
	 * data is stored. 
	 * @return axiom(index) ? 0 : this.trace[index][0] + 1
	 */
	private int litOffset(int index) {
		return axiom(index) ? 0 : trace[index][0] + 1;
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#size()
	 */
	public int size() {	return trace.length; }
	
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#core()
	 */
	public IntSet core() { return core; }

	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#axioms()
	 */
	public IntSet axioms() { return Ints.rangeSet(Ints.range(0, axioms-1)); }
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#resolvents()
	 */
	public IntSet resolvents() { return Ints.rangeSet(Ints.range(axioms, trace.length-1)); }
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#get(int)
	 */
	public Clause get(final int index) {
		if (index>=0 && index<trace.length) {
			if (axiom(index)) { // return a self-contained clause
				return new Clause() {
					final int[] literals = trace[index];
					final int hashCode = Ints.superFastHash(literals);
					public Iterator<Clause> antecedents() { return Containers.emptyIterator(); }
					public IntIterator literals() { return new IntArrayIterator(literals,0,literals.length); }
					public int maxVariable() { return StrictMath.abs(literals[literals.length-1]); }
					public int numberOfAntecedents() { return 0; }
					public int size() { return literals.length;	}
					public int[] toArray(int[] array) {
						if (array.length<literals.length) { array = new int[literals.length]; }
						System.arraycopy(literals, 0, array, 0, literals.length);
						return array;
					}
					public int hashCode() { return hashCode; }
				};
			} else {
				return new ClauseView(index);
			}
		}
		throw new IndexOutOfBoundsException("invalid index: " + index);
	}
		
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#iterator()
	 */
	public Iterator<Clause> iterator() { 
		return new ClauseIterator(new IntIterator() {
			int index = 0;
			public boolean hasNext() { return index>=0 && index < trace.length; }
			public int next() { 
				if (!hasNext()) throw new NoSuchElementException();
				return index++;
			}
			public void remove() { throw new UnsupportedOperationException(); } 
		}); 
	}

	/**
	 * Returns true if indices.min() >= 0 && indices.max() < this.size()
	 * @requires !indices.isEmpty()
	 * @return indices.min() >= 0 && indices.max() < this.size()
	 */
	private boolean valid(IntSet indices) {
		return indices.min()>=0 && indices.max()<trace.length;
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#iterator(kodkod.util.ints.IntSet)
	 */
	public Iterator<Clause> iterator(IntSet indices) {
		if (indices.isEmpty() || valid(indices)) {
			return new ClauseIterator(indices.iterator());
		}
		throw new IndexOutOfBoundsException("invalid indices: " + indices);
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#reverseIterator(kodkod.util.ints.IntSet)
	 */
	public Iterator<Clause> reverseIterator(IntSet indices) {
		if (indices.isEmpty() || valid(indices)) {
			return new ClauseIterator(indices.iterator(Integer.MAX_VALUE, Integer.MIN_VALUE));
		}
		throw new IndexOutOfBoundsException("invalid indices: " + indices);
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#implicants(kodkod.util.ints.IntSet)
	 */
	public IntSet reachable(IntSet indices) {
		if (indices.isEmpty()) return Ints.EMPTY_SET;
		else if (valid(indices)) {
			final IntSet ret = new IntBitSet(trace.length);
			ret.addAll(indices);
			for(int i = indices.max(); i >= axioms; i--) {
				if (ret.contains(i)) {
					int[] resolvent = trace[i];
					for(int j = 1, antes = resolvent[0]; j <= antes; j++) {
						ret.add(resolvent[j]);
					}
				}
			}
			return ret;
		}
		else throw new IndexOutOfBoundsException("invalid indices: " + indices);
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#backwardReachable(kodkod.util.ints.IntSet)
	 */
	public IntSet backwardReachable(IntSet indices) {
		if (indices.isEmpty()) return Ints.EMPTY_SET;
		else if (valid(indices)) {
			final IntSet ret = new IntBitSet(trace.length);
			ret.addAll(indices);
			for(int i = axioms, length = trace.length; i < length; i++) {
				int[] resolvent = trace[i];
				for(int j = 1, antes = resolvent[0]; j <= antes; j++) {
					if (ret.contains(resolvent[j])) {
						ret.add(i);
						break;
					}
				}
			}
			return ret;
		}
		else throw new IndexOutOfBoundsException("invalid indices: " + indices);
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#learnable(kodkod.util.ints.IntSet)
	 */
	public IntSet learnable(IntSet indices) {
		if (indices.isEmpty()) return Ints.EMPTY_SET;
		else if (valid(indices)) {
			final IntSet ret = new IntBitSet(trace.length);
			ret.addAll(indices);
			TOP: for(int i = axioms, length = trace.length; i < length; i++) {
				int[] resolvent = trace[i];
				for(int j = 1, antes = resolvent[0]; j <= antes; j++) {
					if (!ret.contains(resolvent[j])) {
						continue TOP;
					}
				}
				ret.add(i);
			}
			return ret;
		}
		else throw new IndexOutOfBoundsException("invalid indices: " + indices);
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ResolutionTrace#directlyLearnable(kodkod.util.ints.IntSet)
	 */
	public IntSet directlyLearnable(IntSet indices) { 
		if (indices.isEmpty()) return Ints.EMPTY_SET;
		else if (valid(indices)) {
			final IntSet ret = new IntBitSet(trace.length);
			ret.addAll(indices);
			TOP: for(int i = axioms, length = trace.length; i < length; i++) {
				int[] resolvent = trace[i];
				for(int j = 1, antes = resolvent[0]; j <= antes; j++) {
					if (!indices.contains(resolvent[j])) {
						continue TOP;
					}
				}
				ret.add(i);
			}
			return ret;
		}
		
		else throw new IndexOutOfBoundsException("invalid indices: " + indices);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		for(int i = 0; i < axioms; i++) {
			ret.append("AXIOM.  Literals: ");
			int[] clause = trace[i];
			for(int j = 0, c = clause.length; j < c; j++) {
				ret.append(clause[j]);
				ret.append(" ");
			}
			ret.append("\n");
		}
		for(int i = axioms, max = trace.length; i < max; i++) {
			ret.append("RESOLVENT.  Antecedents:  ");
			int[] clause = trace[i];
			for(int j = 1, c = clause[0]; j <= c; j++) {
				ret.append(clause[j]);
				ret.append(" ");
			}
			ret.append("\n");
		}
		return ret.toString();
	}
	
	/**
	 * A mutable implementation of the Clause interface.
	 * @author Emina Torlak
	 */
	private class ClauseView extends Clause {
		private int[] clause;
		private int litOffset;
		
		/**
		 * Constructs a clause view for the ith clause.
		 * @requires 0 <= index < trace.length
		 */
		ClauseView(int index) {
			this.clause = trace[index];
			this.litOffset = litOffset(index);
		}
		
		/**
		 * Constructs a clause view for the 0th clause.
		 */
		ClauseView() { this(0); }
		
		/**
		 * Sets the state of this clause view to represent
		 * the ith clause in the trace and returns this.
		 * @ensures sets the state of this clause view to represent
		 * the ith clause in the trace
		 * @return this
		 */
		ClauseView set(int index) {
			this.clause = trace[index];
			this.litOffset = litOffset(index);
			return this;
		}
		
		public int maxVariable() { return StrictMath.abs(clause[clause.length-1]); }
		public int numberOfAntecedents() { return StrictMath.max(0, litOffset-1); }		
		public int size() {	return clause.length - litOffset; }
		public Iterator<Clause> antecedents() { return new ClauseIterator(new IntArrayIterator(clause, 1, litOffset)); }
		public IntIterator literals() {	return new IntArrayIterator(clause, litOffset, clause.length); }	
		
		public int[] toArray(int[] array) {
			final int size = size();
			if (array.length < size) {
				array = new int[size];
			}
			System.arraycopy(clause, litOffset, array, 0, size);
			return array;
		}
	}
	
	/**
	 * A clause iterator wrapper for an int iterator.
	 * @author Emina Torlak
	 */
	private final class ClauseIterator extends ClauseView implements Iterator<Clause> {
		private final IntIterator itr;
		/**
		 * Constructs a clause iterator that will iterate over the clauses in this.trace
		 * located at the indices given by itr.  The given iterator must return valid indices.
		 */
		ClauseIterator(IntIterator itr) {
			this.itr = itr;
		}
		public boolean hasNext() { return itr.hasNext(); }
		public Clause next() { return set(itr.next()); }
		public void remove() { throw new UnsupportedOperationException(); }
	}
	
	/**
	 * An int iterator that iterates over the portion of an integer array
	 * in the increasing order of indices.
	 * @author Emina Torlak
	 */
	private static final class IntArrayIterator implements IntIterator {
		private final int[] array;
		private int from;
		private final int to;	
		/**
		 * Constructs an int iterator that iterates over the given array,
		 * returning the elements between from, inclusive, and to, exclusive.
		 * @requires 0 <= from < array.length < Integer.MAX_VALUE
		 */
		IntArrayIterator(int[] array, int from, int to) {
			this.array = array;
			this.from = from;
			this.to = to;
		}
		public boolean hasNext() {	return from >= 0 && from < to; }
		public int next() {
			if (!hasNext()) throw new NoSuchElementException();
			return array[from++];
		}
		public void remove() {	throw new UnsupportedOperationException(); }	
	}	
}
