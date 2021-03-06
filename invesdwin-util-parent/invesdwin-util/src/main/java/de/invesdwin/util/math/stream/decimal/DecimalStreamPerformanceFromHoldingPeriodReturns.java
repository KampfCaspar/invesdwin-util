package de.invesdwin.util.math.stream.decimal;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.norva.marker.ISerializableValueObject;
import de.invesdwin.util.math.decimal.Decimal;
import de.invesdwin.util.math.decimal.scaled.Percent;
import de.invesdwin.util.math.stream.IStreamAlgorithm;

@NotThreadSafe
public class DecimalStreamPerformanceFromHoldingPeriodReturns
        implements IStreamAlgorithm<Percent, Void>, ISerializableValueObject {

    private Decimal performance = getInitialValue();
    private Double performanceDouble = performance.doubleValue();
    private final double initialPerformance = performanceDouble;
    private final DecimalStreamProduct<Decimal> product = new DecimalStreamProduct<Decimal>(Decimal.ZERO) {
        @Override
        protected Decimal getValueAdjustmentAddition() {
            return Decimal.valueOf(Percent.ONE_HUNDRED_PERCENT.getRate());
        }
    };

    @Override
    public Void process(final Percent holdingPeriodReturn) {
        //improve accuracy by using log sum instead of multiplication directly for the HPRs
        product.process(Decimal.valueOf(holdingPeriodReturn.getRate()));
        performance = null;
        performanceDouble = null;
        return null;
    }

    public Decimal getPerformance() {
        if (performance == null) {
            performance = new Decimal(getPerformanceDouble());
        }
        return performance;
    }

    public double getPerformanceDouble() {
        if (performanceDouble == null) {
            performanceDouble = calculatePerformanceDouble();
        }
        return performanceDouble;
    }

    private double calculatePerformanceDouble() {
        final double multiplier = product.getProductDouble();
        final double performanceDouble = initialPerformance * (multiplier + 1D);
        return performanceDouble;
    }

    public Decimal getInitialValue() {
        return Decimal.ONE;
    }

}
