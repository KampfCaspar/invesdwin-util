package de.invesdwin.util.math.decimal.internal.resample.blocklength;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.math.decimal.ADecimal;
import de.invesdwin.util.math.decimal.IDecimalAggregate;

/**
 * Politis, N. Dimitris, White Halbert, "Automatic Block-Length Selection for the Dependent Bootstrap", Econometric
 * Reviews , 2004
 * 
 * Politis, D., White, H., Patton Andrew,"CORRECTION TO 'Automatic Block-Length Selection for the Dependent Bootstrap'",
 * Econometric Reviews, 28(4):372–375, 2009
 * 
 * http://www.math.ucsd.edu/~politis/SOFT/PPW/ppw.R
 * 
 */
@NotThreadSafe
public class OptimalBlockLength<E extends ADecimal<E>> {
    private final List<E> sample;
    private final double sampleAvg;
    private final double sampleAutoCovariance0;

    public OptimalBlockLength(final IDecimalAggregate<E> parent) {
        this.sample = parent.values();
        this.sampleAvg = parent.avg().doubleValueRaw();
        this.sampleAutoCovariance0 = sampleAutoCovariance(0);
    }

    private long determineOptimalLag() {
        final int length = sample.size();
        final int a2 = determineOptimalLag_1(length);
        final long a3 = determineOptimalLag_2(length);
        final double a4 = determineOptimalLag_3(length);
        int a6 = 0;
        int a7 = 0;
        int a8 = 0;
        int a9 = 1;
        while (a9 <= a3) {
            if (Math.abs(sampleAutoCorrelation(a9)) > a4) {
                a6 = a9;
                a8 = 0;
            } else if (++a8 == a2) {
                a7 = a9 - a2 + 1;
                break;
            }
            a9++;
        }
        a7 = a7 == 0 ? a6 : a7;
        return 2 * a7 > a3 ? a3 : (long) (2 * a7);
    }

    private int determineOptimalLag_1(final int a) {
        return Math.max(5, (int) Math.ceil(Math.sqrt(Math.log10(a))));
    }

    private long determineOptimalLag_2(final int a) {
        return (long) Math.ceil(Math.sqrt(a)) + determineOptimalLag_1(a);
    }

    private double determineOptimalLag_3(final int a) {
        return 1.959964 * Math.sqrt(Math.log10(a) / a);
    }

    private double determineOptimalBlockLength_1(final double a) {
        final double a2 = Math.abs(a);
        return a2 <= 0.5 ? 1.0 : (a2 <= 1.0 ? 2.0 * (1.0 - a2) : 0.0);
    }

    private long determineOptimalBlockLength_2(final int a) {
        return (long) Math.ceil(Math.min(3.0 * Math.sqrt(a), a / 3.0));
    }

    public long getBlockLength() {
        final int length = sample.size();
        final long a2 = determineOptimalLag();
        double a3 = sampleAutoCovariance0;
        double a4 = 0.0;
        int a5 = 1;
        while (a5 <= a2) {
            final double a6 = determineOptimalBlockLength_1(1.0 * a5 / a2);
            final double a7 = sampleAutoCovariance(a5);
            a3 += 2.0 * a6 * a7;
            a4 += 2.0 * a6 * a5 * a7;
            ++a5;
        }
        final double a8 = a3 * a3 * determineOptimalBlockLength_multiplicator();
        double a9 = Math.pow(2.0 * a4 * a4 * length / a8, 0.3333333333333333);
        final double a10 = determineOptimalBlockLength_2(length);
        a9 = a9 > a10 ? a10 : a9;
        a9 = a9 < 1.0 ? 1.0 : a9;
        return Math.round(a9);
    }

    protected double determineOptimalBlockLength_multiplicator() {
        return 1.3333333333333333;
    }

    private double sampleAutoCorrelation(final int index) {
        return sampleAutoCovariance(index) / sampleAutoCovariance0;
    }

    private double sampleAutoCovariance(final int index) {
        Assertions.checkTrue(index < sample.size());
        final int length = sample.size();
        double a2 = 0.0;
        for (int i = 1; i <= length - index; ++i) {
            a2 += (sample.get(i).doubleValueRaw() - sampleAvg) * (sample.get(i + index).doubleValueRaw() - sampleAvg);
        }
        return a2 / length;
    }

}
