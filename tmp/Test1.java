package tmp;
import java.util.Arrays;
import java.util.List;
import kodkod.ast.*;
import kodkod.ast.operator.*;
import kodkod.instance.*;
import kodkod.util.nodes.PrettyPrinter;
import kodkod.engine.*;
import kodkod.engine.satlab.SATFactory;
import kodkod.engine.config.Options;

public final class Test1 {

    public static void main(String[] args) throws Exception {

        Relation x0 = Relation.unary("Int/min");
        Relation x1 = Relation.unary("Int/zero");
        Relation x2 = Relation.unary("Int/max");
        Relation x3 = Relation.nary("Int/next", 2);
        Relation x4 = Relation.unary("seq/Int");
        Relation x5 = Relation.unary("String");
        Relation x6 = Relation.unary("this/R");

        List<String> atomlist = Arrays.asList("-1", "-2", "0", "1", "R$0", "R$1", "unused0");

        Universe universe = new Universe(atomlist);
        TupleFactory factory = universe.factory();
        Bounds bounds = new Bounds(universe);

        TupleSet x0_upper = factory.noneOf(1);
        x0_upper.add(factory.tuple("-2"));
        bounds.boundExactly(x0, x0_upper);

        TupleSet x1_upper = factory.noneOf(1);
        x1_upper.add(factory.tuple("0"));
        bounds.boundExactly(x1, x1_upper);

        TupleSet x2_upper = factory.noneOf(1);
        x2_upper.add(factory.tuple("1"));
        bounds.boundExactly(x2, x2_upper);

        TupleSet x3_upper = factory.noneOf(2);
        x3_upper.add(factory.tuple("-2").product(factory.tuple("-1")));
        x3_upper.add(factory.tuple("-1").product(factory.tuple("0")));
        x3_upper.add(factory.tuple("0").product(factory.tuple("1")));
        bounds.boundExactly(x3, x3_upper);

        TupleSet x4_upper = factory.noneOf(1);
        x4_upper.add(factory.tuple("0"));
        bounds.boundExactly(x4, x4_upper);

        TupleSet x5_upper = factory.noneOf(1);
        bounds.boundExactly(x5, x5_upper);

        TupleSet x6_upper = factory.noneOf(1);
        x6_upper.add(factory.tuple("unused0"));
        x6_upper.add(factory.tuple("R$0"));
        x6_upper.add(factory.tuple("R$1"));
        bounds.bound(x6, x6_upper);

        bounds.boundExactly(-2, factory.range(factory.tuple("-2"), factory.tuple("-2")));
        bounds.boundExactly(-1, factory.range(factory.tuple("-1"), factory.tuple("-1")));
        bounds.boundExactly(0, factory.range(factory.tuple("0"), factory.tuple("0")));
        bounds.boundExactly(1, factory.range(factory.tuple("1"), factory.tuple("1")));

        Formula x11 = x6.some();
        Formula x10 = x11.not();
        IntExpression x13 = x6.count();
        IntExpression x14 = IntConstant.constant(0);
        Formula x12 = x13.gt(x14);
        Formula x9 = x10.or(x12);
        Formula x8 = x9.not();
        Formula x15 = x0.eq(x0);
        Formula x16 = x1.eq(x1);
        Formula x17 = x2.eq(x2);
        Formula x18 = x3.eq(x3);
        Formula x19 = x4.eq(x4);
        Formula x20 = x5.eq(x5);
        Formula x21 = x6.eq(x6);
        Formula x7 = Formula.compose(FormulaOperator.AND, x8, x15, x16, x17, x18, x19, x20, x21);

        Solver solver = new Solver();
        solver.options().setSolver(SATFactory.DefaultSAT4J);
        solver.options().setBitwidth(2);
        solver.options().setFlatten(false);
        solver.options().setIntEncoding(Options.IntEncoding.TWOSCOMPLEMENT);
        solver.options().setSymmetryBreaking(20);
        solver.options().setSkolemDepth(0);
        System.out.println("Solving...");
        System.out.println(PrettyPrinter.print(x7, 2));
        System.out.println(bounds);
        System.out.flush();
        Solution sol = solver.solve(x7, bounds);
        System.out.println(sol.toString());
        Evaluator ev = new Evaluator(sol.instance(), solver.options());
        System.out.println(ev.evaluate(x7));
        System.out.println(ev.evaluate(x6.count()));
        System.out.println(ev.evaluate(x6.count().gt(IntConstant.constant(0))));
    }
}
