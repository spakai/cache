package com.spakai.cache;

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

public class CacheIt<K, V> {
  
  private final ConcurrentMap<K, Future<V>> cache = new ConcurrentHashMap<>();
  
  private final Map<K,Instant> lastAccessTimeKeeper = new HashMap<>();
  
  private final ExecutorService pool; 
  
  private final double percentageForCleanup;
  
  private final long maxSize;
  
  private volatile boolean cleanupInProgress = false;
  
  
  /**
   * Sets the size of Java Thread pool and sets default values for percentageForCleanup and maxSize.
   * 
   * @param numberOfThreads Number of Threads in the Thread Pool.
   * 
  */
  public CacheIt(int numberOfThreads) {
    this.percentageForCleanup = 0.01;
    this.maxSize = 10000L;
    pool = Executors.newFixedThreadPool(numberOfThreads);
  }
  
  /**
   * Sets the size of Java Thread pool , values for percentageForCleanup and maxSize.
   * 
   * @param numberOfThreads Number of Threads in the Thread Pool.
   * 
  */
  public CacheIt(int numberOfThreads, double percentage_for_cleanup, long maxSize) {
    this.percentageForCleanup = percentage_for_cleanup;
    this.maxSize = maxSize;
    pool = Executors.newFixedThreadPool(numberOfThreads);
  }
  
  /**
   * Gets the value from cache if possible , otherwise runs the callable logic in a thread pool
   * and pushes the Future object into the cache. Another get() call with the same key will then get
   * the same Future object returned regardless whether the logic has completed. This ensures that the same 
   * logic doesn't get computed twice. Whenever an item is pushed to the cache, removeLeastRecentlyUsedEntries()
   * is called to cleanup the cache.
   * 
   * @param key Unique key to retrieve or push to cache.
   * @param callable business logic calls.
   * @return Future<V> returns the Future object so that the client can call Future.get() to get the result.
  */
  public Future<V> get(K key, Callable<V> callable) {
    Future<V> future = cache.get(key);
    if (future == null) {
    	try {
	      future = pool.submit(callable);
	      cache.putIfAbsent(key, future);
	      
	      if(!cleanupInProgress) {
	    	  cleanupInProgress=true;
	    	  removeLeastRecentlyUsedEntries();
	    	  cleanupInProgress=false;
	      }
	      
	    } catch(CancellationException e) {
	    	cache.remove(key);
	    } 
	}
    
    //overwrites same key so last access time is updated
    lastAccessTimeKeeper.put(key, Instant.now());
    
    return future;
  }
  
  /**
   * Removes X least used entries from the cache. 
   * X 's value is calculated as percentage_for_cleanup * maxSize which is defined during creation of cache
   * On its default setting , when the cache reaches more than 10k in size , 100 LRU entries will be removed
  */
  
  private void removeLeastRecentlyUsedEntries() {
	  if(cache.size() > maxSize) {
		  lastAccessTimeKeeper.entrySet().stream()
		    .sorted(Map.Entry.comparingByValue())
		    .limit((long) (percentageForCleanup * maxSize))
		    .forEach(entry ->  cache.remove(entry.getKey()));
	  }
  }
}

