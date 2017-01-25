package de.invesdwin.util.math.decimal.stream;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.math.decimal.ADecimal;

@Immutable
public class DecimalPoint<X, Y extends ADecimal<Y>> {

    private final X x;
    private final Y y;

    public DecimalPoint(final X x, final Y y) {
        this.x = x;
        this.y = y;
    }

    public X getX() {
        return x;
    }

    public Y getY() {
        return y;
    }

}