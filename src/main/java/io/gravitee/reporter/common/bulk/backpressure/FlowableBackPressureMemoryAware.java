/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.common.bulk.backpressure;

import io.gravitee.reporter.common.bulk.compressor.CompressedBulk;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Backpressure implementation allowing to buffer the bulk of reports in memory until it reaches the <code>maxMemorySize</code>.
 * Once the max memory is reached, it drops the oldest bulk of reports and calls the registered <code>onDropped</code> callback (if any) for each dropped bulk.
 *
 * It is inspired by {@link io.reactivex.rxjava3.internal.operators.flowable.FlowableOnBackpressureBufferStrategy}.
 */
public final class FlowableBackPressureMemoryAware
  extends Flowable<CompressedBulk> {

  final Flowable<CompressedBulk> source;
  final long maxMemorySize;
  final Consumer<? super CompressedBulk> onDropped;

  public FlowableBackPressureMemoryAware(
    Flowable<CompressedBulk> source,
    long maxMemorySize,
    Consumer<? super CompressedBulk> onDropped
  ) {
    this.source = source;
    this.maxMemorySize = maxMemorySize;
    this.onDropped = onDropped;
  }

  @Override
  protected void subscribeActual(
    @NonNull Subscriber<? super CompressedBulk> s
  ) {
    source.subscribe(
      new BackpressureBufferMemoryAwareSubscriber(s, maxMemorySize, onDropped)
    );
  }

  static class BackpressureBufferMemoryAwareSubscriber
    extends AtomicInteger
    implements FlowableSubscriber<CompressedBulk>, Subscription {

    @Serial
    private static final long serialVersionUID = -7651817331081893000L;

    final Subscriber<? super CompressedBulk> downstream;
    final long maxMemorySize;
    final Consumer<? super CompressedBulk> onDropped;
    final AtomicLong requested;
    final Deque<CompressedBulk> deque;
    Subscription upstream;

    volatile boolean cancelled;
    volatile boolean done;
    Throwable error;
    final AtomicLong totalMemoryUsed = new AtomicLong(0);

    BackpressureBufferMemoryAwareSubscriber(
      Subscriber<? super CompressedBulk> actual,
      long maxMemorySize,
      Consumer<? super CompressedBulk> onDropped
    ) {
      this.downstream = actual;
      this.maxMemorySize = maxMemorySize;
      this.requested = new AtomicLong();
      this.deque = new ArrayDeque<>();
      this.onDropped = onDropped;
    }

    @Override
    public void onSubscribe(@NonNull Subscription s) {
      if (SubscriptionHelper.validate(this.upstream, s)) {
        this.upstream = s;

        downstream.onSubscribe(this);

        s.request(Long.MAX_VALUE);
      }
    }

    @Override
    public void onNext(CompressedBulk c) {
      if (done) {
        return;
      }
      boolean callDrain = false;
      List<CompressedBulk> toDropList = null;

      synchronized (deque) {
        if (totalMemoryUsed.get() <= maxMemorySize) {
          deque.offer(c);
          increaseMemoryUsed(c);
          callDrain = true;
        } else {
          CompressedBulk dropCandidate;
          long totalFreeSpace = 0;
          do {
            dropCandidate = deque.poll();
            if (dropCandidate != null) {
              final int freeSpace = dropCandidate.compressed().length();
              decreaseMemoryUsed(freeSpace);
              totalFreeSpace += freeSpace;
              if (toDropList == null) {
                toDropList = new ArrayList<>();
              }
              toDropList.add(dropCandidate);
            }
          } while (
            dropCandidate != null && totalFreeSpace < c.compressed().length()
          );

          deque.offer(c);
          increaseMemoryUsed(c);
        }
      }

      if (onDropped != null && toDropList != null) {
        try {
          for (CompressedBulk drop : toDropList) {
            onDropped.accept(drop);
          }
        } catch (Throwable ex) {
          Exceptions.throwIfFatal(ex);
          upstream.cancel();
          onError(ex);
        }
      }

      if (callDrain) {
        drain();
      }
    }

    private void decreaseMemoryUsed(int freeSpace) {
      totalMemoryUsed.updateAndGet(currentValue -> currentValue - freeSpace);
    }

    private void increaseMemoryUsed(CompressedBulk c) {
      totalMemoryUsed.updateAndGet(currentValue ->
        currentValue + c.compressed().length()
      );
    }

    @Override
    public void onError(Throwable t) {
      if (done) {
        RxJavaPlugins.onError(t);
        return;
      }
      error = t;
      done = true;
      drain();
    }

    @Override
    public void onComplete() {
      done = true;
      drain();
    }

    @Override
    public void request(long n) {
      if (SubscriptionHelper.validate(n)) {
        BackpressureHelper.add(requested, n);
        drain();
      }
    }

    @Override
    public void cancel() {
      cancelled = true;
      upstream.cancel();

      if (getAndIncrement() == 0) {
        clear();
      }
    }

    void clear() {
      synchronized (deque) {
        deque.clear();
      }
    }

    void drain() {
      if (getAndIncrement() != 0) {
        return;
      }

      int missed = 1;
      Subscriber<? super CompressedBulk> a = downstream;
      do {
        long r = requested.get();
        long e = 0L;
        while (e != r) {
          if (cancelled) {
            clear();
            return;
          }

          boolean d = done;

          CompressedBulk b;

          synchronized (deque) {
            b = deque.poll();
          }

          boolean empty = b == null;

          if (d) {
            Throwable ex = error;
            if (ex != null) {
              clear();
              a.onError(ex);
              return;
            }
            if (empty) {
              a.onComplete();
              return;
            }
          }

          if (empty) {
            break;
          }

          decreaseMemoryUsed(b.compressed().length());
          a.onNext(b);

          e++;
        }

        if (e == r) {
          if (cancelled) {
            clear();
            return;
          }

          boolean d = done;

          boolean empty;

          synchronized (deque) {
            empty = deque.isEmpty();
          }

          if (d) {
            Throwable ex = error;
            if (ex != null) {
              clear();
              a.onError(ex);
              return;
            }
            if (empty) {
              a.onComplete();
              return;
            }
          }
        }

        if (e != 0L) {
          BackpressureHelper.produced(requested, e);
        }

        missed = addAndGet(-missed);
      } while (missed != 0);
    }
  }
}
