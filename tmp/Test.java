package tmp;

import java.util.Arrays;
import java.util.List;
import kodkod.ast.*;
import kodkod.ast.operator.*;
import kodkod.instance.*;
import kodkod.engine.*;
import kodkod.engine.satlab.SATFactory;
import kodkod.engine.config.Options;

/* 
 ==================================================
 kodkod formula: 
 ==================================================
 #(this/A) = (#(this/B) + 1) && 
 Int/min = Int/min && 
 Int/zero = Int/zero && 
 Int/max = Int/max && 
 Int/next = Int/next && 
 seq/Int = seq/Int && 
 String = String && 
 this/A = this/A && 
 this/B = this/B
 ==================================================
 */
public final class Test {

    public static void main(String[] args) throws Exception {

        Relation x5 = Relation.unary("String");
        Relation x6 = Relation.unary("this/A");
        Relation x7 = Relation.unary("this/B");

        List<String> atomlist = Arrays.asList("A$0", "unused0", "unused1", "unused2", "unused3", "unused4", "unused5", "unused6",
                "unused7", "unused8");

        Universe universe = new Universe(atomlist);
        TupleFactory factory = universe.factory();
        Bounds bounds = new Bounds(universe);

        TupleSet x5_upper = factory.noneOf(1);
        bounds.boundExactly(x5, x5_upper);

        TupleSet x6_upper = factory.noneOf(1);
        x6_upper.add(factory.tuple("unused0"));
        x6_upper.add(factory.tuple("unused1"));
        x6_upper.add(factory.tuple("unused2"));
        x6_upper.add(factory.tuple("unused3"));
        x6_upper.add(factory.tuple("A$0"));
        bounds.bound(x6, x6_upper);

        TupleSet x7_upper = factory.noneOf(1);
        x7_upper.add(factory.tuple("unused4"));
        x7_upper.add(factory.tuple("unused5"));
        x7_upper.add(factory.tuple("unused6"));
        x7_upper.add(factory.tuple("unused7"));
        x7_upper.add(factory.tuple("unused8"));
        bounds.bound(x7, x7_upper);

        IntExpression x10 = x6.count();
        IntExpression x12 = x7.count();
        IntExpression x13 = IntConstant.constant(1);
        IntExpression x11 = x12.plus(x13);
        Formula x9 = x10.eq(x11);
        Formula x19 = x5.eq(x5);
        Formula x20 = x6.eq(x6);
        Formula x21 = x7.eq(x7);
        Formula x8 = Formula.compose(FormulaOperator.AND, x9, x19, x20, x21);

        Solver solver = new Solver();
        solver.options().setSolver(SATFactory.DefaultSAT4J);
        solver.options().setBitwidth(4);
        solver.options().setFlatten(false);
        solver.options().setIntEncoding(Options.IntEncoding.TWOSCOMPLEMENT);
        solver.options().setSymmetryBreaking(20);
        solver.options().setSkolemDepth(0);
        System.out.println("Solving...");
        System.out.flush();
        Solution sol = solver.solve(x8, bounds);
        System.out.println(sol.toString());
    }
}
