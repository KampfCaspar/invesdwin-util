package de.invesdwin.util.math.expression.eval;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.math.expression.ExpressionType;
import de.invesdwin.util.math.expression.variable.AConstant;
import de.invesdwin.util.math.expression.variable.IVariable;

@Immutable
public final class Variables {

    public static final IVariable PI = new AConstant(Math.PI) {

        @Override
        public String getExpressionName() {
            return "pi";
        }

        @Override
        public String getName() {
            return "PI";
        }

        @Override
        public String getDescription() {
            return "The {@code double} value that is closer than any other to "
                    + "<i>pi</i>, the ratio of the circumference of a circle to its diameter: " + Math.PI;
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.Double;
        }

    };

    public static final IVariable EULER = new AConstant(Math.E) {

        @Override
        public String getExpressionName() {
            return "euler";
        }

        @Override
        public String getName() {
            return "Euler";
        }

        @Override
        public String getDescription() {
            return "The double value that is closer than any other to "
                    + "<i>e</i>, the base of the natural logarithms: " + Math.E;
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.Double;
        }

    };

    private Variables() {}

}
