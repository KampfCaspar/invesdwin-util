package de.invesdwin.util.lang;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import de.invesdwin.util.collections.loadingcache.ALoadingCache;

@ThreadSafe
public class UniqueNameGenerator {

    private final ALoadingCache<String, AtomicLong> name_sequencenumber = new ALoadingCache<String, AtomicLong>() {
        @Override
        protected AtomicLong loadValue(final String key) {
            return new AtomicLong(getInitialValue());
        }
    };

    /**
     * Generates IDs in the schema of [Name]_[SequenceNumber].
     */
    public String get(final String name) {
        final long nextSequenceNumber = nextSequenceNumber(name);
        if (nextSequenceNumber == 1) {
            return name;
        } else {
            return Strings.addSuffixToFileName(name, wrapSequenceNumber(nextSequenceNumber - 1));
        }
    }

    protected String wrapSequenceNumber(final long sequenceNumber) {
        return "_" + sequenceNumber;
    }

    public synchronized long nextSequenceNumber(final String name) {
        final AtomicLong sequenceNumber = name_sequencenumber.get(name);
        long sequenceNumberIncremented = sequenceNumber.incrementAndGet();
        if (sequenceNumberIncremented == getResetAtValue()) {
            sequenceNumber.set(getInitialValue());
            sequenceNumberIncremented = sequenceNumber.incrementAndGet();
        }
        return sequenceNumberIncremented;
    }

    /**
     * Initialize with 1 to skip the first value without a number
     */
    protected long getInitialValue() {
        return 0;
    }

    protected long getResetAtValue() {
        return Long.MAX_VALUE;
    }

}
