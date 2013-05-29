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

import static kodkod.util.nodes.AnnotatedNode.annotate;
import static kodkod.util.nodes.AnnotatedNode.annotateRoots;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;
import kodkod.ast.Node;
import kodkod.ast.Relation;
import kodkod.ast.RelationPredicate;
import kodkod.ast.visitor.AbstractReplacer;
import kodkod.engine.bool.BooleanAccumulator;
import kodkod.engine.bool.BooleanConstant;
import kodkod.engine.bool.BooleanFactory;
import kodkod.engine.bool.BooleanFormula;
import kodkod.engine.bool.BooleanMatrix;
import kodkod.engine.bool.BooleanValue;
import kodkod.engine.bool.Int;
import kodkod.engine.bool.Operator;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATSolver;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.util.ints.IntSet;
import kodkod.util.nodes.AnnotatedNode;

/** 
 * Translates, evaluates, and approximates {@link Node nodes} with
 * respect to given {@link Bounds bounds} (or {@link Instance instances}) and {@link Options}.
 * 
 * @author Emina Torlak 
 */
public final class Translator {
	
	/*---------------------- public methods ----------------------*/
	/**
	 * Overapproximates the value of the given expression using the provided bounds and options.
	 * @return a BooleanMatrix whose TRUE entries represent the tuples contained in a sound overapproximation
	 * of the expression.
	 * @throws expression = null || instance = null || options = null
	 * @throws UnboundLeafException - the expression refers to an undeclared variable or a relation not mapped by the instance
	 * @throws HigherOrderDeclException - the expression contains a higher order declaration
	 */
	public static BooleanMatrix approximate(Expression expression, Bounds bounds, Options options) {
		Environment<BooleanMatrix, Expression> emptyEnv = Environment.empty();
        return FOL2BoolTranslator.approximate(annotate(expression), LeafInterpreter.overapproximating(bounds, options), emptyEnv);
	}
	
	/**
	 * Evaluates the given formula to a BooleanConstant using the provided instance and options.  
	 * 
	 * @return a BooleanConstant that represents the value of the formula.
	 * @throws NullPointerException - formula = null || instance = null || options = null
	 * @throws UnboundLeafException - the formula refers to an undeclared variable or a relation not mapped by the instance
	 * @throws HigherOrderDeclException - the formula contains a higher order declaration
	 */
	public static BooleanConstant evaluate(Formula formula, Instance instance, Options options) {
		final LeafInterpreter interpreter = LeafInterpreter.exact(instance, options);
		final BooleanConstant eval = (BooleanConstant) FOL2BoolTranslator.translate(annotate(formula), interpreter);
		//TODO: check OF
//		final BooleanFactory factory = interpreter.factory();
//        BooleanConstant overflow = (BooleanConstant) factory.of();
//        if (options.noOverflow() && overflow.booleanValue()) { //[AM]
//            eval = BooleanConstant.FALSE;
//        }
        return eval;
	}
	
	/**
	 * Evaluates the given expression to a BooleanMatrix using the provided instance and options.
	 * 
	 * @return a BooleanMatrix whose TRUE entries represent the tuples contained by the expression.
	 * @throws NullPointerException - expression = null || instance = null || options = null
	 * @throws UnboundLeafException - the expression refers to an undeclared variable or a relation not mapped by the instance
	 * @throws HigherOrderDeclException - the expression contains a higher order declaration
	 */
	public static BooleanMatrix evaluate(Expression expression,Instance instance, Options options) {
		return (BooleanMatrix) FOL2BoolTranslator.translate(annotate(expression), LeafInterpreter.exact(instance, options));
	}

	/**
	 * Evalutes the given intexpression to an {@link kodkod.engine.bool.Int} using the provided instance and options. 
	 * @return an {@link kodkod.engine.bool.Int} representing the value of the intExpr with respect
	 * to the specified instance and options.
	 * @throws NullPointerException - formula = null || instance = null || options = null
	 * @throws UnboundLeafException - the expression refers to an undeclared variable or a relation not mapped by the instance
	 * @throws HigherOrderDeclException - the expression contains a higher order declaration
	 */
	public static Int evaluate(IntExpression intExpr, Instance instance, Options options) {
		LeafInterpreter interpreter = LeafInterpreter.exact(instance, options);
        Int ret = (Int) FOL2BoolTranslator.translate(annotate(intExpr), interpreter);
        //TODO: check OF
//		BooleanFactory factory = interpreter.factory();
//		BooleanConstant bc = (BooleanConstant) factory.of();
//		boolean overflow = false;
//		if (options.noOverflow() && bc.booleanValue()) //[AM]
//		    overflow = true;
//		ret.setOverflowFlag(overflow);
		return ret;
	}
	
	/**
	 * Translates the given formula using the specified bounds and options.
	 * @return a Translation whose solver is a SATSolver instance initialized with the 
	 * CNF representation of the given formula, with respect to the given bounds.  The CNF
	 * is generated in such a way that the magnitude of the literal representing the truth
	 * value of a given formula is strictly larger than the magnitudes of the literals representing
	 * the truth values of the formula's descendants.  
	 * @throws TrivialFormulaException - the given formula is reduced to a constant during translation
	 * (i.e. the formula is trivially (un)satisfiable).
	 * @throws NullPointerException - any of the arguments are null
	 * @throws UnboundLeafException - the formula refers to an undeclared variable or a relation not mapped by the given bounds.
	 * @throws HigherOrderDeclException - the formula contains a higher order declaration that cannot
	 * be skolemized, or it can be skolemized but options.skolemize is false.
	 */
	public static Translation translate(Formula formula, Bounds bounds, Options options) throws TrivialFormulaException {
		return (new Translator(formula,bounds,options)).translate();
	}
	
	/*---------------------- private translation state and methods ----------------------*/
	/**
	 * @specfield formula: Formula
	 * @specfield bounds: Bounds
	 * @specfield options: Options
	 * @specfield log: TranslationLog
	 */
	private final Formula formula;
	private final Bounds bounds;
	private final Options options;
	
	private TranslationLog log;
	
	/**
	 * Constructs a Translator for the given formula, bounds and options.
	 * @ensures this.formula' = formula and 
	 * 	this.options' = options and 
	 * 	this.bounds' = bounds.clone() and
	 *  no this.log'
	 */
	private Translator(Formula formula, Bounds bounds, Options options) {
		this.formula = formula;
		this.bounds = bounds.clone();
		this.options = options;
		this.log = null;
	}
	
	/**
	 * Translates this.formula with respect to this.bounds and this.options.
	 * @return a Translation whose solver is a SATSolver instance initialized with the 
	 * CNF representation of the given formula, with respect to the given bounds.  The CNF
	 * is generated in such a way that the magnitude of a literal representing the truth
	 * value of a given formula is strictly larger than the magnitudes of the literals representing
	 * the truth values of the formula's descendants.  
	 * @throws TrivialFormulaException - this.formula is reduced to a constant during translation
	 * (i.e. the formula is trivially (un)satisfiable).
	 * @throws UnboundLeafException - this.formula refers to an undeclared variable or a relation not mapped by this.bounds.
	 * @throws HigherOrderDeclException - this.formula contains a higher order declaration that cannot
	 * be skolemized, or it can be skolemized but this.options.skolemDepth < 0
	 */
	private Translation translate() throws TrivialFormulaException  {
		final AnnotatedNode<Formula> annotated = options.logTranslation()>0 ? annotateRoots(formula) : annotate(formula);
		final SymmetryBreaker breaker = optimizeBounds(annotated);
		return toBoolean(optimizeFormula(annotated, breaker), breaker);
	}
	
	/**
	 * Removes bindings for unused relations/ints from this.bounds and
	 * returns a SymmetryBreaker for the reduced bounds.
	 * @requires annotated.node = this.formula
	 * @ensures this.bounds'.relations = this.formula.*children & Relations
	 * @ensures !annotated.usesInts() => no this.bounds'.int
	 * @return { b: SymmetryBreaker | b.bounds = this.bounds' }
	 */
	private SymmetryBreaker optimizeBounds(AnnotatedNode<Formula> annotated) {	
		// remove bindings for unused relations/ints
		bounds.relations().retainAll(annotated.relations());
		if (!annotated.usesInts()) bounds.ints().clear();
		
		// detect symmetries
		return new SymmetryBreaker(bounds, options.reporter());
	}
	
	/**
	 * Optimizes annotated.node by first breaking symmetries on its top-level predicates,
	 * replacing them with the simpler formulas generated by {@linkplain SymmetryBreaker#breakMatrixSymmetries(Map, boolean) breaker.breakMatrixSymmetries(...)}, 
	 * and skolemizing the result.
	 * @requires annotated.node = this.formula
	 * @requires breaker.bounds = this.bounds
	 * @return the skolemization, up to depth this.options.skolemDepth, of annotated.node with
	 * the broken predicates replaced with simpler constraints and the remaining predicates inlined. 
	 */
	private AnnotatedNode<Formula> optimizeFormula(AnnotatedNode<Formula> annotated, SymmetryBreaker breaker) {	
		options.reporter().optimizingBoundsAndFormula();
		if (options.logTranslation()==0) { // no logging
			annotated = inlinePredicates(annotated, breaker.breakMatrixSymmetries(annotated.predicates(), true).keySet());
			annotated = options.skolemDepth()>=0 ? Skolemizer.skolemize(annotated, bounds, options) : annotated;
//			if (options.noOverflow()) {
//	            annotated = NNFConverter.flatten(annotated); // FullNegationPropagator.flatten(annotated);
//	        } 
			return annotated;
		} else { 
		    // logging; inlining of predicates *must* happen last when logging is enabled
			if (options.coreGranularity()==1) { 
				annotated = FormulaFlattener.flatten(annotated, false);
			}
			if (options.skolemDepth()>=0) {
				annotated = Skolemizer.skolemize(annotated, bounds, options);
			}
			if (options.coreGranularity()>1) { 
				annotated = FormulaFlattener.flatten(annotated, options.coreGranularity()==3);
			}
//			if (options.noOverflow()) {
//                annotated = NNFConverter.flatten(annotated); //FullNegationPropagator.flatten(annotated);          
//            } 
			return inlinePredicates(annotated, breaker.breakMatrixSymmetries(annotated.predicates(), false));
		}
	}
	
	/**
	 * Returns an annotated formula f such that f.node is equivalent to annotated.node
	 * with its <tt>truePreds</tt> replaced with the constant formula TRUE and the remaining
	 * predicates replaced with equivalent constraints.
	 * @requires this.options.logTranslation = false
	 * @requires truePreds in annotated.predicates()[RelationnPredicate.NAME]
	 * @requires truePreds are trivially true with respect to this.bounds
	 * @return an annotated formula f such that f.node is equivalent to annotated.node
	 * with its <tt>truePreds</tt> replaced with the constant formula TRUE and the remaining
	 * predicates replaced with equivalent constraints.
	 */
	private AnnotatedNode<Formula> inlinePredicates(final AnnotatedNode<Formula> annotated, final Set<RelationPredicate> truePreds) {
		final AbstractReplacer inliner = new AbstractReplacer(annotated.sharedNodes()) {
			public Formula visit(RelationPredicate pred) {
				Formula ret = lookup(pred);
				if (ret!=null) return ret;
				return truePreds.contains(pred) ? cache(pred, Formula.TRUE) : cache(pred, pred.toConstraints());
			}
		};
		return annotate(annotated.node().accept(inliner));	
	}
	
	/**
	 * Returns an annotated formula f such that f.node is equivalent to annotated.node
	 * with its <tt>simplified</tt> predicates replaced with their corresponding Formulas and the remaining
	 * predicates replaced with equivalent constraints.  The annotated formula f will contain transitive source 
	 * information for each of the subformulas of f.node.  Specifically, let t be a subformula of f.node, and
	 * s be a descdendent of annotated.node from which t was derived.  Then, f.source[t] = annotated.source[s]. </p>
	 * @requires this.options.logTranslation = true
	 * @requires simplified.keySet() in annotated.predicates()[RelationPredicate.NAME]
	 * @requires no disj p, p': simplified.keySet() | simplified.get(p) = simplifed.get(p') // this must hold in order
	 * to maintain the invariant that each subformula of the returned formula has exactly one source
	 * @requires for each p in simplified.keySet(), the formulas "p and [[this.bounds]]" and
	 * "simplified.get(p) and [[this.bounds]]" are equisatisfiable
	 * @return an annotated formula f such that f.node is equivalent to annotated.node
	 * with its <tt>simplified</tt> predicates replaced with their corresponding Formulas and the remaining
	 * predicates replaced with equivalent constraints.
	 */
	private AnnotatedNode<Formula> inlinePredicates(final AnnotatedNode<Formula> annotated, final Map<RelationPredicate,Formula> simplified) {
		final Map<Node,Node> sources = new IdentityHashMap<Node,Node>();
		final AbstractReplacer inliner = new AbstractReplacer(annotated.sharedNodes()) {
			private RelationPredicate source =  null;			
			protected <N extends Node> N cache(N node, N replacement) {
				if (replacement instanceof Formula) {
					if (source==null) {
						final Node nsource = annotated.sourceOf(node);
						if (replacement!=nsource) 
							sources.put(replacement, nsource);
					} else {
						sources.put(replacement, source);
					}
				}
				return super.cache(node, replacement);
			}
			public Formula visit(RelationPredicate pred) {
				Formula ret = lookup(pred);
				if (ret!=null) return ret;
				source = pred;
				if (simplified.containsKey(pred)) {
					ret = simplified.get(pred).accept(this);
				} else {
					ret = pred.toConstraints().accept(this);
				}
				source = null;
				return cache(pred, ret);
			}
		};

		return annotate(annotated.node().accept(inliner), sources);
	}
	
	/**
	 * Translates the given annotated formula to a circuit, conjoins the circuit with an 
	 * SBP generated by the given symmetry breaker, flattens the result if so specified by this.options, 
	 * and returns its Translation to CNF.
	 * @requires [[annotated.node]] <=> ([[this.formula]] and [[breaker.broken]])
	 * @ensures this.options.logTranslation => some this.log'
	 * @return the result of calling  {@link #generateSBP(BooleanFormula, LeafInterpreter, SymmetryBreaker)}
	 * on the translation of annotated.node with respect to this.bounds
	 * @throws TrivialFormulaException - the translation of annotated is a constant or can be made into
	 * a constant by flattening 
	 */
	private Translation toBoolean(AnnotatedNode<Formula> annotated, SymmetryBreaker breaker) throws TrivialFormulaException {
		
		options.reporter().translatingToBoolean(annotated.node(), bounds);
		
		final LeafInterpreter interpreter = LeafInterpreter.exact(bounds, options);
		//final BooleanFactory factory = interpreter.factory();
		
		if (options.logTranslation()>0) {
			final TranslationLogger logger = options.logTranslation()==1 ? new MemoryLogger(annotated, bounds) : new FileLogger(annotated, bounds);
			BooleanAccumulator circuit = FOL2BoolTranslator.translate(annotated, interpreter, logger);
			log = logger.log();
			if (circuit.isShortCircuited()) {
				throw new TrivialFormulaException(annotated.node(), bounds, circuit.op().shortCircuit(), log);
			} else if (circuit.size()==0) { 
				throw new TrivialFormulaException(annotated.node(), bounds, circuit.op().identity(), log);
			}
			return generateSBP(circuit, interpreter, breaker);
		} else {
			BooleanValue circuit = (BooleanValue)FOL2BoolTranslator.translate(annotated, interpreter);
			if (circuit.op()==Operator.CONST) {
				throw new TrivialFormulaException(annotated.node(), bounds, (BooleanConstant)circuit, null);
			} 
			return generateSBP(annotated, (BooleanFormula)circuit, interpreter, breaker);
		}
	}
	
	/**
	 * Adds to given accumulator an SBP generated using the given symmetry breaker and interpreter,
	 * and returns the resulting circuit's translation to CNF.
	 * @requires circuit is a translation of this.formula with respect to this.bounds
	 * @requires interpreter is the leaf interpreter used in generating the given circuit
	 * @requires breaker.bounds = this.bounds
	 * @return toCNF(circuit && breaker.generateSBP(interpreter))
	 */
	private Translation generateSBP(BooleanAccumulator circuit, LeafInterpreter interpreter, SymmetryBreaker breaker) {
		options.reporter().generatingSBP();
		final BooleanFactory factory = interpreter.factory();
		circuit.add(breaker.generateSBP(interpreter, options.symmetryBreaking())); 
		return toCNF((BooleanFormula)factory.accumulate(circuit), factory.numberOfVariables(), interpreter.vars());
	}
	
	/**
	 * Conjoins the given circuit with an SBP generated using the given symmetry breaker and interpreter,
	 * and returns the resulting circuit's translation to CNF.
	 * @requires [[annotated.node]] <=> ([[this.formula]] and [[breaker.broken]])
	 * @requires circuit is a translation of annotated.node with respect to this.bounds
	 * @requires interpreter is the leaf interpreter used in generating the given circuit
	 * @requires breaker.bounds = this.bounds
	 * @return flatten(circuit && breaker.generateSBP(interpreter), interpreter)
	 * @throws TrivialFormulaException - flattening the circuit and the predicate yields a constant
	 */
	private Translation generateSBP(AnnotatedNode<Formula> annotated, BooleanFormula circuit, LeafInterpreter interpreter, SymmetryBreaker breaker) 
	throws TrivialFormulaException {
		options.reporter().generatingSBP();
		final BooleanFactory factory = interpreter.factory();
		final BooleanValue sbp = breaker.generateSBP(interpreter, options.symmetryBreaking()); 
		return flatten(annotated, (BooleanFormula)factory.and(circuit, sbp), interpreter);
	}

	/**
	 * If this.options.flatten is true, flattens the given circuit and returns its translation to CNF.
	 * Otherwise, simply returns the given circuit's translation to CNF.
	 * @requires [[annotated.node]] <=> ([[this.formula]] and [[breaker.broken]])
	 * @requires circuit is a translation of annotated.node with respect to this.bounds
	 * @requires interpreter is the leaf interpreter used in generating the given circuit
	 * @return if this.options.flatten then 
	 * 	toCNF(flatten(circuit), interpreter.factory().numberOfVariables(), interpreter.vars()) else
	 *  toCNF(circuit, interpreter.factory().numberOfVariables(), interpreter.vars())
	 * @throws TrivialFormulaException - flattening the circuit yields a constant
	 */
	private Translation flatten(AnnotatedNode<Formula> annotated, BooleanFormula circuit, LeafInterpreter interpreter) throws TrivialFormulaException {	
		final BooleanFactory factory = interpreter.factory();
		if (options.flatten()) {
			options.reporter().flattening(circuit);
			final BooleanValue flatCircuit = BooleanFormulaFlattener.flatten(circuit, factory);
			if (flatCircuit.op()==Operator.CONST) {
				throw new TrivialFormulaException(annotated.node(), bounds, (BooleanConstant)flatCircuit, null);
			} else {
				return toCNF((BooleanFormula)flatCircuit, factory.numberOfVariables(), interpreter.vars());
			}
		} else {
			return toCNF(circuit, factory.numberOfVariables(), interpreter.vars());
		}
	}
	
	/**
	 * Translates the given circuit to CNF, adds the clauses to a SATSolver returned
	 * by options.solver(), and returns a Translation object constructed from the solver
	 * and the provided arguments.
	 * @requires circuit is a translation of this.formula with respect to this.bounds
	 * @requires primaryVars is the number of primary variables generated by translating 
	 * this.formula and this.bounds into the given circuit
	 * @requires varUsage maps each non-constant relation in this.bounds to the labels of 
	 * the primary variables used to represent that relation in the given circuit
	 * @return Translation constructed from a SAT solver initialized with the CNF translation
	 * of the given circuit, the provided arguments, this.bounds, and this.log
	 */
	private Translation toCNF(BooleanFormula circuit, int primaryVars, Map<Relation,IntSet> varUsage) {	
		options.reporter().translatingToCNF(circuit);
		final SATSolver cnf = Bool2CNFTranslator.translate((BooleanFormula)circuit, options.solver(), primaryVars);
		return new Translation(cnf, bounds, varUsage, primaryVars, log);
	}
	
}
