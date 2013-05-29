package kodkod.engine.fol2sat;

import static kodkod.ast.operator.FormulaOperator.AND;
import static kodkod.ast.operator.FormulaOperator.OR;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kodkod.ast.BinaryFormula;
import kodkod.ast.ComparisonFormula;
import kodkod.ast.ConstantFormula;
import kodkod.ast.Formula;
import kodkod.ast.IntComparisonFormula;
import kodkod.ast.IntExpression;
import kodkod.ast.MultiplicityFormula;
import kodkod.ast.NaryFormula;
import kodkod.ast.Node;
import kodkod.ast.NotFormula;
import kodkod.ast.QuantifiedFormula;
import kodkod.ast.RelationPredicate;
import kodkod.ast.operator.FormulaOperator;
import kodkod.ast.visitor.AbstractReplacer;
import kodkod.util.nodes.AnnotatedNode;

public class NNFConverter extends AbstractReplacer {

    public static AnnotatedNode<Formula> flatten(AnnotatedNode<Formula> annotated) {  
        final NNFConverter flat = new NNFConverter(annotated.sharedNodes());
        Formula f = annotated.node().accept(flat);
        final List<Formula> roots = new ArrayList<Formula>(flat.annotations.size());
        roots.addAll(flat.annotations.keySet());
        for(Iterator<Map.Entry<Formula,Node>> itr = flat.annotations.entrySet().iterator(); itr.hasNext(); ) { 
            final Map.Entry<Formula, Node> entry = itr.next();
            final Node source = annotated.sourceOf(entry.getValue());
            if (entry.getKey()==source)     { itr.remove(); /* TODO: what is this for? */ }
            else                            { entry.setValue(source); }
        }
        return AnnotatedNode.annotate(f, flat.annotations);
    }
    
    private  Map<Formula, Node> annotations;
    private boolean negated;
    
    protected NNFConverter(Set<Node> shared) {
        this(shared, new LinkedHashMap<Formula, Node>(), new IdentityHashMap<Node,Boolean>());
    }
    
    protected NNFConverter(Set<Node> shared, Map<Formula, Node> annotations, Map<Node, Boolean> visited) {
        super(shared);
        this.annotations = annotations;
        this.negated = false;
    }
    
    protected Formula addMapping(Formula f, Node source) {
        annotations.put(f, source);
        return f;
    }

    /**
     * Calls nf.formula.accept(this) after flipping the negation flag.
     * @see kodkod.ast.visitor.AbstractVoidVisitor#visit(kodkod.ast.NotFormula)
     */
    public final Formula visit(NotFormula nf) {
        //if (visited(bf)) return;
        negated = !negated;
        Formula f = nf.formula().accept(this);
        negated = !negated;
        if (f instanceof NotFormula) {
            if (((NotFormula) f).formula() == nf.formula()) {
                return addMapping(nf, nf);
            }            
        }
        return addMapping(f, nf);
    }
    
    /**
     * Visits the formula's children with appropriate settings
     * for the negated flag if bf  has not been visited before.
     * @see kodkod.ast.visitor.AbstractVoidVisitor#visit(kodkod.ast.BinaryFormula)
     */
    public final Formula visit(BinaryFormula bf) { 
        //if (visited(bf)) return;
        final FormulaOperator op = bf.op();
        switch (op) {
        case AND:
            if (!negated) {
                // left && right
                Formula lf = bf.left().accept(this);
                Formula rf = bf.right().accept(this);
                if (lf == bf.left() && rf == bf.right()) {
                    return addMapping(bf, bf);
                } else {
                    return addMapping(lf.and(rf), bf);
                }
            } else {
                // !(left && right) --> !left || !right
                Formula lf = bf.left().accept(this);
                Formula rf = bf.right().accept(this);
                return addMapping(lf.or(rf), bf);
            }
        case OR:
            if (!negated) {
                // left || right
                Formula lf = bf.left().accept(this);
                Formula rf = bf.right().accept(this);
                if (lf == bf.left() && rf == bf.right()) {
                    return addMapping(bf, bf);
                } else {
                    return addMapping(lf.or(rf), bf);
                }
            } else {
                // !(left || right) --> !left && !right
                Formula lf = bf.left().accept(this);
                Formula rf = bf.right().accept(this);
                return addMapping(lf.or(rf), bf);
            }
        case IMPLIES:
            if (!negated) {
                // left => right --> !left || right
                Formula lf = bf.left().not().accept(this);
                Formula rf = bf.right().accept(this);
                return addMapping(lf.or(rf), bf);
            } else {
                // !(left => right) --> left && !right
                negated = false;
                Formula lf = bf.left().accept(this);
                negated = true;
                Formula rf = bf.right().accept(this);
                return addMapping(lf.and(rf), bf);
            }
        case IFF: 
            if (!negated) {
                // a = b --> (a && b) || (!a && !b)
                Formula lf = bf.left().and(bf.right()).accept(this);
                Formula rf = bf.left().not().and(bf.right().not()).accept(this);
                return addMapping(lf.or(rf), bf);
            } else {
                // !(a = b) --> (a && !b) || (!a && b)
                negated = false;
                Formula lf = bf.left().and(bf.right().not()).accept(this);
                Formula rf = bf.left().not().and(bf.right()).accept(this);
                negated = true;
                return addMapping(lf.or(rf), bf);
            }
        default:
            return addMapping(bf, bf);
        } 
    }
    
    /**
     * Visits the formula's children with appropriate settings
     * for the negated flag if bf  has not been visited before.
     * @see kodkod.ast.visitor.AbstractVoidVisitor#visit(kodkod.ast.NaryFormula)
     */
    //TODO: probably don't needed
    public final Formula visit(NaryFormula nf) { 
        //if (visited(nf)) return;
        final FormulaOperator op = nf.op();
        if (negated && op==AND) {            
            List<Formula> formulas = new LinkedList<Formula>();
            for (Formula f : nf) {
                formulas.add(f.accept(this));
            }
            return addMapping(Formula.or(formulas), nf);
        } else if (negated && op==OR) {            
            List<Formula> formulas = new LinkedList<Formula>();
            for (Formula f : nf) {
                formulas.add(f.accept(this));
            }
            return addMapping(Formula.and(formulas), nf);            
        } else {
            List<Formula> formulas = new LinkedList<Formula>();
            boolean changed = false;
            for (Formula f : nf) {
                Formula ff = f.accept(this);
                changed = changed || ff != f;
                formulas.add(ff);
            }
            if (changed) {
                if (op==AND)
                    return addMapping(Formula.and(formulas), nf);
                else 
                    return addMapping(Formula.or(formulas), nf);
            } else {
                return addMapping(nf, nf);
            }
        }
    }
        
    /** @see #visitFormula(Formula) */
    public final Formula visit(IntComparisonFormula cf) {
        //if (visited(cf)) return;
        IntExpression lh = cf.left().accept(this);
        IntExpression rh = cf.right().accept(this);
        if (!negated) {
            if (lh == cf.left() && rh == cf.right()) {
                return addMapping(cf, cf);                
            } else {
                return addMapping(lh.compare(cf.op(), rh), cf);
            }
        } else {
            switch (cf.op()) {
            case GT:
                return addMapping(lh.lte(rh), cf);
            case GTE:
                return addMapping(lh.lt(rh), cf);
            case LT:
                return addMapping(lh.gte(rh), cf);
            case LTE:
                return addMapping(lh.gt(rh), cf);
            case EQ:
                return addMapping(lh.neq(rh), cf);
            case NEQ:
                return addMapping(lh.eq(rh), cf);
            default:   
                return addMapping(cf, cf);
            }            
        }
    }

    protected Formula addFormula(Formula f, Node src, boolean negOld) {
        negated = negOld;
        if (negated) {
            return addMapping(f.not(), src);
        } else {
            return addMapping(f, src);
        }
    }
    
    @Override
    public Formula visit(ConstantFormula constant) {
        boolean negOld = negated;
        negated = false;
        return addFormula(super.visit(constant), constant, negOld); 
    }

    @Override
    public Formula visit(QuantifiedFormula quantFormula) {
        boolean negOld = negated;
        negated = false;
        return addFormula(super.visit(quantFormula), quantFormula, negOld);
    }

    @Override
    public Formula visit(ComparisonFormula compFormula) {
        boolean negOld = negated;
        negated = false;
        return addFormula(super.visit(compFormula), compFormula, negOld);
    }

    @Override
    public Formula visit(MultiplicityFormula multFormula) {
        boolean negOld = negated;
        negated = false;
        return addFormula(super.visit(multFormula), multFormula, negOld);
    }

    @Override
    public Formula visit(RelationPredicate pred) {
        boolean negOld = negated;
        negated = false;
        return addFormula(super.visit(pred), pred, negOld);
    }
        
}
