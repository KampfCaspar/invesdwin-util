// CHECKSTYLE:OFF
package de.invesdwin.util.collections.loadingcache.historical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import com.google.common.collect.Iterables;

import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.bean.tuple.Pair;
import de.invesdwin.util.collections.iterable.buffer.BufferingIterator;
import de.invesdwin.util.collections.loadingcache.historical.key.APullingHistoricalCacheAdjustKeyProvider;
import de.invesdwin.util.collections.loadingcache.historical.key.APushingHistoricalCacheAdjustKeyProvider;
import de.invesdwin.util.collections.loadingcache.historical.key.IHistoricalCacheAdjustKeyProvider;
import de.invesdwin.util.collections.loadingcache.historical.refresh.HistoricalCacheRefreshManager;
import de.invesdwin.util.time.fdate.FDate;
import de.invesdwin.util.time.fdate.FDateBuilder;

@ThreadSafe
public class AGapHistoricalCacheWithUnlimitedCacheTest {
    //CHECKSTYLE:ON

    //marker object
    private final List<FDate> entities;

    private int countReadAllValuesAscendingFrom;
    private int countReadNewestValueTo;
    private int countInnerExtractKey;
    private int countAdjustKey;
    private boolean returnNullInReadNewestValueTo;
    private boolean returnAllInReadAllValuesAscendingFrom;
    private Integer returnMaxResults;
    private final int testReturnMaxResultsValue = 2;
    private final TestGapHistoricalCache cache = new TestGapHistoricalCache();

    public AGapHistoricalCacheWithUnlimitedCacheTest() {
        this.entities = new ArrayList<FDate>();
        entities.add(FDateBuilder.newDate(1990, 1, 1));
        entities.add(FDateBuilder.newDate(1991, 1, 1));
        entities.add(FDateBuilder.newDate(1992, 1, 1));
        entities.add(FDateBuilder.newDate(1993, 1, 1));
        entities.add(FDateBuilder.newDate(1994, 1, 1));
        entities.add(FDateBuilder.newDate(1995, 1, 1));
    }

    @Test
    public void testInconsistentGapKey() {
        FDate searchedKey = entities.get(0);
        FDate value = cache.query().getValue(searchedKey);
        Assertions.assertThat(value).isEqualTo(searchedKey);

        searchedKey = entities.get(1);
        value = cache.query().getValue(searchedKey.addDays(1));
        Assertions.assertThat(value).isEqualTo(searchedKey);
    }

    @Test
    public void testGaps() {
        //once through the complete list
        final List<FDate> liste = new ArrayList<FDate>();
        for (final FDate entity : entities) {
            final FDate cachedEntity = cache.query().getValue(entity);
            liste.add(cachedEntity);
            Assertions.assertThat(cachedEntity).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(liste).isEqualTo(entities);

        //new maxKey without new db results
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(5))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //new minKey without new db limit
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().withFuture().getValue(entity.addYears(-5))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //again in the same limit
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(3);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //random order
        for (final FDate entity : new HashSet<FDate>(entities)) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(3);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //simulate cache eviction
        cache.clear();
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        cache.clear();
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(6);
    }

    @Test
    public void testGapsWithReturnMaxResults() {
        returnMaxResults = testReturnMaxResultsValue;

        //once through the complete list
        final List<FDate> liste = new ArrayList<FDate>();
        for (final FDate entity : entities) {
            final FDate cachedEntity = cache.query().getValue(entity);
            liste.add(cachedEntity);
            Assertions.assertThat(cachedEntity).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(liste).isEqualTo(entities);

        //new maxKey without new db results
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(5))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(7);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //new minKey without new db limit
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().withFuture().getValue(entity.addYears(-5))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(7);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //again in the same limit
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(12);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //random order
        for (final FDate entity : new HashSet<FDate>(entities)) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(12);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //simulate cache eviction
        cache.clear();
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        cache.clear();
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addDays(2))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(22);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(6);
    }

    @Test
    public void testOneResult() {
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addYears(5))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNoResultsUp() {
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().getValue(entity.addYears(100))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNoResultsDown() {
        for (final FDate entity : entities) {
            Assertions.assertThat(cache.query().withFuture().getValue(entity.addYears(-100))).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    /**
     * for reverse sorting this is less efficient, this costs O(n) for queries.
     */
    @Test
    public void testInverseOrder() {
        final List<FDate> ents = new ArrayList<FDate>(entities);
        Collections.reverse(ents);
        for (final FDate entity : ents) {
            Assertions.assertThat(cache.query().getValue(entity)).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(4);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testInverseOrderWithReturnMaxResults() {
        returnMaxResults = testReturnMaxResultsValue;

        final List<FDate> ents = new ArrayList<FDate>(entities);
        Collections.reverse(ents);
        for (final FDate entity : ents) {
            Assertions.assertThat(cache.query().getValue(entity)).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(4);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKey() {
        FDate previousKey = cache.query().getPreviousKey(new FDate(), entities.size());
        Assertions.assertThat(previousKey).isEqualTo(entities.get(0));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);

        previousKey = cache.query().getPreviousKey(new FDate(), 1);
        Assertions.assertThat(previousKey).isEqualTo(entities.get(entities.size() - 2));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextKey() {
        FDate previousKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isEqualTo(entities.get(entities.size() - 1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, 1);
        Assertions.assertThat(previousKey).isEqualTo(entities.get(1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValueWithDistance() {
        FDate previousValue = cache.query().getPreviousValue(new FDate(), entities.size());
        Assertions.assertThat(previousValue).isSameAs(entities.get(0));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);

        previousValue = cache.query().getPreviousValue(new FDate(), 1);
        Assertions.assertThat(previousValue).isSameAs(entities.get(entities.size() - 2));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextValueWithDistance() {
        FDate previousValue = cache.query().withFuture().getNextValue(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousValue).isSameAs(entities.get(entities.size() - 1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousValue = cache.query().withFuture().getNextValue(FDate.MIN_DATE, 1);
        Assertions.assertThat(previousValue).isSameAs(entities.get(1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValueWithoutDistance() {
        FDate previousValue = cache.query()

                .getPreviousValue(entities.get(entities.size() - 1), entities.size());
        Assertions.assertThat(previousValue).isSameAs(entities.get(0));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);

        previousValue = cache.query().getPreviousValue(entities.get(entities.size() - 1), 1);
        Assertions.assertThat(previousValue).isSameAs(entities.get(entities.size() - 2));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextValueWithoutDistance() {
        FDate nextValue = cache.query().withFuture().getNextValue(entities.get(0), entities.size());
        Assertions.assertThat(nextValue).isSameAs(entities.get(entities.size() - 1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        nextValue = cache.query().withFuture().getNextValue(entities.get(0), 1);
        Assertions.assertThat(nextValue).isSameAs(entities.get(1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeys() {
        final Collection<FDate> previousKeys = asList(cache.query().getPreviousKeys(new FDate(), entities.size()));
        Assertions.assertThat(previousKeys).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testPreviousKeysBeforeFirst() {
        Collection<FDate> previousKeys = asList(
                cache.query().withFuture().getPreviousKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousKeys).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKeys = asList(cache.query().withFutureNull().getPreviousKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousKeys).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKeys = asList(cache.query().withFuture().getPreviousKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousKeys).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeyBeforeFirst() {
        FDate previousKey = cache.query().withFuture().getPreviousKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isNull();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().withFutureNull().getPreviousKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isNull();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().withFuture().getPreviousKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isNull();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeysBeforeFirstReverse() {
        Collection<FDate> previousKeys = asList(
                cache.query().withFutureNull().getPreviousKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousKeys).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKeys = asList(cache.query().withFuture().getPreviousKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousKeys).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKeys = asList(cache.query().withFutureNull().getPreviousKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousKeys).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeyBeforeFirstReverse() {
        FDate previousKey = cache.query().withFutureNull().getPreviousKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isNull();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().withFuture().getPreviousKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isNull();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().withFutureNull().getPreviousKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(previousKey).isNull();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNextKeys() {
        final Collection<FDate> nextKeys = asList(
                cache.query().withFuture().getNextKeys(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(nextKeys).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    private <T> List<T> asList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<T>();
        Iterables.addAll(list, iterable);
        return list;
    }

    @Test
    public void testKeys() {
        final Iterable<FDate> iterable = cache.query().getKeys(FDate.MIN_DATE, FDate.MAX_DATE);
        final List<FDate> previousKeys = new ArrayList<FDate>();
        for (final FDate d : iterable) {
            previousKeys.add(d);
        }
        Assertions.assertThat(previousKeys).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testValues() {
        final Iterable<FDate> iterable = cache.query().getValues(entities.get(0).addMilliseconds(1), FDate.MAX_DATE);
        final List<FDate> previousKeys = new ArrayList<FDate>();
        for (final FDate d : iterable) {
            previousKeys.add(d);
        }
        Assertions.assertThat(previousKeys).isEqualTo(entities.subList(1, entities.size()));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testKeysWithReturnMaxResults() {
        returnMaxResults = testReturnMaxResultsValue;

        final Iterable<FDate> iterable = cache.query().getKeys(FDate.MIN_DATE, FDate.MAX_DATE);
        final List<FDate> previousKeys = new ArrayList<FDate>();
        for (final FDate d : iterable) {
            previousKeys.add(d);
        }
        Assertions.assertThat(previousKeys).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValuesWithDistance() {
        final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(new FDate(), entities.size()));
        Assertions.assertThat(previousValues).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextValuesWithDistance() {
        final Collection<FDate> nextValues = asList(
                cache.query().withFuture().getNextValues(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(nextValues).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValuesWithoutDistance() {
        final Collection<FDate> previousValues = asList(
                cache.query().getPreviousValues(entities.get(entities.size() - 1), entities.size()));
        Assertions.assertThat(previousValues).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextValuesWithoutDistance() {
        final Collection<FDate> nextValues = asList(
                cache.query().withFuture().getNextValues(entities.get(0), entities.size()));
        Assertions.assertThat(nextValues).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValuesGetsFilledDownWithDistance() {
        final Collection<FDate> previousValues = asList(
                cache.query().withFuture().getPreviousValues(FDate.MIN_DATE, entities.size()));
        Assertions.assertThat(previousValues.size()).isEqualTo(0);
        for (final FDate d : previousValues) {
            Assertions.assertThat(d).isSameAs(entities.get(0));
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNextValuesGetsFilledUpWithDistance() {
        final Collection<FDate> nextValues = asList(
                cache.query().withFuture().getNextValues(FDate.MAX_DATE, entities.size()));
        Assertions.assertThat(nextValues.size()).isEqualTo(0);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValuesGetsFilledDownWithoutDistance() {
        final Collection<FDate> previousValues = asList(
                cache.query().getPreviousValues(entities.get(0), entities.size()));
        Assertions.assertThat(previousValues.size()).isEqualTo(1);
        for (final FDate d : previousValues) {
            Assertions.assertThat(d).isSameAs(entities.get(0));
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNextValuesGetsFilledUpWithoutDistance() {
        final Collection<FDate> nextValues = asList(
                cache.query().withFuture().getNextValues(entities.get(entities.size() - 1), entities.size()));
        Assertions.assertThat(nextValues.size()).isEqualTo(1);
        for (final FDate d : nextValues) {
            Assertions.assertThat(d).isSameAs(entities.get(entities.size() - 1));
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNoData() {
        final List<FDate> liste = new ArrayList<FDate>(entities);
        entities.clear();
        for (final FDate entity : liste) {
            final FDate cachedEntity = cache.query().getValue(entity);
            Assertions.assertThat(cachedEntity).isNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(6);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(18);

        //new minKey limit gets tested
        final Collection<FDate> values = asList(cache.query().getPreviousValues(FDate.MIN_DATE, 5));
        Assertions.assertThat(values).isEmpty();
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(7);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(21);
    }

    @Test
    public void testNoDataInReadAllValuesAscendingFrom() {
        returnNullInReadNewestValueTo = true;
        for (final FDate entity : entities) {
            final FDate cachedEntity = cache.query().getValue(entity);
            Assertions.assertThat(cachedEntity).isNotNull();
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        //new minKey limit gets tested
        final Collection<FDate> values = asList(cache.query().withFuture().getPreviousValues(FDate.MIN_DATE, 5));
        for (final FDate d : values) {
            Assertions.assertThat(d).isEqualTo(entities.get(0));
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeyWithAllValues() {
        returnAllInReadAllValuesAscendingFrom = true;

        FDate previousKey = cache.query().getPreviousKey(new FDate(), entities.size());
        Assertions.assertThat(previousKey).isEqualTo(entities.get(0));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().getPreviousKey(new FDate(), 1);
        Assertions.assertThat(previousKey).isEqualTo(entities.get(entities.size() - 2));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNextKeyWithAllValues() {
        returnAllInReadAllValuesAscendingFrom = true;

        FDate nextKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(nextKey).isEqualTo(entities.get(entities.size() - 1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        nextKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, 1);
        Assertions.assertThat(nextKey).isEqualTo(entities.get(1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeyWithReturnMaxResults() {
        returnMaxResults = testReturnMaxResultsValue;

        FDate previousKey = cache.query().getPreviousKey(new FDate(), entities.size());
        Assertions.assertThat(previousKey).isEqualTo(entities.get(0));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);

        previousKey = cache.query().getPreviousKey(new FDate(), 1);
        Assertions.assertThat(previousKey).isEqualTo(entities.get(entities.size() - 2));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextKeyWithReturnMaxResults() {
        returnMaxResults = testReturnMaxResultsValue;

        FDate nextKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(nextKey).isEqualTo(entities.get(entities.size() - 1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        nextKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, 1);
        Assertions.assertThat(nextKey).isEqualTo(entities.get(1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousKeyWithAllValuesAndNullInReadNewestValueTo() {
        returnAllInReadAllValuesAscendingFrom = true;
        returnNullInReadNewestValueTo = true;

        FDate previousKey = cache.query().getPreviousKey(new FDate(), entities.size());
        Assertions.assertThat(previousKey).isEqualTo(entities.get(0));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);

        previousKey = cache.query().getPreviousKey(new FDate(), 1);
        Assertions.assertThat(previousKey).isEqualTo(entities.get(entities.size() - 2));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testNextKeyWithAllValuesAndNullInReadNewestValueTo() {
        returnAllInReadAllValuesAscendingFrom = true;
        returnNullInReadNewestValueTo = true;

        FDate nextKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, entities.size());
        Assertions.assertThat(nextKey).isEqualTo(entities.get(entities.size() - 1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        //loading newest entity is faster than always loading all entities
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);

        nextKey = cache.query().withFuture().getNextKey(FDate.MIN_DATE, 1);
        Assertions.assertThat(nextKey).isEqualTo(entities.get(1));
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
    }

    @Test
    public void testPreviousKeysFilterDuplicateKeys() {
        Assertions.assertThat(asList(cache.query().getPreviousKeys(new FDate(), 100)).size()).isSameAs(6);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(asList(cache.query().getPreviousKeys(new FDate(), 100)).size())
                .isEqualTo(entities.size());
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextKeysFilterDuplicateKeys() {
        Assertions.assertThat(asList(cache.query().withFuture().getNextKeys(FDate.MIN_DATE, 100)).size()).isSameAs(6);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(asList(cache.query().withFuture().getNextKeys(FDate.MIN_DATE, 100)).size())
                .isEqualTo(entities.size());
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValuesFilterDuplicateKeys() {
        Assertions.assertThat(asList(cache.query().getPreviousValues(new FDate(), 100)).size()).isSameAs(6);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(asList(cache.query().getPreviousValues(new FDate(), 100)).size())
                .isEqualTo(entities.size());
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
    }

    @Test
    public void testNextValuesFilterDuplicateKeys() {
        Assertions.assertThat(asList(cache.query().withFuture().getNextValues(FDate.MIN_DATE, 100)).size()).isSameAs(6);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(asList(cache.query().withFuture().getNextValues(FDate.MIN_DATE, 100)).size())
                .isEqualTo(entities.size());
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
    }

    @Test
    public void testPreviousValueKeyBetween() {
        for (int i = 0; i < entities.size(); i++) {
            final FDate entity = entities.get(i);
            final FDate foundKey = cache.query().getPreviousKeyWithSameValueBetween(FDate.MIN_DATE, FDate.MAX_DATE,
                    entity);
            Assertions.assertThat(foundKey).isEqualTo(entity);
        }
    }

    @Test
    public void testPreviousValueKeyBetweenReverse() {
        for (int i = entities.size() - 1; i >= 0; i--) {
            final FDate entity = entities.get(i);
            final FDate foundKey = cache.query().getPreviousKeyWithSameValueBetween(FDate.MIN_DATE, FDate.MAX_DATE,
                    entity);
            Assertions.assertThat(foundKey).isEqualTo(entity);
        }
    }

    @Test
    public void testNewEntityIncomingAfterClear() {
        final List<FDate> newEntities = new ArrayList<FDate>(entities);
        final FDate newEntity = FDateBuilder.newDate(1996, 1, 1);
        newEntities.add(newEntity);
        for (final FDate entity : newEntities) {
            final FDate value = cache.query().getValue(entity);
            if (newEntity.equals(entity)) {
                Assertions.assertThat(value).isNotEqualTo(newEntity);
                Assertions.assertThat(value).isEqualTo(entities.get(entities.size() - 1));
            } else {
                Assertions.assertThat(value).isEqualTo(entity);
            }
        }
        entities.add(newEntity);
        final FDate wrongValue = cache.query().getValue(newEntity);
        Assertions.assertThat(wrongValue).isEqualTo(entities.get(entities.size() - 2));
        HistoricalCacheRefreshManager.forceRefresh();
        final FDate correctValue = cache.query().getValue(newEntity);
        Assertions.assertThat(correctValue).isEqualTo(newEntity);
    }

    @Test
    public void testNewEntityIncomingPullingAdjustKeyProvider() {
        cache.setAdjustKeyProvider(new APullingHistoricalCacheAdjustKeyProvider(cache) {
            @Override
            protected FDate innerGetHighestAllowedKey() {
                return entities.get(entities.size() - 1);
            }
        });
        final List<FDate> newEntities = new ArrayList<FDate>(entities);
        final FDate newEntity = FDateBuilder.newDate(1996, 1, 1);
        newEntities.add(newEntity);
        for (final FDate entity : newEntities) {
            final FDate value = cache.query().getValue(entity);
            if (newEntity.equals(entity)) {
                Assertions.assertThat(value).isNotEqualTo(newEntity);
                Assertions.assertThat(value).isEqualTo(entities.get(entities.size() - 1));
            } else {
                Assertions.assertThat(value).isEqualTo(entity);
            }
        }
        entities.add(newEntity);
        final FDate correctValue = cache.query().getValue(newEntity);
        Assertions.assertThat(correctValue).isEqualTo(newEntity);
    }

    @Test
    public void testNewEntityIncomingPushingAdjustKeyProvider() {
        final APushingHistoricalCacheAdjustKeyProvider adjustKeyProvider = new APushingHistoricalCacheAdjustKeyProvider(
                cache) {
            @Override
            protected FDate getInitialHighestAllowedKey() {
                return null;
            }
        };
        cache.setAdjustKeyProvider(adjustKeyProvider);
        adjustKeyProvider.pushHighestAllowedKey(entities.get(entities.size() - 1));
        final List<FDate> newEntities = new ArrayList<FDate>(entities);
        final FDate newEntity = FDateBuilder.newDate(1996, 1, 1);
        newEntities.add(newEntity);
        for (final FDate entity : newEntities) {
            final FDate value = cache.query().getValue(entity);
            if (newEntity.equals(entity)) {
                Assertions.assertThat(value).isNotEqualTo(newEntity);
                Assertions.assertThat(value).isEqualTo(entities.get(entities.size() - 1));
            } else {
                Assertions.assertThat(value).isEqualTo(entity);
            }
        }
        adjustKeyProvider.pushHighestAllowedKey(newEntity);
        entities.add(newEntity);
        final FDate correctValue = cache.query().getValue(newEntity);
        Assertions.assertThat(correctValue).isEqualTo(newEntity);
    }

    @Test
    public void testNewEntityIncomingPushingAdjustKeyProviderWithoutInitialPush() {
        final APushingHistoricalCacheAdjustKeyProvider adjustKeyProvider = new APushingHistoricalCacheAdjustKeyProvider(
                cache) {
            @Override
            protected FDate getInitialHighestAllowedKey() {
                return null;
            }
        };
        cache.setAdjustKeyProvider(adjustKeyProvider);
        final List<FDate> newEntities = new ArrayList<FDate>(entities);
        final FDate newEntity = FDateBuilder.newDate(1996, 1, 1);
        newEntities.add(newEntity);
        for (final FDate entity : newEntities) {
            final FDate value = cache.query().getValue(entity);
            if (newEntity.equals(entity)) {
                Assertions.assertThat(value).isNotEqualTo(newEntity);
                Assertions.assertThat(value).isEqualTo(entities.get(entities.size() - 1));
            } else {
                Assertions.assertThat(value).isEqualTo(entity);
            }
        }
        adjustKeyProvider.pushHighestAllowedKey(newEntity);
        entities.add(newEntity);
        final FDate correctValue = cache.query().getValue(newEntity);
        Assertions.assertThat(correctValue).isEqualTo(newEntity);
    }

    @Test
    public void testNewEntityIncomingPushingAdjustKeyProviderWithoutPush() {
        final APushingHistoricalCacheAdjustKeyProvider adjustKeyProvider = new APushingHistoricalCacheAdjustKeyProvider(
                cache) {
            @Override
            protected FDate getInitialHighestAllowedKey() {
                return entities.get(entities.size() - 1);
            }
        };
        cache.setAdjustKeyProvider(adjustKeyProvider);
        final List<FDate> newEntities = new ArrayList<FDate>(entities);
        final FDate newEntity = FDateBuilder.newDate(1996, 1, 1);
        newEntities.add(newEntity);
        for (final FDate entity : newEntities) {
            final FDate value = cache.query().getValue(entity);
            if (newEntity.equals(entity)) {
                Assertions.assertThat(value).isNotEqualTo(newEntity);
                Assertions.assertThat(value).isEqualTo(entities.get(entities.size() - 1));
            } else {
                Assertions.assertThat(value).isEqualTo(entity);
            }
        }
        adjustKeyProvider.pushHighestAllowedKey(newEntity);
        entities.add(newEntity);
        final FDate correctValue = cache.query().getValue(newEntity);
        Assertions.assertThat(correctValue).isEqualTo(newEntity);
    }

    @Test
    public void testNotCorrectTime() {
        for (final FDate entity : entities.subList(1, entities.size() - 1)) {
            final FDate valueBefore = cache.query().getValue(entity.addHours(-3));
            Assertions.assertThat(valueBefore).isEqualTo(entity.addYears(-1));
            final FDate value = cache.query().getValue(entity);
            Assertions.assertThat(value).isEqualTo(entity);
            final FDate valueAfter = cache.query().getValue(entity.addHours(2));
            Assertions.assertThat(valueAfter).isEqualTo(entity);
        }
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithAlwaysSameKey() {
        for (int size = 1; size < entities.size(); size++) {
            final Collection<FDate> previousValues = asList(
                    cache.query().getPreviousValues(entities.get(entities.size() - 1), size));
            final List<FDate> expectedValues = entities.subList(entities.size() - size, entities.size());
            Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(4);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(6);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(8);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithIncrementingAlwaysOneValue() {
        for (int index = 0; index < entities.size(); index++) {
            final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(entities.get(index), 1));
            final List<FDate> expectedValues = entities.subList(index, index + 1);
            Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(2);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithIncrementingKey() {
        for (int index = 0; index < entities.size(); index++) {
            final Collection<FDate> previousValues = asList(
                    cache.query().getPreviousValues(entities.get(index), index + 1));
            final List<FDate> expectedValues = entities.subList(0, index + 1);
            Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithDecrementingKey() {
        for (int index = entities.size() - 1; index >= 0; index--) {
            final Collection<FDate> previousValues = asList(
                    cache.query().getPreviousValues(entities.get(index), index + 1));
            final List<FDate> expectedValues = entities.subList(0, index + 1);
            Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(10);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithDecrementingKeyAlwaysOne() {
        for (int index = entities.size() - 1; index >= 0; index--) {
            final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(entities.get(index), 1));
            final List<FDate> expectedValues = entities.subList(index, index + 1);
            Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(4);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(8);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithJumpingAround() {
        //first
        Collection<FDate> previousValues = asList(cache.query().getPreviousValues(entities.get(0), 1));
        List<FDate> expectedValues = entities.subList(0, 1);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(0);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last
        previousValues = asList(cache.query().getPreviousValues(entities.get(entities.size() - 1), 1));
        expectedValues = entities.subList(entities.size() - 1, entities.size());
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(0);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //first +1
        previousValues = asList(cache.query().getPreviousValues(entities.get(1), 1));
        expectedValues = entities.subList(1, 2);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(2);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last -1
        previousValues = asList(cache.query().getPreviousValues(entities.get(entities.size() - 2), 1));
        expectedValues = entities.subList(entities.size() - 2, entities.size() - 1);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(2);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValuesWithQueryCacheWithJumpingAroundTwoValues() {
        //first
        Collection<FDate> previousValues = asList(cache.query().getPreviousValues(entities.get(1), 2));
        List<FDate> expectedValues = entities.subList(0, 2);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last
        previousValues = asList(cache.query().getPreviousValues(entities.get(entities.size() - 1), 2));
        expectedValues = entities.subList(entities.size() - 2, entities.size());
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //first +1
        previousValues = asList(cache.query().getPreviousValues(entities.get(2), 2));
        expectedValues = entities.subList(1, 3);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last -1
        previousValues = asList(cache.query().getPreviousValues(entities.get(entities.size() - 2), 2));
        expectedValues = entities.subList(entities.size() - 3, entities.size() - 1);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithAlwaysSameKey() {
        for (int size = 1; size < entities.size(); size++) {
            final FDate previousValue = cache.query().getPreviousValue(entities.get(entities.size() - 1), size);
            final FDate expectedValue = entities.get(entities.size() - size - 1);
            Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(10);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithIncrementingKey() {
        for (int index = 0; index < entities.size(); index++) {
            final FDate previousValue = cache.query().getPreviousValue(entities.get(index), index + 1);
            final FDate expectedValue = entities.get(0);
            Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithIncrementingAlwaysOneValue() {
        for (int index = 1; index < entities.size(); index++) {
            final FDate previousValue = cache.query().getPreviousValue(entities.get(index), 1);
            final FDate expectedValue = entities.get(index - 1);
            Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithDecrementingKey() {
        for (int index = entities.size() - 1; index >= 0; index--) {
            final FDate previousValue = cache.query().getPreviousValue(entities.get(index), index + 1);
            final FDate expectedValue = entities.get(0);
            Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(10);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithDecrementingKeyAlwaysOne() {
        for (int index = entities.size() - 1; index > 0; index--) {
            final FDate previousValue = cache.query().getPreviousValue(entities.get(index), 1);
            final FDate expectedValue = entities.get(index - 1);
            Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        }
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(10);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithJumpingAround() {
        //first
        FDate previousValue = cache.query().getPreviousValue(entities.get(0), 1);
        FDate expectedValue = entities.get(0);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(0);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(2);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(0);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last
        previousValue = cache.query().getPreviousValue(entities.get(entities.size() - 1), 1);
        expectedValue = entities.get(entities.size() - 2);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(1);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(2);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //first +1
        previousValue = cache.query().getPreviousValue(entities.get(1), 1);
        expectedValue = entities.get(0);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(3);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(4);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(6);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last -1
        previousValue = cache.query().getPreviousValue(entities.get(entities.size() - 2), 1);
        expectedValue = entities.get(entities.size() - 3);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(3);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(4);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(6);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testPreviousValueWithQueryCacheWithJumpingAroundTwoValues() {
        //first
        FDate previousValue = cache.query().getPreviousValue(entities.get(1), 2);
        FDate expectedValue = entities.get(0);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last
        previousValue = cache.query().getPreviousValue(entities.get(entities.size() - 1), 2);
        expectedValue = entities.get(entities.size() - 3);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //first +1
        previousValue = cache.query().getPreviousValue(entities.get(2), 2);
        expectedValue = entities.get(0);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        //last -1
        previousValue = cache.query().getPreviousValue(entities.get(entities.size() - 2), 2);
        expectedValue = entities.get(entities.size() - 4);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(2);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(3);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(4);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testSubListWhenSwitchingFromNonFilterToFilter() {
        final FDate key = new FDate();
        final FDate previousValue = cache.query().getPreviousValue(key, 4);
        final FDate expectedValue = entities.get(entities.size() - 5);
        Assertions.assertThat(previousValue).isEqualTo(expectedValue);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(4);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(6);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(8);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(key, 4));
        final List<FDate> expectedValues = entities.subList(2, 6);
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(4);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(6);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(8);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testSubListWhenSwitchingFromFilterToNonFilter() {
        final FDate key = new FDate();
        final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(key, 10));
        final List<FDate> expectedValues = entities;
        Assertions.assertThat(previousValues).isEqualTo(expectedValues);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(10);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);

        final Collection<FDate> previousValuesCached = asList(cache.query().getPreviousValues(key, 10));
        Assertions.assertThat(previousValuesCached).isEqualTo(entities);
        Assertions.assertThat(countReadAllValuesAscendingFrom).isEqualTo(5);
        Assertions.assertThat(countReadNewestValueTo).isEqualTo(7);
        Assertions.assertThat(countInnerExtractKey).isEqualTo(10);
        Assertions.assertThat(countAdjustKey).isEqualTo(0);
    }

    @Test
    public void testRandomizedPreviousValues() {
        final List<Pair<Integer, Integer>> reproduce = new ArrayList<Pair<Integer, Integer>>();
        try {
            for (int i = 0; i < 100000; i++) {
                final int keyIndex = RandomUtils.nextInt(0, entities.size());
                final int shiftBackUnits = RandomUtils.nextInt(1, Math.max(1, keyIndex));
                reproduce.add(Pair.of(keyIndex, shiftBackUnits));
                final FDate key = entities.get(keyIndex);
                final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(key, shiftBackUnits));
                final List<FDate> expectedValues = entities.subList(keyIndex - shiftBackUnits + 1, keyIndex + 1);
                Assertions.assertThat(previousValues).isEqualTo(expectedValues);
                if (i % 100 == 0) {
                    cache.clear();
                    reproduce.clear();
                }
            }
        } catch (final Throwable t) {
            //CHECKSTYLE:OFF
            System.out.println(reproduce.size() + ". step: " + t.toString());
            //CHECKSTYLE:ON
            cache.clear();
            for (int step = 1; step <= reproduce.size(); step++) {
                final Pair<Integer, Integer> keyIndex_shiftBackUnits = reproduce.get(step - 1);
                final int keyIndex = keyIndex_shiftBackUnits.getFirst();
                final int shiftBackUnits = keyIndex_shiftBackUnits.getSecond();
                final FDate key = entities.get(keyIndex);
                final List<FDate> expectedValues = entities.subList(keyIndex - shiftBackUnits + 1, keyIndex + 1);
                if (step == reproduce.size()) {
                    //CHECKSTYLE:OFF
                    System.out.println("now");
                    //CHECKSTYLE:ON
                }
                final Collection<FDate> previousValues = asList(cache.query().getPreviousValues(key, shiftBackUnits));
                Assertions.assertThat(previousValues).isEqualTo(expectedValues);
            }
        }
    }

    private class TestGapHistoricalCache extends AGapHistoricalCache<FDate> {

        {
            enableTrailingQueryCore();
        }

        @Override
        protected FDate adjustKey(final FDate key) {
            countAdjustKey++;
            return super.adjustKey(key);
        }

        @Override
        public void setAdjustKeyProvider(final IHistoricalCacheAdjustKeyProvider adjustKeyProvider) {
            super.setAdjustKeyProvider(adjustKeyProvider);
        }

        @Override
        protected Iterable<FDate> readAllValuesAscendingFrom(final FDate key) {
            countReadAllValuesAscendingFrom++;
            if (returnMaxResults != null) {
                Assertions.assertThat(returnAllInReadAllValuesAscendingFrom).isFalse();
                Assertions.assertThat(returnNullInReadNewestValueTo).isFalse();
            }
            List<FDate> result;
            if (returnAllInReadAllValuesAscendingFrom && returnMaxResults == null) {
                result = new ArrayList<FDate>(entities);
            } else {
                final List<FDate> list = new ArrayList<FDate>();
                for (final FDate d : entities) {
                    if (!d.isBefore(key)) {
                        list.add(d);
                    }
                }
                result = list;
            }
            if (returnMaxResults != null && !result.isEmpty()) {
                result = result.subList(0, Math.min(result.size(), returnMaxResults));
            }
            return new BufferingIterator<FDate>(result.iterator());
        }

        @Override
        protected Integer getInitialMaximumSize() {
            return null;
        }

        @Override
        protected FDate innerExtractKey(final FDate key, final FDate entity) {
            countInnerExtractKey++;
            return entity;
        }

        @Override
        protected FDate readLatestValueFor(final FDate key) {
            countReadNewestValueTo++;
            if (returnNullInReadNewestValueTo) {
                return null;
            } else {
                FDate previousE = null;
                for (final FDate e : entities) {
                    if (previousE == null) {
                        previousE = e;
                    } else {
                        if (key.isAfter(e)) {
                            previousE = e;
                        } else {
                            break;
                        }
                    }
                }
                return previousE;
            }
        }

        @Override
        protected FDate innerCalculatePreviousKey(final FDate key) {
            return key.addDays(-1);
        }

        @Override
        protected FDate innerCalculateNextKey(final FDate key) {
            return key.addYears(1);
        }

    }
}
