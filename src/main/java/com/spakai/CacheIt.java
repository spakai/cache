package com.spakai.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CacheIt<K, V> {
  
  private final ConcurrentMap<K, Future<V>> cache = new ConcurrentHashMap<>();
  
  private final ExecutorService pool; 
  
  public CacheIt(int numberOfThreads) {
    pool = Executors.newFixedThreadPool(numberOfThreads);
  }
  
  /**
   * Gets the value from cache if possible , otherwise runs the callable logic in a threadpool 
   * and pushes the Future object into the cache. Another get() call with the same key will then get
   * the same Future object returned regardless whether the logic has completed. This ensures that the same 
   * logic doesn't get computed twice.
   * 
   * @param key Unique key to retrieve or push to cache.
   * @param callable business logic calls.
   * @return Future<V> returns the Future object so that the client can call Future.get() to get the result.
  */
  public Future<V> get(K key, Callable<V> callable) {
    Future<V> future = cache.get(key);
    if (future == null) {
      future = pool.submit(callable);
      cache.putIfAbsent(key, future);
    }
    
    return future;
  }
}


