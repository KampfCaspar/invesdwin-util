package de.invesdwin.util.math.expression.eval.operation;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.math.Doubles;
import de.invesdwin.util.math.expression.eval.IParsedExpression;
import de.invesdwin.util.math.expression.function.IPreviousKeyFunction;
import de.invesdwin.util.time.fdate.FDate;

// AvgTrueRange(10) crosses above AvgTrueRange(20)
// ATR(10)[0] > ATR(20)[0] && ATR(10)[1] <= ATR(20)[1]
@Immutable
public class CrossesAboveOperation extends BinaryOperation {

    private final IPreviousKeyFunction leftPreviousKeyFunction;
    private final IPreviousKeyFunction rightPreviousKeyFunction;

    public CrossesAboveOperation(final IParsedExpression left, final IParsedExpression right,
            final IPreviousKeyFunction leftPreviousKeyFunction, final IPreviousKeyFunction rightPreviousKeyFunction) {
        super(Op.CROSSES_ABOVE, left, right);
        this.leftPreviousKeyFunction = leftPreviousKeyFunction;
        this.rightPreviousKeyFunction = rightPreviousKeyFunction;
    }

    @Override
    public double evaluateDouble(final FDate key) {
        //crosses above => left was below but went above right

        final double leftValue0 = left.evaluateDouble(key);
        final double rightValue0 = right.evaluateDouble(key);
        //left is above right
        if (Doubles.compare(leftValue0, rightValue0) > 0) {
            final FDate leftPreviousKey = leftPreviousKeyFunction.getPreviousKey(key, 1);
            final double leftValue1 = leftPreviousKeyFunction.evaluateDouble(left, leftPreviousKey);
            final FDate rightPreviousKey = rightPreviousKeyFunction.getPreviousKey(key, 1);
            final double rightValue1 = rightPreviousKeyFunction.evaluateDouble(right, rightPreviousKey);
            //previous left is below or equal to previous right
            if (Doubles.compare(leftValue1, rightValue1) <= 0) {
                return 1D;
            }
        }

        return 0D;
    }

    @Override
    public double evaluateDouble(final int key) {
        //crosses above => left was below but went above right

        final double leftValue0 = left.evaluateDouble(key);
        final double rightValue0 = right.evaluateDouble(key);
        //left is above right
        if (Doubles.compare(leftValue0, rightValue0) > 0) {
            final int leftPreviousKey = leftPreviousKeyFunction.getPreviousKey(key, 1);
            final double leftValue1 = leftPreviousKeyFunction.evaluateDouble(left, leftPreviousKey);
            final int rightPreviousKey = rightPreviousKeyFunction.getPreviousKey(key, 1);
            final double rightValue1 = rightPreviousKeyFunction.evaluateDouble(right, rightPreviousKey);
            //previous left is below or equal to previous right
            if (Doubles.compare(leftValue1, rightValue1) <= 0) {
                return 1D;
            }
        }

        return 0D;
    }

    @Override
    public double evaluateDouble() {
        throw new UnsupportedOperationException("crosses below operation is only supported with time or int index");
    }

    @Override
    public Boolean evaluateBooleanNullable(final FDate key) {
        //crosses above => left was below but went above right

        final double leftValue0 = left.evaluateDouble(key);
        final double rightValue0 = right.evaluateDouble(key);
        //left is above right
        if (Doubles.compare(leftValue0, rightValue0) > 0) {
            final FDate leftPreviousKey = leftPreviousKeyFunction.getPreviousKey(key, 1);
            final double leftValue1 = leftPreviousKeyFunction.evaluateDouble(left, leftPreviousKey);
            final FDate rightPreviousKey = rightPreviousKeyFunction.getPreviousKey(key, 1);
            final double rightValue1 = rightPreviousKeyFunction.evaluateDouble(right, rightPreviousKey);
            //previous left is below or equal to previous right
            if (Doubles.compare(leftValue1, rightValue1) <= 0) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    @Override
    public Boolean evaluateBooleanNullable(final int key) {
        //crosses above => left was below but went above right

        final double leftValue0 = left.evaluateDouble(key);
        final double rightValue0 = right.evaluateDouble(key);
        //left is above right
        if (Doubles.compare(leftValue0, rightValue0) > 0) {
            final int leftPreviousKey = leftPreviousKeyFunction.getPreviousKey(key, 1);
            final double leftValue1 = leftPreviousKeyFunction.evaluateDouble(left, leftPreviousKey);
            final int rightPreviousKey = rightPreviousKeyFunction.getPreviousKey(key, 1);
            final double rightValue1 = rightPreviousKeyFunction.evaluateDouble(right, rightPreviousKey);
            //previous left is below or equal to previous right
            if (Doubles.compare(leftValue1, rightValue1) <= 0) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    @Override
    public Boolean evaluateBooleanNullable() {
        throw new UnsupportedOperationException("crosses below operation is only supported with time or int index");
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    protected BinaryOperation newBinaryOperation(final Op op, final IParsedExpression left,
            final IParsedExpression right) {
        return new CrossesAboveOperation(left, right, leftPreviousKeyFunction, rightPreviousKeyFunction);
    }

    @Override
    public IParsedExpression simplify() {
        if (leftPreviousKeyFunction == rightPreviousKeyFunction) {
            return new SimpleCrossesAboveOperation(left, right, leftPreviousKeyFunction).simplify();
        } else {
            return super.simplify();
        }
    }

}
