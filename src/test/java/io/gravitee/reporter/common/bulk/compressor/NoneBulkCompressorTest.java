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
package io.gravitee.reporter.common.bulk.compressor;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.common.bulk.transformer.TransformedReport;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NoneBulkCompressorTest {

  private NoneBulkCompressor cut;

  @BeforeEach
  public void beforeEach() {
    cut = new NoneBulkCompressor();
  }

  @Test
  void should_not_compress_buffer() throws IOException {
    TransformedReport transformedReport = new TransformedReport(
      Buffer.buffer("clear"),
      Reportable.class
    );
    final CompressedBulk expectedBody = cut.compress(
      List.of(transformedReport)
    );
    assertThat(expectedBody.compressed())
      .isEqualTo(transformedReport.transformed());
  }

  @Test
  void should_compute_per_type() throws IOException {
    TransformedReport metricsType = new TransformedReport(
      Buffer.buffer("metrics"),
      Metrics.class
    );
    TransformedReport messageMetricsType = new TransformedReport(
      Buffer.buffer("message_metrics"),
      MessageMetrics.class
    );
    final CompressedBulk expectedBody = cut.compress(
      List.of(metricsType, messageMetricsType)
    );
    assertThat(expectedBody.countPerType()).hasSize(2);
    assertThat(expectedBody.countPerType())
      .extracting(metricsType.type())
      .isEqualTo(1);
    assertThat(expectedBody.countPerType())
      .extracting(messageMetricsType.type())
      .isEqualTo(1);
  }
}
