package com.motoservice;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;

final class ConcurrentRunner {
  static <T> List<T> run(int requests, IntFunction<T> action) throws Exception {
    CountDownLatch ready = new CountDownLatch(requests), start = new CountDownLatch(1);
    ExecutorService workers = Executors.newFixedThreadPool(requests);
    try {
      List<Future<T>> tasks = new ArrayList<>();
      for (int i = 0; i < requests; i++) {
        final int index = i;
        tasks.add(
            workers.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(15, TimeUnit.SECONDS))
                    throw new TimeoutException("Start barrier");
                  return action.apply(index);
                }));
      }
      if (!ready.await(15, TimeUnit.SECONDS)) throw new TimeoutException("Ready barrier");
      start.countDown();
      List<T> result = new ArrayList<>();
      for (Future<T> task : tasks) result.add(task.get(90, TimeUnit.SECONDS));
      return result;
    } finally {
      start.countDown();
      workers.shutdownNow();
      workers.awaitTermination(10, TimeUnit.SECONDS);
    }
  }
}
