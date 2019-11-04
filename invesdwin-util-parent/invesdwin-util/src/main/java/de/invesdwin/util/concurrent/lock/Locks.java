package de.invesdwin.util.concurrent.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.Immutable;

import com.google.common.util.concurrent.CycleDetectingLockFactory;
import com.google.common.util.concurrent.CycleDetectingLockFactory.Policies;
import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

import de.invesdwin.norva.apt.staticfacade.StaticFacadeDefinition;
import de.invesdwin.util.concurrent.lock.internal.ALocksStaticFacade;
import de.invesdwin.util.concurrent.lock.internal.TimeoutLock;
import de.invesdwin.util.concurrent.lock.internal.TimeoutReentrantLock;
import de.invesdwin.util.concurrent.lock.internal.TracedLock;
import de.invesdwin.util.concurrent.lock.internal.TracedReentrantLock;
import de.invesdwin.util.concurrent.lock.internal.WrappedLock;
import de.invesdwin.util.concurrent.lock.internal.WrappedReentrantLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.TimeoutReadWriteLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.TimeoutReentrantReadWriteLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.TracedReadWriteLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.TracedReentrantReadWriteLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.WrappedReadWriteLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.WrappedReentrantReadWriteLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.update.TimeoutReadWriteUpdateLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.update.TracedReadWriteUpdateLock;
import de.invesdwin.util.concurrent.lock.internal.readwrite.update.WrappedReadWriteUpdateLock;
import de.invesdwin.util.concurrent.lock.readwrite.IReadWriteLock;
import de.invesdwin.util.concurrent.lock.readwrite.IReadWriteUpdateLock;
import de.invesdwin.util.concurrent.lock.readwrite.IReentrantReadWriteLock;
import de.invesdwin.util.concurrent.lock.trace.ILockTrace;
import de.invesdwin.util.concurrent.lock.trace.internal.DisabledLockTrace;
import de.invesdwin.util.concurrent.lock.trace.internal.EnabledLockTrace;
import de.invesdwin.util.lang.UniqueNameGenerator;
import de.invesdwin.util.time.duration.Duration;

@StaticFacadeDefinition(name = "de.invesdwin.util.concurrent.lock.internal.ALocksStaticFacade", targets = {
        com.googlecode.concurentlocks.Locks.class })
@Immutable
public final class Locks extends ALocksStaticFacade {

    private static final UniqueNameGenerator UNIQUE_NAME_GENERATOR = new UniqueNameGenerator();

    private static ILockTrace lockTrace = DisabledLockTrace.INSTANCE;
    private static Duration lockWaitTimeout = null;

    /**
     * Keep it disabled by default to keep best performance.
     */
    private static CycleDetectingLockFactory cycleDetectingLockFactory = CycleDetectingLockFactory
            .newInstance(Policies.DISABLED);

    private Locks() {}

    public static IReentrantLock newReentrantLock(final String lockName) {
        final String name = UNIQUE_NAME_GENERATOR.get(lockName);
        final ReentrantLock lock = cycleDetectingLockFactory.newReentrantLock(name);
        return maybeWrap(name, lock);
    }

    public static IReentrantLock newReentrantLock(final String lockName, final boolean fair) {
        final String name = UNIQUE_NAME_GENERATOR.get(lockName);
        final ReentrantLock lock = cycleDetectingLockFactory.newReentrantLock(name, fair);
        return maybeWrap(name, lock);
    }

    public static IReentrantReadWriteLock newReentrantReadWriteLock(final String lockName) {
        final String name = UNIQUE_NAME_GENERATOR.get(lockName);
        final ReentrantReadWriteLock lock = cycleDetectingLockFactory.newReentrantReadWriteLock(name);
        return maybeWrap(name, lock);
    }

    public static IReentrantReadWriteLock newReentrantReadWriteLock(final String lockName, final boolean fair) {
        final String name = UNIQUE_NAME_GENERATOR.get(lockName);
        final ReentrantReadWriteLock lock = cycleDetectingLockFactory.newReentrantReadWriteLock(name, fair);
        return maybeWrap(name, lock);
    }

    /**
     * The read write update lock is slow but can be used to debug scenarios where the same thread locks the read lock
     * and then tries to upgrade to a write lock. This lock implementation will throw an exception for that case.
     * Otherwise after the code works with this lock implementation, it is faster to create a different update lock
     * yourself and use that in combination with a normal read write lock.
     * 
     * Though if performance is not critical, you can just stick to this implementation.
     */
    public static IReadWriteUpdateLock newReentrantReadWriteUpdateLock(final String lockName) {
        final String name = UNIQUE_NAME_GENERATOR.get(lockName);
        final ReentrantReadWriteUpdateLock lock = new ReentrantReadWriteUpdateLock();
        return maybeWrap(name, lock);
    }

    public static void setCycleDetectingLockFactory(final CycleDetectingLockFactory cycleDetectingLockFactory) {
        Locks.cycleDetectingLockFactory = cycleDetectingLockFactory;
    }

    /**
     * Cycle detection is disabled per default for performance purposes, using this you can enable it for debugging
     * purposes when required.
     */
    public static void setCycleDetectingPolicy(final Policies cycleDetectingPolicy) {
        setCycleDetectingLockFactory(CycleDetectingLockFactory.newInstance(cycleDetectingPolicy));
    }

    public static ILock maybeWrap(final String lockName, final Lock lock) {
        if (lock instanceof ILock) {
            return (ILock) lock;
        } else {
            return maybeWrapTimeout(maybeWrapTrace(lockName, lock));
        }
    }

    private static ILock maybeWrapTimeout(final ILock lock) {
        final Duration lockWaitTimeoutCopy = getLockWaitTimeout();
        if (lockWaitTimeoutCopy == null) {
            return lock;
        } else {
            return new TimeoutLock(lock, lockWaitTimeoutCopy);
        }
    }

    private static ILock maybeWrapTrace(final String lockName, final Lock lock) {
        if (isLockTraceEnabled()) {
            return new TracedLock(lockName, lock);
        } else {
            return new WrappedLock(lockName, lock);
        }
    }

    public static IReentrantLock maybeWrap(final String lockName, final ReentrantLock lock) {
        if (lock instanceof IReentrantLock) {
            return (IReentrantLock) lock;
        } else {
            return maybeWrapTimeout(maybeWrapTrace(lockName, lock));
        }
    }

    private static IReentrantLock maybeWrapTimeout(final IReentrantLock lock) {
        final Duration lockWaitTimeoutCopy = getLockWaitTimeout();
        if (lockWaitTimeoutCopy == null) {
            return lock;
        } else {
            return new TimeoutReentrantLock(lock, lockWaitTimeoutCopy);
        }
    }

    private static IReentrantLock maybeWrapTrace(final String lockName, final ReentrantLock lock) {
        if (isLockTraceEnabled()) {
            return new TracedReentrantLock(lockName, lock);
        } else {
            return new WrappedReentrantLock(lockName, lock);
        }
    }

    public static IReadWriteLock maybeWrap(final String lockName, final ReadWriteLock lock) {
        if (lock instanceof IReadWriteLock) {
            return (IReadWriteLock) lock;
        } else {
            return maybeWrapTimeout(maybeWrapTrace(lockName, lock));
        }
    }

    private static IReadWriteLock maybeWrapTimeout(final IReadWriteLock lock) {
        final Duration lockWaitTimeoutCopy = getLockWaitTimeout();
        if (lockWaitTimeoutCopy == null) {
            return lock;
        } else {
            return new TimeoutReadWriteLock(lock, lockWaitTimeoutCopy);
        }
    }

    private static IReadWriteLock maybeWrapTrace(final String lockName, final ReadWriteLock lock) {
        if (isLockTraceEnabled()) {
            return new TracedReadWriteLock(lockName, lock);
        } else {
            return new WrappedReadWriteLock(lockName, lock);
        }
    }

    public static IReentrantReadWriteLock maybeWrap(final String lockName, final ReentrantReadWriteLock lock) {
        if (lock instanceof IReentrantReadWriteLock) {
            return (IReentrantReadWriteLock) lock;
        } else {
            return maybeWrapTimeout(maybeWrapTrace(lockName, lock));
        }
    }

    private static IReentrantReadWriteLock maybeWrapTimeout(final IReentrantReadWriteLock lock) {
        final Duration lockWaitTimeoutCopy = getLockWaitTimeout();
        if (lockWaitTimeoutCopy == null) {
            return lock;
        } else {
            return new TimeoutReentrantReadWriteLock(lock, lockWaitTimeoutCopy);
        }
    }

    private static IReentrantReadWriteLock maybeWrapTrace(final String lockName, final ReentrantReadWriteLock lock) {
        if (isLockTraceEnabled()) {
            return new TracedReentrantReadWriteLock(lockName, lock);
        } else {
            return new WrappedReentrantReadWriteLock(lockName, lock);
        }
    }

    public static IReadWriteUpdateLock maybeWrap(final String lockName, final ReadWriteUpdateLock lock) {
        if (lock instanceof IReadWriteUpdateLock) {
            return (IReadWriteUpdateLock) lock;
        } else {
            return maybeWrapTimeout(maybeWrapTrace(lockName, lock));
        }
    }

    private static IReadWriteUpdateLock maybeWrapTimeout(final IReadWriteUpdateLock lock) {
        final Duration lockWaitTimeoutCopy = getLockWaitTimeout();
        if (lockWaitTimeoutCopy == null) {
            return lock;
        } else {
            return new TimeoutReadWriteUpdateLock(lock, lockWaitTimeoutCopy);
        }
    }

    private static IReadWriteUpdateLock maybeWrapTrace(final String lockName, final ReadWriteUpdateLock lock) {
        if (isLockTraceEnabled()) {
            return new TracedReadWriteUpdateLock(lockName, lock);
        } else {
            return new WrappedReadWriteUpdateLock(lockName, lock);
        }
    }

    public static ILockTrace getLockTrace() {
        return lockTrace;
    }

    public static void setLockTraceEnabled(final boolean lockTraceEnabled) {
        if (lockTraceEnabled) {
            if (!(Locks.lockTrace instanceof EnabledLockTrace)) {
                Locks.lockTrace = new EnabledLockTrace();
            }
        } else {
            Locks.lockTrace = DisabledLockTrace.INSTANCE;
        }
    }

    public static boolean isLockTraceEnabled() {
        return Locks.lockTrace != DisabledLockTrace.INSTANCE;
    }

    public static boolean isLockWaitTimeoutEnabled() {
        return lockWaitTimeout != null;
    }

    public static Duration getLockWaitTimeout() {
        return lockWaitTimeout;
    }

    public static void setLockWaitTimeout(final Duration lockWaitTimeout) {
        Locks.lockWaitTimeout = lockWaitTimeout;
    }

}
