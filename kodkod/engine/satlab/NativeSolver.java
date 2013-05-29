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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import kodkod.engine.config.Options;

/**
 * A skeleton implementation of a wrapper for a sat solver
 * accessed through JNI.
 * 
 * @author Emina Torlak
 */
abstract class NativeSolver implements SATSolver {
	/**
	 * The memory address of the native instance wrapped by this wrapper.
	 */
	private long peer;
	private Boolean sat;
	private int clauses, vars;
	
	private OutputStream cnfFile;  
	
	/**
	 * Constructs a new wrapper for the given 
	 * instance of the native solver.
	 */
	NativeSolver(long peer) {
		this.peer = peer;
		this.clauses = this.vars = 0;
		this.sat = null;
		try {
		    if (Options.isDebug())
		        cnfFile = new BufferedOutputStream(new FileOutputStream(new File(System.getProperty("java.io.tmpdir"), "cnf_kk.cnf")));
        } catch (IOException e) {
        }
	}
	
	/**
	 * Loads the JNI library with the given name.
	 */
	static void loadLibrary(String library) {
	    try { System.loadLibrary(library);      return; } catch(UnsatisfiedLinkError ex) { }
        try { System.loadLibrary(library+"x1"); return; } catch(UnsatisfiedLinkError ex) { }
        try { System.loadLibrary(library+"x2"); return; } catch(UnsatisfiedLinkError ex) { }
        try { System.loadLibrary(library+"x3"); return; } catch(UnsatisfiedLinkError ex) { }
        try { System.loadLibrary(library+"x4"); return; } catch(UnsatisfiedLinkError ex) { }
        System.loadLibrary(library+"x5");        
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#numberOfVariables()
	 */
	public final int numberOfVariables() {
		return vars;
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#numberOfClauses()
	 */
	public final int numberOfClauses() {
		return clauses;
	}
	
	/**
	 * Adjusts the internal clause count so that the next call to {@linkplain #numberOfClauses()}
	 * will return the given value.      
	 * @requires clauseCount >= 0 
	 * @ensures adjusts the internal clause so that the next call to {@linkplain #numberOfClauses()}
	 * will return the given value.
	 */
	void adjustClauseCount(int clauseCount) {
		assert clauseCount >= 0;
		clauses = clauseCount;
	}
		
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#addVariables(int)
	 * @see #addVariables(long, int)
	 */
	public final void addVariables(int numVars) {
		if (numVars < 0)
			throw new IllegalArgumentException("vars < 0: " + numVars);
		else if (numVars > 0) {
			vars += numVars;
			addVariables(peer, numVars);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#addClause(int[])
	 * @see #addClause(long, int[])
	 */
	public final boolean addClause(int[] lits) {
		if (lits.length > 0) {
			if (addClause(peer, lits)) {
				clauses++;
				try {
                    if (Options.isDebug()) cnfFile.write((Arrays.toString(lits) + " 0\n").getBytes());
                } catch (IOException e) {}
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Returns a pointer to the C++ peer class (the native instance wrapped by this object).
	 * @return a pointer to the C++ peer class (the native instance wrapped by this object).
	 */
	final long peer() { return peer; }
	
	/**
	 * Returns the current sat of the solver.
	 * @return null if the sat is unknown, TRUE if the last
	 * call to solve() yielded SAT, and FALSE if the last call to
	 * solve() yielded UNSAT.
	 */
	final Boolean status() { return sat; }
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#solve()
	 * @see #solve(long)
	 */
	public final boolean solve() {
	    try {
            if (Options.isDebug()) {
                cnfFile.write(String.format("p cnf %s %s\n", vars, clauses).getBytes());
                cnfFile.flush();
                cnfFile.close();
            }
        } catch (IOException e) {
        }
		return (sat = solve(peer));
	}
	

	/**
	 * Throws an IllegalArgumentException if variable !in this.variables.
	 * Otherwise does nothing.
	 * @throws IllegalArgumentException - variable !in this.variables
	 */
	final void validateVariable(int variable) {
		if (variable < 1 || variable > vars)
			throw new IllegalArgumentException(variable + " !in [1.." + vars+"]");
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#valueOf(int)
	 */
	public final boolean valueOf(int variable) {
		if (!Boolean.TRUE.equals(sat))
			throw new IllegalStateException();
		validateVariable(variable);
		return valueOf(peer, variable);
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.SATSolver#free()
	 */
	public synchronized final void free() {
		if (peer!=0) {
//			System.out.println("freeing " + peer + " " + getClass());
			free(peer);
			peer = 0;
		} // already freed
	}
	
	
	/**
	 * Releases the resources used by this native solver.
	 */
	protected final void finalize() throws Throwable {
		super.finalize();
		free();
	}
	
	/**
	 * Releases the resources associated with
	 * the native solver at the given memory address.  
	 * This method must be called when the object holding the
	 * given reference goes out of scope to avoid
	 * memory leaks.
	 * @ensures releases the resources associated
	 * with the given native peer
	 */
	abstract void free(long peer);
	
	/**
	 * Adds the specified number of variables to the given native peer.
	 * @ensures increases the vocabulary of the given native peer by 
	 * the specified number of variables
	 */
	abstract void addVariables(long peer, int numVariables);

	/**
	 * Ensures that the given native peer logically contains the
	 * specified clause and returns true if the solver's clause database 
	 * changed as a result of the call.
	 * @requires all i: [0..lits.length) | abs(lits[i]) in this.variables 
	 * @requires all disj i,j: [0..lits.length) | abs(lits[i]) != abs(lits[j])
	 * @ensures ensures that the given native peer logically contains the specified clause
	 * @return true if the peer's clause database changed as a result of the call; a negative integer if not.
	 */
	abstract boolean addClause(long peer, int[] lits);
	
	/**
	 * Calls the solve method on the given native peer.
	 * @return true if the clauses in the solver are SAT;
	 * otherwise returns false.
	 */
	abstract boolean solve(long peer);
	
	/**
	 * Returns the assignment for the given literal
	 * by the specified native peer
	 * @requires the last call to {@link #solve(long) solve(peer)} returned SATISFIABLE
	 * @return the assignment for the given literal
	 */
	abstract boolean valueOf(long peer, int literal);

}
