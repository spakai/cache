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
  
  Long counter;
    
  @Before
  public void setup() {
    counter = 0L;
  }

  public Future<Long> IncrementsCountersIfNotInCache(Long key) {
    Future<Long> future = cache.get(key, new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        counter += 1L;
        return counter;
      }
    });

    return future;
  }

  public Future<Long> DoesnotIncrementCounterReturnsKeyAsValue(Long key) {
    Future<Long> future = cache.get(key, new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        return key;
      }
    });

    return future;
  }



  @Test
  public void cacheIsEmpty() throws InterruptedException, ExecutionException {
    cache = new CacheIt<Long, Long>(10);

    Future<Long> future = IncrementsCountersIfNotInCache(1L);
    
    assertThat(future.get(), is(1L));
    
  }
  
  @Test
  public void cacheIsNotEmptyOnTheSecondCall() throws InterruptedException, ExecutionException {
    cache = new CacheIt<Long, Long>(10);

    Future<Long> future = IncrementsCountersIfNotInCache(1L);

    assertThat(future.get(), is(1L));
    assertThat(future.get(), is(1L));
    
  }

  @Test
  public void cacheIsClearedOnceItHitsLimit() throws InterruptedException, ExecutionException {

    //create a cache that will clear all (100%) once the size hits above 3
    cache = new CacheIt<Long, Long>(10,100,3,1);

    IncrementsCountersIfNotInCache(1L);

    IncrementsCountersIfNotInCache(2L);

    IncrementsCountersIfNotInCache(3L);

    assertThat(cache.size(), is(3));

    //Make all keys expire
    Thread.sleep(2000);

    //This call clears the cache
    DoesnotIncrementCounterReturnsKeyAsValue(100L);

    assertThat(cache.size(), is(1));

    Future<Long> future = IncrementsCountersIfNotInCache(3L);

    assertThat(future.get(), is(4L));
  }

  @Test
  public void cacheIsNotClearedOnceItHitsSizeLimitBecauseOfStayAliveTimings() throws InterruptedException, ExecutionException {

    //create a cache that will not clear for an hour
    cache = new CacheIt<Long, Long>(10,100,3,3600);

    IncrementsCountersIfNotInCache(1L);

    IncrementsCountersIfNotInCache(2L);

    IncrementsCountersIfNotInCache(3L);

    assertThat(cache.size(), is(3));

    //This call will try to clear the cache
    DoesnotIncrementCounterReturnsKeyAsValue(100L);

    assertThat(cache.size(), is(4));

    Future<Long> future = IncrementsCountersIfNotInCache(3L);

    assertThat(future.get(), is(3L));
  }
}