package de.invesdwin.util.math.decimal.scaled;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.math.Doubles;
import de.invesdwin.util.math.decimal.ADecimal;
import de.invesdwin.util.math.decimal.AScaledDecimal;
import de.invesdwin.util.math.decimal.Decimal;
import de.invesdwin.util.math.decimal.IScaledNumber;
import de.invesdwin.util.time.duration.Duration;
import de.invesdwin.util.time.fdate.FTimeUnit;

@Immutable
public class Percent extends AScaledDecimal<Percent, PercentScale> {

    public static final PercentScale DEFAULT_SCALE;
    public static final Percent THREE_HUNDRED_PERCENT;
    public static final Percent TWO_HUNDRED_PERCENT;
    public static final Percent ONE_HUNDRED_PERCENT;
    public static final Percent NINETY_PERCENT;
    public static final Percent SEVENTYFIVE_PERCENT;
    public static final Percent FIFTY_PERCENT;
    public static final Percent TWENTYFIVE_PERCENT;
    public static final Percent TEN_PERCENT;
    public static final Percent FIVE_PERCENT;
    public static final Percent TWO_PERCENT;
    public static final Percent ONE_PERCENT;
    public static final Percent ZERO_PERCENT;
    public static final Percent MINUS_ONE_PERCENT;
    public static final Percent MINUS_TWO_PERCENT;
    public static final Percent MINUS_FIVE_PERCENT;
    public static final Percent MINUS_TEN_PERCENT;

    static {
        DEFAULT_SCALE = PercentScale.RATE;
        THREE_HUNDRED_PERCENT = new Percent(3D, PercentScale.RATE);
        TWO_HUNDRED_PERCENT = new Percent(2D, PercentScale.RATE);
        ONE_HUNDRED_PERCENT = new Percent(1D, PercentScale.RATE);
        NINETY_PERCENT = new Percent(90D, PercentScale.PERCENT);
        SEVENTYFIVE_PERCENT = new Percent(75D, PercentScale.PERCENT);
        FIFTY_PERCENT = new Percent(50D, PercentScale.PERCENT);
        TWENTYFIVE_PERCENT = new Percent(25D, PercentScale.PERCENT);
        TEN_PERCENT = new Percent(10D, PercentScale.PERCENT);
        FIVE_PERCENT = new Percent(5D, PercentScale.PERCENT);
        TWO_PERCENT = new Percent(2D, PercentScale.PERCENT);
        ONE_PERCENT = new Percent(1D, PercentScale.PERCENT);
        ZERO_PERCENT = new Percent(0D, PercentScale.RATE);
        MINUS_ONE_PERCENT = ONE_PERCENT.negate();
        MINUS_TWO_PERCENT = TWO_PERCENT.negate();
        MINUS_FIVE_PERCENT = FIVE_PERCENT.negate();
        MINUS_TEN_PERCENT = TEN_PERCENT.negate();
    }

    public Percent(final double value, final PercentScale scale) {
        super(value, scale);
    }

    public Percent(final Decimal value, final PercentScale scale) {
        super(value.doubleValue(), scale);
    }

    public Percent(final Number dividend, final Number divisor) {
        this(dividend.doubleValue(), divisor.doubleValue());
    }

    public Percent(final Double dividend, final Double divisor) {
        this(dividend.doubleValue(), divisor.doubleValue());
    }

    public Percent(final double dividend, final double divisor) {
        this(newRate(dividend, divisor), PercentScale.RATE);
    }

    /**
     * Use default values of the scaled decimal instead! This constructor is functioning as a compiler warning for a
     * programming issue.
     */
    @Deprecated
    public Percent(final IScaledNumber dividend, final Number divisor) throws Exception {
        super(0D, PercentScale.PERCENT);
        throw new UnsupportedOperationException();
    }

    /**
     * Use default values of the scaled decimal instead! This constructor is functioning as a compiler warning for a
     * programming issue.
     */
    @Deprecated
    public Percent(final Number dividend, final IScaledNumber divisor) throws Exception {
        super(0D, PercentScale.PERCENT);
        throw new UnsupportedOperationException();
    }

    public Percent(final ADecimal<?> dividend, final ADecimal<?> divisor) {
        this(newRate(dividend.getDefaultValue(), divisor.getDefaultValue()), PercentScale.RATE);
        if (dividend instanceof AScaledDecimal) {
            final AScaledDecimal<?, ?> cDividend = (AScaledDecimal<?, ?>) dividend;
            cDividend.assertSameDefaultScale(divisor);
        }
    }

    public Percent(final Duration dividend, final Duration divisor) {
        this(dividend.doubleValue(FTimeUnit.MILLISECONDS), divisor.doubleValue(FTimeUnit.MILLISECONDS));
    }

    public <T extends AScaledDecimal<T, ?>> Percent(final AScaledDecimal<T, ?> dividend,
            final AScaledDecimal<T, ?> divisor) {
        this(newRate(dividend.getDefaultValue(), divisor.getDefaultValue()), PercentScale.RATE);
        dividend.assertSameDefaultScale(divisor);
    }

    public Percent(final Percent percent) {
        this(percent.getValue(percent.getScale()), percent.getScale());
    }

    private static double newRate(final double dividend, final double divisor) {
        //workaround for both values being 0
        if (Doubles.compare(dividend, divisor) >= 0) {
            return 1D;
        } else {
            return Doubles.divide(dividend, divisor);
        }
    }

    @Override
    public PercentScale getDefaultScale() {
        return DEFAULT_SCALE;
    }

    @Override
    protected Percent getGenericThis() {
        return this;
    }

    @Override
    protected Percent newValueCopy(final double value, final PercentScale scale) {
        return new Percent(value, scale);
    }

    @Override
    public Percent zero() {
        return ZERO_PERCENT;
    }

    public static Percent nullToZero(final Percent value) {
        if (value == null) {
            return ZERO_PERCENT;
        } else {
            return value;
        }
    }

    public double getRate() {
        return getDefaultValue();
    }

    /**
     * (newValue - oldValue) / abs(oldValue)
     */
    public static <T extends ADecimal<T>> Percent relativeDifference(final ADecimal<T> oldValue,
            final ADecimal<T> newValue) {
        return new Percent(newValue.subtract(oldValue), oldValue.abs());
    }

    /**
     * (newValue - oldValue) / abs(oldValue)
     */
    public static Percent relativeDifference(final double oldValue, final double newValue) {
        return new Percent(newValue - oldValue, Doubles.abs(oldValue));
    }

    public static void putPercent(final ByteBuffer buffer, final Percent value) {
        if (value == null) {
            buffer.putDouble(Double.MIN_VALUE);
        } else {
            buffer.putDouble(value.getRate());
        }
    }

    public static Percent extractPercent(final ByteBuffer buffer, final int index) {
        final double value = buffer.getDouble(index);
        return extractPercent(value);
    }

    public static Percent extractPercent(final ByteBuffer buffer) {
        final double value = buffer.getDouble();
        return extractPercent(value);
    }

    public static Percent extractPercent(final double value) {
        if (value == Double.MIN_VALUE) {
            return null;
        } else {
            return new Percent(value, PercentScale.RATE);
        }
    }

}
