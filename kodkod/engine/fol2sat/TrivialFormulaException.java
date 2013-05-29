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

import kodkod.ast.Formula;
import kodkod.engine.bool.BooleanConstant;
import kodkod.instance.Bounds;

/**
 * Thrown when a formula is found to be trivially (un)satisfiable 
 * with respect to given Bounds.
 * 
 * @specfield formula: Formula // possibly de-sugared (skolemized) formula
 * @specfield bounds: Bounds // bounds (possibly a subset of the original bounds) with respect to which the formula reduces to a constant 
 * @specfield log: lone TranslationLog // log is null if translation logging was not enabled
 * @specfield value: BooleanConstant // the value to which the reduction simplified
 * @author Emina Torlak
 */
public final class TrivialFormulaException extends Exception {
	private final BooleanConstant value;
	private final TranslationLog log;
	private final Formula formula;
	private final Bounds bounds;
	
	private static final long serialVersionUID = 6251577831781586067L;

	/**
	 * Constructs a new TrivialFormulaException using the given arguments.  
	 * @requires log != null && bounds != null && value != null
	 * @ensures this.log' = log && this.formula' = log.formula && 
	 * this.bounds' = bounds && this.value' = value 
	 */
	 TrivialFormulaException(Formula formula, Bounds bounds, BooleanConstant formulaValue, TranslationLog log) {
		super("Trivially " + ((formulaValue==BooleanConstant.FALSE) ? "un" : "" )  + "satisfiable formula.");
		assert formulaValue != null && bounds != null;
		this.log = log;
		this.formula = formula;
		this.bounds = bounds;
		this.value = formulaValue;
	}

	/**
	 * Returns this.log.
	 * @return this.log
	 */
	public TranslationLog log() {
		return log;
	}
	
	/**
	 * Returns this.formula.
	 * @return this.formula
	 */
	public Formula formula() { 
		return formula;
	}
	
	/**
	 * Return this.bounds.
	 * @return this.bounds
	 */
	public Bounds bounds() {
		return bounds;
	}
	
	/**
	 * Returns the value to which this.formula is trivially reducible.
	 * @return this.value
	 */
	public BooleanConstant value() {
		return value;
	}

}
