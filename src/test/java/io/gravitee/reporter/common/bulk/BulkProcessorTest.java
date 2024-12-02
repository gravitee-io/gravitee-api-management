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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.common.bulk.backpressure.BulkDropper;
import io.gravitee.reporter.common.bulk.compressor.BulkCompressor;
import io.gravitee.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.reporter.common.bulk.compressor.NoneBulkCompressor;
import io.gravitee.reporter.common.bulk.exception.NonRetryableException;
import io.gravitee.reporter.common.bulk.exception.SendReportException;
import io.gravitee.reporter.common.bulk.sender.BulkSender;
import io.gravitee.reporter.common.bulk.transformer.BulkFormatterTransformer;
import io.gravitee.reporter.common.bulk.transformer.BulkTransformer;
import io.gravitee.reporter.common.bulk.transformer.TransformedReport;
import io.gravitee.reporter.common.formatter.FormatterFactory;
import io.gravitee.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.reporter.common.formatter.Type;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith({ MockitoExtension.class })
public class BulkProcessorTest {

  private BulkProcessor cut;

  @Mock
  private io.gravitee.node.api.Node node;

  @Mock
  private BulkSender bulkSender;

  private BulkTransformer bulkTransformer;
  private BulkCompressor bulkCompressor;
  private BulkDropper bulkDropper;

  @BeforeEach
  public void beforeEach() {
    lenient().when(node.id()).thenReturn("nodeId");

    this.bulkTransformer =
      new BulkFormatterTransformer(
        new FormatterFactory(
          node,
          FormatterFactoryConfiguration
            .builder()
            .elasticSearchVersion(8)
            .build()
        )
          .getFormatter(Type.ELASTICSEARCH)
      );
    this.bulkDropper = new BulkDropper();
    this.bulkCompressor = new NoneBulkCompressor();
  }

  private void initBulkProcessor() throws Exception {
    initBulkProcessor(1);
  }

  private void initBulkProcessor(final int items) throws Exception {
    initBulkProcessor(items, 26214400L);
  }

  private void initBulkProcessor(final int items, final long memorySize)
    throws Exception {
    final BulkConfiguration bulkConfiguration = new BulkConfiguration(
      items,
      5L,
      1,
      5,
      10,
      30,
      memorySize
    );
    when(bulkSender.send(any())).thenReturn(Completable.complete());
    cut =
      new BulkProcessor(
        bulkSender,
        bulkConfiguration,
        bulkTransformer,
        bulkCompressor,
        bulkDropper
      );
    cut.start();
  }

  @AfterEach
  public void afterEach() throws Exception {
    if (cut != null) {
      cut.stop();
    }
  }

  @Test
  void should_process_a_reportable_and_send_it() throws Exception {
    initBulkProcessor();
    final Metrics reportable = buildMetrics();
    final CompressedBulk expectedBody = bulkCompressor.compress(
      List.of(bulkTransformer.transform(reportable))
    );

    cut.process(reportable);
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() ->
        verify(bulkSender)
          .send(argThat(argument -> argument.equals(expectedBody)))
      );
  }

  @Test
  void should_buffer_reports_and_send_single_content() throws Exception {
    int items = 10;
    initBulkProcessor(items);

    final List<TransformedReport> transformedReports = new ArrayList<>();
    for (int i = 0; i < items; i++) {
      final Metrics reportable = buildMetrics();
      cut.process(reportable);
      transformedReports.add(bulkTransformer.transform(reportable));
    }
    final CompressedBulk expectedBody = bulkCompressor.compress(
      transformedReports
    );

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() ->
        verify(bulkSender)
          .send(
            argThat(compressedBulk -> {
              assertThat(compressedBulk.countPerType())
                .contains(entry("metrics", 10));
              assertThat(compressedBulk.compressed())
                .isEqualTo(expectedBody.compressed());
              return true;
            })
          )
      );
  }

  @Test
  void should_ignore_invalid_reportable() throws Exception {
    initBulkProcessor();
    cut.process(Metrics.builder().build()); // << invalid reportable with missing data
    cut.process(buildMetrics());
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender).send(any()));
  }

  @Test
  void should_ignore_and_continue_reporting_when_send_reports_throw_non_retryable_exceptions()
    throws Exception {
    initBulkProcessor();
    when(bulkSender.send(any()))
      .thenReturn(
        Completable.error(
          new NonRetryableException("1", new RuntimeException())
        ),
        Completable.error(
          new NonRetryableException("2", new RuntimeException())
        ),
        Completable.complete()
      );
    cut.process(buildMetrics()); // expect NonRetryableException
    cut.process(buildMetrics()); // expect NonRetryableException
    cut.process(buildMetrics()); // expect ok
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(3)).send(any()));
  }

  @Test
  void should_retry_reporting_when_send_reports_throw_retryable_exceptions()
    throws Exception {
    initBulkProcessor();
    when(bulkSender.send(any()))
      .thenReturn(
        Completable.error(new SendReportException("1", new RuntimeException())),
        Completable.error(new SendReportException("2", new RuntimeException())),
        Completable.complete()
      );
    cut.process(buildMetrics()); // expect SendReportException
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(3)).send(any()));
  }

  @Test
  void should_retry_reporting_when_any_exception_occurred_on_sending_reports()
    throws Exception {
    initBulkProcessor();
    when(bulkSender.send(any()))
      .thenThrow(new RuntimeException())
      .thenReturn(Completable.complete());
    cut.process(buildMetrics()); // expect SendReportException
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(2)).send(any()));
  }

  @Test
  void should_drop_report_when_max_retries_is_reached() throws Exception {
    initBulkProcessor(1, 30);
    when(bulkSender.send(any())).thenThrow(new RuntimeException());
    cut.process(buildMetrics()); // expect SendReportException
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(6)).send(any()));
  }

  @Test
  void should_drop_report_when_max_memory_size_is_reached() throws Exception {
    initBulkProcessor(1, 0);
    when(bulkSender.send(any()))
      .thenReturn(
        Maybe.just(1).delay(1000, TimeUnit.MILLISECONDS).ignoreElement()
      );
    for (int i = 0; i < 100; i++) {
      cut.process(buildMetrics());
    }
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(1)).send(any()));
  }

  @Test
  void should_drain_pending_reports_when_stopping() throws Exception {
    initBulkProcessor(1, 0);
    when(bulkSender.send(any()))
      .thenReturn(
        Maybe.just(1).delay(5000, TimeUnit.MILLISECONDS).ignoreElement()
      );
    for (int i = 0; i < 100; i++) {
      cut.process(buildMetrics());
    }
    await()
      .atMost(2, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(1)).send(any()));

    // Cancel while the first call is still in progress (delay of 5s).
    cut.stop();

    // At the end, only 1 call has been made.
    verify(bulkSender, times(1)).send(any());
  }

  @Test
  void should_continue_reporting_when_transform_throws_exception()
    throws Exception {
    this.bulkTransformer = mock(BulkTransformer.class);
    when(bulkTransformer.transform(any()))
      .thenThrow(new RuntimeException())
      .thenReturn(new TransformedReport(Buffer.buffer("test"), Metrics.class));

    initBulkProcessor();
    cut.process(buildMetrics()); // expect exception
    cut.process(buildMetrics()); // expect ok

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(1)).send(any()));
  }

  @Test
  void should_continue_reporting_when_compress_throws_exception()
    throws Exception {
    this.bulkCompressor = mock(BulkCompressor.class);

    when(bulkCompressor.compress(ArgumentMatchers.any()))
      .thenThrow(new IOException("Exception during compression"))
      .thenReturn(
        new CompressedBulk(
          io.vertx.rxjava3.core.buffer.Buffer.buffer("data"),
          Collections.emptyMap()
        )
      );

    initBulkProcessor();
    cut.process(buildMetrics()); // expect exception
    cut.process(buildMetrics()); // expect ok

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bulkSender, times(1)).send(any()));
  }

  private static Metrics buildMetrics() {
    return Metrics
      .builder()
      .requestId("requestId")
      .transactionId("transactionId")
      .httpMethod(HttpMethod.GET)
      .uri("/uri")
      .api("api")
      .apiName("apiName")
      .apiResponseTimeMs(1)
      .application("application")
      .clientIdentifier("clientIdentifier")
      .endpoint("endpoint")
      .host("host")
      .path("/path")
      .localAddress("localAddress")
      .remoteAddress("remoteAddress")
      .build();
  }
}
