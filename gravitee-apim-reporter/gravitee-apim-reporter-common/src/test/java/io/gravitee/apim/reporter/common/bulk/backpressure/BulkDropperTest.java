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
package io.gravitee.apim.reporter.common.bulk.backpressure;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.apim.reporter.common.bulk.transformer.TransformedReport;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.vertx.core.buffer.Buffer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BulkDropperTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(BulkDropper.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final BulkDropper cut = new BulkDropper();

    @BeforeEach
    void setUp() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    @Test
    void should_count_the_rejected_reports_per_type_and_add_them_to_the_dropped_ones() {
        // Given
        cut.drop(new CompressedBulk(Buffer.buffer(), Map.of("metrics", 2)), BulkDropper.Reason.ERROR);

        // When
        cut.drop(List.of(report(Metrics.class), report(Metrics.class), report(Log.class)), List.of("cluster_block_exception"));

        // Then
        assertThat(lastWarning())
            .startsWith("Reports rejected by the target. Dropping reports [")
            .contains("metrics=2", "log=1")
            .contains("Reasons: [cluster_block_exception]")
            .contains("Total: [")
            .satisfies(message -> assertThat(message.substring(message.indexOf("Total: ["))).contains("metrics=4", "log=1"));
    }

    private String lastWarning() {
        return logs.list
            .stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .reduce((first, second) -> second)
            .orElseThrow()
            .getFormattedMessage();
    }

    private static TransformedReport report(Class<? extends Reportable> type) {
        return new TransformedReport(Buffer.buffer("{}"), type);
    }
}
