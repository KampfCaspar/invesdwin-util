package de.invesdwin.util.collections.iterable;

public interface ICloseableIterable<E> extends Iterable<E> {

    @Override
    ICloseableIterator<E> iterator();

}
