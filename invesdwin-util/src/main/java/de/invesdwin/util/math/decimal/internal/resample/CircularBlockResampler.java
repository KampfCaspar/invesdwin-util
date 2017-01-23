package de.invesdwin.util.math.decimal.internal.resample;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.math.decimal.ADecimal;
import de.invesdwin.util.math.decimal.IDecimalAggregate;
import de.invesdwin.util.math.decimal.config.BlockBootstrapConfig;
import de.invesdwin.util.math.decimal.internal.DecimalAggregate;
import de.invesdwin.util.math.decimal.internal.resample.blocklength.CircularOptimalBlockLength;

/**
 * http://www.math.ucsd.edu/~politis/SOFT/PPW/ppw.R
 * 
 * http://www.numericalmethod.com/javadoc/suanshu/com/numericalmethod/suanshu/stats/random/sampler/resampler/bootstrap/block/PattonPolitisWhite2009.html
 * 
 * https://github.com/colintbowers/DependentBootstrap.jl
 */
@NotThreadSafe
public class CircularBlockResampler<E extends ADecimal<E>> implements IDecimalResampler<E> {

    protected final RandomGenerator random = newRandomGenerator();
    private final int blockLength;
    private final List<E> sample;
    private final E converter;
    private final IDecimalResampler<E> delegate;
    private BlockBootstrapConfig config;

    public CircularBlockResampler(final DecimalAggregate<E> parent, final BlockBootstrapConfig config) {
        this.sample = parent.values();
        this.converter = parent.getConverter();
        this.config = config;
        if (config.getBlockLength() != null) {
            this.blockLength = config.getBlockLength();
        } else {
            this.blockLength = newOptimalBlockLength(parent);
        }
        Assertions.assertThat(blockLength).isGreaterThanOrEqualTo(1);
        if (blockLength == 1) {
            //blockwise resample makes no sense with block length 1
            delegate = new CaseReplacementResampler<E>(parent);
        } else {
            delegate = new IDecimalResampler<E>() {
                @Override
                public IDecimalAggregate<E> resample() {
                    return internalResample();
                }
            };
        }
    }

    protected int newOptimalBlockLength(final IDecimalAggregate<E> parent) {
        return new CircularOptimalBlockLength<E>(parent).getBlockLength();
    }

    @Override
    public final IDecimalAggregate<E> resample() {
        return delegate.resample();
    }

    protected int nextBlockLength() {
        return blockLength;
    }

    protected RandomGenerator newRandomGenerator() {
        return new JDKRandomGenerator();
    }

    private IDecimalAggregate<E> internalResample() {
        int curBlockLength;
        final int length = sample.size();
        final List<E> resample = new ArrayList<E>(length);
        for (int resampleIdx = 0; resampleIdx < length; resampleIdx += curBlockLength) {
            final int startIdx = (int) (random.nextLong() % length);
            curBlockLength = nextBlockLength();
            final int maxBlockIdx;
            if (resampleIdx + curBlockLength < length) {
                maxBlockIdx = curBlockLength;
            } else {
                maxBlockIdx = length - resampleIdx;
            }
            for (int blockIdx = 0; blockIdx < maxBlockIdx; blockIdx++) {
                final int valuesIndex = (startIdx + blockIdx) % length;
                resample.add(sample.get(valuesIndex));
            }
        }
        return new DecimalAggregate<E>(resample, converter);
    }

}