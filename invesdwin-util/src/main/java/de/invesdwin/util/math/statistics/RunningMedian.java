package de.invesdwin.util.math.statistics;

import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.collections.sorted.BisectSortedList;
import de.invesdwin.util.math.Doubles;

/**
 * https://code.activestate.com/recipes/576930/#c3
 */
@NotThreadSafe
public class RunningMedian {

    private final Deque<Double> queue = new LinkedList<Double>();
    private final List<Double> sortedList = newSortedList(Doubles.COMPARATOR);
    private final int size;

    public RunningMedian(final int size) {
        this.size = size;
    }

    @SuppressWarnings("rawtypes")
    protected List<Double> newSortedList(final Comparator comparator) {
        return new BisectSortedList<Double>(comparator);
    }

    public void add(final Double value) {
        if (queue.size() >= size) {
            final Double first = queue.removeFirst();
            Assertions.checkTrue(sortedList.remove(first));
        }
        queue.add(value);
        sortedList.add(value);
    }

    public Double getMedian() {
        if (sortedList.isEmpty()) {
            return null;
        }
        if (sortedList.size() == 1) {
            return sortedList.get(0);
        }
        final int middle = sortedList.size() / 2;
        final Double median;
        if (sortedList.size() % 2 == 0) {
            final Double medianA = sortedList.get(middle);
            final Double medianB = sortedList.get(middle - 1);
            median = (medianA + medianB) / 2D;
        } else {
            median = sortedList.get(middle);
        }
        return median;
    }

    public boolean isEmpty() {
        return sortedList.isEmpty();
    }

}
