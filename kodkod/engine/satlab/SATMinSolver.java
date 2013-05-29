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

/**
 * Provides an interface to a SAT solver that produces
 * minimal cost solutions.  That is, given a CNF formula
 * and a function f from the variables to non-negative integer,
 * the solver produces the solution that minimizes the expression
 * sum(f(v)*valueOf(v)) for all variables v, where valueOf(v) is 
 * 1 if the variable is set to TRUE and 0 otherwise. 
 * 
 * @specfield variables: set [1..)
 * @specfield cost: variables -> one [0..)
 * @specfield clauses: set Clause
 * @invariant all i: [2..) | i in variables => i-1 in variables
 * @invariant all c: clauses | all lit: c.literals | lit in variables || -lit in variables
 * @invariant all c: clauses | all disj i,j: c.literals | abs(i) != abs(j)
 * @author Emina Torlak
 */
public interface SATMinSolver extends SATSolver {

	/**
	 * Sets the cost of the given variable to the specified value.
	 * @requires variable in this.variables && cost >= 0
	 * @ensures this.cost' = this.cost ++ variable -> cost
	 * @throws IllegalArgumentException - variable !in this.variables || cost < 0
	 */
	public abstract void setCost(int variable, int cost);
	
	/**
	 * Returns the cost of setting the given variable to TRUE.
	 * @requires variable in this.variables
	 * @return this.cost[variable]
	 * @throws IllegalArgumentException - variable !in this.variables
	 */
	public abstract int costOf(int variable);
	
}
