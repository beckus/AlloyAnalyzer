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

import org.sat4j.minisat.SolverFactory;

/**
 * A factory for generating SATSolver instances of a given type.
 * Built-in support is provided for <a href="http://www.sat4j.org/">SAT4J solvers</a>, 
 * the <a href="http://www.princeton.edu/~chaff/zchaff.html">zchaff</a> solver from Princeton, 
 * and the <a href="http://www.cs.chalmers.se/Cs/Research/FormalMethods/MiniSat/">MiniSat</a> solver by 
 * Niklas E&eacute;n and Niklas S&ouml;rensson.
 * @author Emina Torlak
 */
public abstract class SATFactory {
	
	/**
	 * Constructs a new instance of SATFactory.
	 */
	protected SATFactory() {}
	
	/**
	 * The factory that produces instances of the default sat4j solver.
	 * @see org.sat4j.core.ASolverFactory#defaultSolver()
	 */
	public static final SATFactory DefaultSAT4J = new SATFactory() { 
		public SATSolver instance() { 
			return new SAT4J(SolverFactory.instance().defaultSolver()); 
		}
		public String toString() { return "DefaultSAT4J"; }
	};
	
	/**
	 * The factory that produces instances of the "light" sat4j solver.  The
	 * light solver is suitable for solving many small instances of SAT problems.
	 * @see org.sat4j.core.ASolverFactory#lightSolver()
	 */
	public static final SATFactory LightSAT4J = new SATFactory() {
		public SATSolver instance() { 
			return new SAT4J(SolverFactory.instance().lightSolver()); 
		}
		public String toString() { return "LightSAT4J"; }
	};
	
	/**
	 * The factory that produces instances of the zchaff solver from Princeton; 
	 * the returned instances 
	 * support only basic sat solver operations (adding variables/clauses,
	 * solving, and obtaining a satisfying solution, if any).  ZChaff is not incremental.
	 */
	public static final SATFactory ZChaff = new SATFactory() {
		public SATSolver instance() { 
			return new ZChaff(); 
		}
		public boolean incremental() { return false; }
		public String toString() { return "ZChaff"; }
	};
	
	
	
	/**
	 * The factory the produces {@link SATMinSolver cost-minimizing} 
	 * instances of the zchaff solver from Princeton.  Note that cost minimization
	 * can incur a time and/or memory overhead during solving,
	 * so if you do not need this functionality, use the {@link #ZChaff} factory
	 * instead.  ZChaffMincost is not incremental.
	 */
	public static final SATFactory ZChaffMincost = new SATFactory() {
		public SATSolver instance() {
			return new ZChaffMincost();
		}
		@Override
		public boolean minimizer() { return true; }
		public boolean incremental() { return false; }
		public String toString() { return "ZChaffMincost"; }
	};
	
	/**
	 * The factory the produces {@link SATProver proof logging} 
	 * instances of the MiniSat solver.  Note that core
	 * extraction can incur a significant time overhead during solving,
	 * so if you do not need this functionality, use the {@link #MiniSat} factory
	 * instead.
	 */
	public static final SATFactory MiniSatProver = new SATFactory() {
		public SATSolver instance() { 
			return new MiniSatProver(); 
		}
		@Override
		public boolean prover() { return true; }
		public String toString() { return "MiniSatProver"; }
	};
	
	/**
	 * The factory that produces instances of Niklas E&eacute;n and Niklas S&ouml;rensson's
	 * MiniSat solver.
	 */
	public static final SATFactory MiniSat = new SATFactory() {
		public SATSolver instance() {
			return new MiniSat();
		}
		public String toString() { return "MiniSat"; }
	};
	
	public static final SATFactory MiniSatExternal = new SATFactory() {
        public SATSolver instance() {
            return new MiniSatExternal();
        }
        public String toString() { return "MiniSatExternal"; }
    };
    
	
	/**
	 * Returns a SATFactory that produces instances of the specified
	 * SAT4J solver.  For the list of available SAT4J solvers see
	 * {@link org.sat4j.core.ASolverFactory#solverNames() org.sat4j.core.ASolverFactory#solverNames()}.
	 * @requires solverName is a valid solver name
	 * @return a SATFactory that returns the instances of the specified
	 * SAT4J solver
	 * @see org.sat4j.core.ASolverFactory#solverNames()
	 */
	public static final SATFactory sat4jFactory(final String solverName) {
		return new SATFactory() {
			@Override
			public SATSolver instance() {
				return new SAT4J(SolverFactory.instance().createSolverByName(solverName));
			}
			public String toString() { return solverName; }
		};
	}
	
	/**
	 * Returns a SATFactory that produces SATSolver wrappers for the external
	 * SAT solver specified by the executable parameter.  The solver's input
	 * and output formats must conform to the SAT competition standards
	 * (http://www.satcompetition.org/2004/format-solvers2004.html).  The solver
	 * will be called with the specified options, and the given tempInput file name will
	 * be used to store the generated CNF files.  If the tempOutput string is empty,
	 * the solver specified by the executable string is assumed to write its output 
	 * to standard out; otherwise, the
	 * solver is assumed to write its output to the tempOutput file.  It is the caller's responsibility to 
	 * delete the temporary file(s) when no longer needed.  External solvers are never incremental.
	 * @return  SATFactory that produces interruptible SATSolver wrappers for the specified external
	 * SAT solver
	 */
	public static final SATFactory externalFactory(final String executable, final String tempInput, final String tempOutput, final String... options) {
		return new SATFactory() {

			@Override
			public SATSolver instance() {
				return new ExternalSolver(executable, tempInput, tempOutput, options);
			}
			
			@Override
			public boolean incremental() {
				return false;
			}
		};
	}
	
	
	/**
	 * Returns an instance of a SATSolver produced by this factory.
	 * @return a SATSolver instance
	 */
	public abstract SATSolver instance();
	
	/**
	 * Returns true if the solvers returned by this.instance() are
	 * {@link SATProver SATProvers}.  Otherwise returns false.
	 * @return true if the solvers returned by this.instance() are
	 * {@link SATProver SATProvers}.  Otherwise returns false.
	 */
	public boolean prover() {
		return false;
	}
	
	/**
	 * Returns true if the solvers returned by this.instance() are 
	 * {@link SATMinSolver SATMinSolvers}.  Otherwise returns false.
	 * @return true if the solvers returned by this.instance() are
	 * {@link SATMinSolver SATMinSolvers}.  Otherwise returns false.
	 */
	public boolean minimizer() { 
		return false;
	}
	
	/**
	 * Returns true if the solvers returned by this.instance() are incremental;
	 * i.e. if clauses/variables can be added to the solver between multiple
	 * calls to solve().
	 * @return true if the solvers returned by this.instance() are incremental
	 */
	public boolean incremental() {
		return true;
	}

}
