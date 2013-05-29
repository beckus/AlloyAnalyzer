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
package kodkod.engine;

import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;

/**
 * A cost function to be minimized during {@link kodkod.engine.Solver#solve(Formula, Bounds, Cost) solving}.
 * In particular, each Cost is a total function from a set of {@link kodkod.ast.Relation relations}  
 * to a non-negative integer which represents the weight of one edge in the relation's value.  (Hence, the 
 * total cost of a relation <i>r</i> in a given instance is <i>s</i> is #s.tuples(r) * edgeCost(r).)
 * @specfield relations: set Relation // the domain of this cost function
 * @specfield cost: relations -> one [0..)
 * @author Emina Torlak
 */
public interface Cost {

	/**
	 * Returns the cost of one edge in the relational value of the given
	 * {@link Relation Relation instance}.
	 * @return this.cost[relation]
	 * @throws IllegalArgumentException - relation !in this.relations
	 */
	public abstract int edgeCost(Relation relation);
}
