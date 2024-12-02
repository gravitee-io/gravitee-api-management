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
package io.gravitee.reporter.common.bulk;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.common.bulk.backpressure.BulkDropper;
import io.gravitee.reporter.common.bulk.backpressure.FlowableBackPressureMemoryAware;
import io.gravitee.reporter.common.bulk.compressor.BulkCompressor;
import io.gravitee.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.reporter.common.bulk.exception.NonRetryableException;
import io.gravitee.reporter.common.bulk.sender.BulkSender;
import io.gravitee.reporter.common.bulk.transformer.BulkTransformer;
import io.gravitee.reporter.common.bulk.transformer.TransformedReport;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class BulkProcessor extends AbstractService<BulkProcessor> {

  public static final double RETRY_FACTOR = 1.5;

  private final BulkSender bulkSender;
  private final BulkConfiguration bulkConfiguration;
  private final BulkTransformer bulkTransformer;
  private final UnicastProcessor<Reportable> processor =
    UnicastProcessor.create();
  private final BulkCompressor bulkCompressor;
  private final BulkDropper bulkDropper;
  private Disposable subscribe;

  @Override
  protected void doStart() throws Exception {
    super.doStart();
    bulkSender.start();
    subscribe =
      processor
        .observeOn(Schedulers.io())
        .flatMapMaybe(this::transform)
        .buffer(
          bulkConfiguration.flushInterval(),
          TimeUnit.SECONDS,
          bulkConfiguration.items()
        )
        .filter(reports -> !reports.isEmpty())
        .flatMapMaybe(this::compress)
        .compose(bulks ->
          new FlowableBackPressureMemoryAware(
            bulks,
            bulkConfiguration.maxMemorySize(),
            bulk -> bulkDropper.drop(bulk, BulkDropper.Reason.OVERFLOW)
          )
        )
        .flatMapCompletable(
          this::send,
          true,
          bulkConfiguration.maxConcurrentSend()
        )
        .retry()
        .subscribe();
  }

  private Maybe<@NonNull TransformedReport> transform(
    final Reportable reportable
  ) {
    return Maybe
      .fromCallable(() -> bulkTransformer.transform(reportable))
      .filter(transformedReport ->
        Objects.nonNull(transformedReport.transformed())
      )
      .doOnError(throwable ->
        log.warn("Unable to format incoming reportable", throwable)
      )
      .onErrorComplete();
  }

  private Maybe<CompressedBulk> compress(
    List<@NonNull TransformedReport> reports
  ) {
    return Maybe
      .fromCallable(() -> bulkCompressor.compress(reports))
      .doOnError(throwable ->
        log.warn("Unable to compress incoming reports", throwable)
      )
      .onErrorComplete();
  }

  private Completable send(CompressedBulk bulk) {
    final AtomicInteger attempts = new AtomicInteger(0);

    return Completable
      .defer(() -> bulkSender.send(bulk))
      .doOnSubscribe(s ->
        log.debug(
          "Sending bulk of reports [{}] (attempt {}).",
          bulk,
          attempts.get()
        )
      )
      .doOnComplete(() -> log.debug("Bulk of reports successfully sent."))
      .retryWhen(
        RxHelper.retryExponentialBackoff(
          bulkConfiguration.retryInitialDelay(),
          bulkConfiguration.retryMaxDelay(),
          TimeUnit.MILLISECONDS,
          RETRY_FACTOR,
          t -> {
            log.warn(
              "An error occurred when sending bulk of reports [{}]",
              bulk,
              t
            );
            if (
              t instanceof NonRetryableException ||
              attempts.incrementAndGet() > bulkConfiguration.maxRetries()
            ) {
              bulkDropper.drop(bulk, BulkDropper.Reason.ERROR);
              return false;
            }

            return true;
          }
        )
      )
      .onErrorComplete();
  }

  @Override
  protected void doStop() throws Exception {
    super.doStop();
    if (subscribe != null) {
      subscribe.dispose();
    }
    bulkSender.stop();
  }

  public void process(Reportable reportable) {
    processor.onNext(reportable);
  }
}
