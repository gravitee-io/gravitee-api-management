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
package io.gravitee.apim.reporter.elasticsearch.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.reporter.common.bulk.BulkConfiguration;
import io.gravitee.apim.reporter.common.bulk.BulkProcessor;
import io.gravitee.apim.reporter.common.bulk.backpressure.BulkDropper;
import io.gravitee.apim.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.apim.reporter.common.bulk.compressor.NoneBulkCompressor;
import io.gravitee.apim.reporter.common.bulk.sender.BulkSender;
import io.gravitee.apim.reporter.common.bulk.transformer.TransformedReport;
import io.gravitee.apim.reporter.elasticsearch.IntegrationTestConfiguration;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.buffer.Buffer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(IntegrationTestConfiguration.class)
class ElasticBulkFailureIntegrationTest {

    @Autowired
    private Client client;

    private final String openIndex = "bulk-failure-open-" + UUID.randomUUID();
    private final String blockedIndex = "bulk-failure-blocked-" + UUID.randomUUID();
    private final BulkDropper bulkDropper = spy(new BulkDropper());
    private BulkSender bulkSender;
    private BulkProcessor bulkProcessor;

    @BeforeEach
    void setUp() throws Exception {
        client.createIndexWithAlias(blockedIndex, "{ \"settings\": { \"index.blocks.write\": true } }").blockingAwait();

        bulkSender = spy(new ElasticBulkSender(client));
        bulkProcessor = new BulkProcessor(
            bulkSender,
            new BulkConfiguration(2, 1L, 1, 2, 10, 30, 1_048_576L),
            reportable -> {
                final String index = ((Metrics) reportable).getRequestId();
                return new TransformedReport(
                    Buffer.buffer("{ \"index\": { \"_index\": \"" + index + "\" } }\n{ \"field\": \"value\" }\n"),
                    Metrics.class
                );
            },
            new NoneBulkCompressor(),
            bulkDropper
        );
        bulkProcessor.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        bulkProcessor.stop();
    }

    @Test
    void should_drop_and_count_the_report_elasticsearch_rejects_and_write_the_others_once() {
        bulkProcessor.process(Metrics.builder().requestId(openIndex).build());
        bulkProcessor.process(Metrics.builder().requestId(blockedIndex).build());

        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() ->
                verify(bulkDropper).drop(
                    argThat((List<TransformedReport> reports) -> reports.size() == 1),
                    argThat((Collection<String> reasons) ->
                        reasons.stream().anyMatch(reason -> reason.startsWith("cluster_block_exception"))
                    )
                )
            );
        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() ->
                assertThat(client.search(openIndex, null, "{}").blockingGet().getSearchHits().getTotal().getValue()).isOne()
            );
        verify(bulkSender, times(1)).send(any(CompressedBulk.class));
        verify(bulkDropper, times(0)).drop(any(CompressedBulk.class), any());
    }
}
