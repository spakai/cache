package com.spakai.cache;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CacheItTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private CacheIt<Long, Long> cache ;

  private Long counter;

  @Before
  public void setup() {
    counter = 0L;
  }

  private Future<Long> LongRunningTasksThatIncrementsCountersIfNotInCache(Long key) {
    return cache.get(key, () -> {
      Thread.sleep(5000);
      counter += 1L;
      return counter;
    });
  }

  private Future<Long> IncrementsCountersIfNotInCache(Long key) {
    return cache.get(key, () -> {
      counter += 1L;
      return counter;
    });
  }

  private Future<Long>  DoesNotIncrementCounterReturnsKeyAsValue(Long key) {
    return cache.get(key, () -> key);
  }

  @Test
  public void cacheIsEmpty() throws InterruptedException, ExecutionException {
    cache = new CacheIt<>(10);

    Future<Long> future = IncrementsCountersIfNotInCache(1L);

    assertThat(future.get(), is(1L));

  }

  @Test
  public void cacheIsNotEmptyOnTheSecondCall() throws InterruptedException, ExecutionException {
    cache = new CacheIt<>(10);

    Future<Long> future = IncrementsCountersIfNotInCache(1L);

    assertThat(future.get(), is(1L));
    assertThat(future.get(), is(1L));

  }

  @Test
  public void cacheDoesNotExecuteDuplicateKey() throws InterruptedException, ExecutionException  {
    cache = new CacheIt<>(10);

    Future<Long> future1 = LongRunningTasksThatIncrementsCountersIfNotInCache(1L);
    Future<Long> future2 = LongRunningTasksThatIncrementsCountersIfNotInCache(1L);

    assertThat(future1.get(), is(1L));
    assertThat(future2.get(), is(1L));

  }

  @Test
  public void cacheIsClearedOnceItHitsLimit() throws InterruptedException, ExecutionException {

    //create a cache that will clear all (100%) once the size hits above 3
    cache = new CacheIt<>(10, 100, 3, 1);

    IncrementsCountersIfNotInCache(1L);

    IncrementsCountersIfNotInCache(2L);

    IncrementsCountersIfNotInCache(3L);

    assertThat(cache.size(), is(3L));

    //Make all keys expire
    Thread.sleep(2000);

    //This call clears the cache
    DoesNotIncrementCounterReturnsKeyAsValue(100L);

    assertThat(cache.size(), is(1L));

    Future<Long> future = IncrementsCountersIfNotInCache(3L);

    assertThat(future.get(), is(4L));
  }

  @Test
  public void cacheIsNotClearedOnceItHitsSizeLimitBecauseOfStayAliveTimings() throws InterruptedException, ExecutionException {

    //create a cache that will not clear for an hour
    cache = new CacheIt<>(10, 100, 3, 3600);

    IncrementsCountersIfNotInCache(1L);

    IncrementsCountersIfNotInCache(2L);

    IncrementsCountersIfNotInCache(3L);

    assertThat(cache.size(), is(3L));

    //This call will try to clear the cache but it won't because of StayAliveTimings
    DoesNotIncrementCounterReturnsKeyAsValue(100L);

    assertThat(cache.size(), is(4L));

    Future<Long> future = IncrementsCountersIfNotInCache(3L);

    assertThat(future.get(), is(3L));
  }

  @Test
  public void cacheClearsLeastRecentlyUsed() throws InterruptedException, ExecutionException {

    cache = new CacheIt<>(10, 98, 100, 5);

    for(long key=1L; key<101L;key++) {
      IncrementsCountersIfNotInCache(key);
    }

    assertThat(cache.size(), is(100L));

    //get key 55L, make it recently used
    Thread.sleep(1000);
    Future<Long> future;
    future = IncrementsCountersIfNotInCache(55L);
    assertThat(future.get(), is(55L));

    Thread.sleep(5000);

    //trigger cleanup
    DoesNotIncrementCounterReturnsKeyAsValue(200L);

    //(100-98%) 2% of 100 is 2 , plus entry with key=200L is 3
    assertThat(cache.size(), is(2L + 1L));

    future = IncrementsCountersIfNotInCache(55L);

    assertThat(future.get(), is(55L));

  }

  @Test
  public void multipleClientsStress() {

  }
}
