package com.spakai.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheIt<K, V> {

  private final ConcurrentMap<K, Future<V>> cache = new ConcurrentHashMap<>();

  private final Map<K, Instant> lastAccessTimeKeeper = new HashMap<>();

  private final ExecutorService pool;

  private final double percentageForCleanup;

  private final long maxSize;

  private AtomicBoolean cleanupInProgress;

  private long cacheStayAliveTimeInSecs;

  /**
  * Sets the size of Java Thread pool and sets default values for percentageForCleanup ,maxSize, cacheStayAliveTimeInSecs
  *
  * @param numberOfThreads Number of Threads in the Thread Pool.
  */
  public CacheIt(int numberOfThreads) {
    this.percentageForCleanup = 10;
    this.maxSize = 10000L;
    this.cacheStayAliveTimeInSecs = 60 * 60;
    pool = Executors.newFixedThreadPool(numberOfThreads);
    cleanupInProgress = new AtomicBoolean(false);
  }

  /**
  * Sets the size of Java Thread pool , values for percentageForCleanup,maxSize 
  * and cacheStayAliveTimeInSecs
  *
  * @param numberOfThreads Number of Threads in the Thread Pool.
  * @param percentageForCleanup Percentage of total cache that should be cleaned up
  * @param maxSize Maximum size of the cache
  * @param cacheStayAliveTimeInSecs Number of seconds an entry can stay in a cache
  */
  public CacheIt(int numberOfThreads, double percentageForCleanup, long maxSize, 
      long cacheStayAliveTimeInSecs) {
    this.percentageForCleanup = percentageForCleanup;
    this.maxSize = maxSize;
    this.cacheStayAliveTimeInSecs = cacheStayAliveTimeInSecs;
    pool = Executors.newFixedThreadPool(numberOfThreads);
    cleanupInProgress = new AtomicBoolean(false);
  }

  /**
  * Gets the value from cache if possible , otherwise runs the callable logic in a thread pool
  * and pushes the Future object into the cache. Another get() call with the same key will then get
  * the same Future object returned regardless whether the logic has completed. This ensures that the same
  * logic doesn't get computed twice. Whenever an item is pushed to the cache, removeLeastRecentlyUsedEntries()
  * is called to cleanup the cache.
  *
  * @param key      Unique key to retrieve or push to cache.
  * @param callable business logic calls.
  * @return Future<V> returns the Future object so that the client can call Future.get() to get the result.
  */
  public Future<V> get(K key, Callable<V> callable) {
    Future<V> future = cache.get(key);
    if (future == null) { 
      try {
        future = pool.submit(callable);
        cache.putIfAbsent(key, future);

        if (!cleanupInProgress.get()) {
          cleanupInProgress.set(true);
          removeLeastRecentlyUsedEntries();
          cleanupInProgress.set(false);
        }

      } catch (CancellationException e) {
          cache.remove(key);
      }


    }

    //overwrites same key so last access time is updated
    lastAccessTimeKeeper.put(key, Instant.now());

    return future;
  }

  /**
  * Removes X least used entries from the cache if it has exceeded cacheStayAliveTimeInSecs.
  * X 's value is calculated as percentage_for_cleanup * maxSize which is defined during creation of cache.
  * On its default setting , when the cache reaches more than 10k in size , 100 LRU entries will be removed
  * but only if it has expired i.e > cacheStayAliveTimeInSecs.
  */

  private void removeLeastRecentlyUsedEntries() {
    if (size() > maxSize) {
      lastAccessTimeKeeper.entrySet().stream()
      .sorted(Map.Entry.comparingByValue())
      .limit((long) ((percentageForCleanup / 100) * maxSize))
      .filter(entry -> hasExpired(entry.getValue()))
      .forEach(entry -> {
        cache.remove(entry.getKey());
        lastAccessTimeKeeper.remove(entry.getKey());
      });
    }
  }

  private boolean hasExpired(Instant instant) {
    return (Duration.between(instant, Instant.now()).toMillis() * 0.001 > cacheStayAliveTimeInSecs);
  }

  public int size() {
    return cache.size();
  }

}
