package de.invesdwin.util.concurrent.taskinfo.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.concurrent.ThreadSafe;

import de.invesdwin.util.concurrent.callable.ImmutableCallable;
import de.invesdwin.util.concurrent.priority.IPriorityCallable;
import de.invesdwin.util.concurrent.priority.IPriorityProvider;
import de.invesdwin.util.concurrent.taskinfo.TaskInfoManager;
import de.invesdwin.util.lang.Objects;
import de.invesdwin.util.math.decimal.scaled.Percent;

@ThreadSafe
public final class TaskInfoCallable<V> implements IPriorityCallable<V>, ITaskInfoProvider {

    private final String name;
    private final Callable<V> delegate;
    private volatile TaskInfoStatus status;
    private final Callable<Percent> progress;

    private TaskInfoCallable(final String name, final Callable<V> delegate, final Callable<Percent> progress) {
        this.name = name;
        this.delegate = delegate;
        if (progress == null) {
            this.progress = new ImmutableCallable<Percent>(null);
        } else {
            this.progress = progress;
        }
        this.status = TaskInfoStatus.CREATED;
        TaskInfoManager.onCreated(this);
    }

    @Override
    public V call() throws Exception {
        this.status = TaskInfoStatus.STARTED;
        TaskInfoManager.onStarted(this);
        try {
            return delegate.call();
        } finally {
            TaskInfoManager.onCompleted(this);
            this.status = TaskInfoStatus.COMPLETED;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TaskInfoStatus getStatus() {
        return status;
    }

    @Override
    public Percent getProgress() {
        try {
            return progress.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getPriority() {
        if (delegate instanceof IPriorityProvider) {
            final IPriorityProvider cDelegate = (IPriorityProvider) delegate;
            return cDelegate.getPriority();
        }
        return MISSING_PRIORITY;
    }

    public static <T> TaskInfoCallable<T> of(final String name, final Callable<T> callable) {
        return of(name, callable, null);
    }

    public static <T> TaskInfoCallable<T> of(final String name, final Callable<T> callable,
            final Callable<Percent> progress) {
        return new TaskInfoCallable<>(name, callable, progress);
    }

    public static <T> List<TaskInfoCallable<T>> of(final String taskName,
            final Collection<? extends Callable<T>> tasks) {
        final List<TaskInfoCallable<T>> wrapped = new ArrayList<>(tasks.size());
        for (final Callable<T> task : tasks) {
            wrapped.add(TaskInfoCallable.of(taskName, task));
        }
        return wrapped;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("name", name).add("identity", hashCode()).toString();
    }

}
