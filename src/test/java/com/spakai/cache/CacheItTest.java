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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CacheItTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  private CacheIt<Long, Long> cache ;
  
  private Lock lock; 
  
  private Condition wasExecuted;
  
  Long counter;
    
  @Before
  public void setup() {
    cache = new CacheIt<Long, Long>(10);
    lock = new ReentrantLock();
    wasExecuted  = lock.newCondition();
    counter = 0L;
  }
  
  @Test
  public void cacheIsEmpty() throws InterruptedException, ExecutionException {
    Future<Long> future = cache.get(1L, new Callable<Long>() {
      @Override
      public Long call() throws Exception {
    	  Thread.sleep(10000); 
    	  counter += 1L;
    	  return counter; 
      }
    });
    
    assertThat(future.get(), is(1L));
    
  }
  
  @Test
  public void cacheIsNotEmptyOnTheSecondCall() throws InterruptedException, ExecutionException {
    Future<Long> future = cache.get(1L, new Callable<Long>() {
      @Override
      public Long call() throws Exception {
      Thread.sleep(10000); 
      counter += 1L;
        return counter; 
      }
    });
    
    assertThat(future.get(), is(1L));
    assertThat(future.get(), is(1L));
    
  }
}