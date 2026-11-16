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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.apim.reporter.common.bulk.exception.BulkItemsFailureException;
import io.gravitee.apim.reporter.common.bulk.exception.BulkItemsFailureException.ItemFailure;
import io.gravitee.apim.reporter.common.bulk.exception.SendReportException;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.bulk.BulkItemResponse;
import io.gravitee.elasticsearch.model.bulk.BulkResponse;
import io.gravitee.elasticsearch.model.bulk.Failure;
import io.gravitee.elasticsearch.model.bulk.Index;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticBulkSenderTest {

    private static final CompressedBulk BULK = new CompressedBulk(Buffer.buffer("bulk"), Map.of("metrics", 4));

    @Mock
    private Client client;

    private ElasticBulkSender cut;

    @BeforeEach
    void setUp() {
        cut = new ElasticBulkSender(client);
    }

    @Test
    void should_complete_when_every_report_is_written() {
        when(client.bulk(any(Buffer.class))).thenReturn(Single.just(response(false, item(201, null), item(200, null))));

        cut.send(BULK).test().assertComplete();
    }

    @Test
    void should_fail_with_a_retryable_error_when_elasticsearch_cannot_be_reached() {
        when(client.bulk(any(Buffer.class))).thenReturn(Single.error(new IOException("Connection refused")));

        cut
            .send(BULK)
            .test()
            .assertError(error -> error instanceof SendReportException && error.getMessage().contains("Connection refused"));
    }

    @Test
    void should_report_each_report_elasticsearch_did_not_write_and_whether_to_send_it_again() {
        when(client.bulk(any(Buffer.class))).thenReturn(
            Single.just(
                response(
                    true,
                    item(201, null),
                    item(400, failure("document_parsing_exception", "failed to parse field [status]")),
                    item(429, failure("es_rejected_execution_exception", "rejected execution")),
                    item(503, failure("unavailable_shards_exception", "primary shard is not active"))
                )
            )
        );

        cut
            .send(BULK)
            .test()
            .assertError(error -> {
                assertThat(error).isInstanceOf(BulkItemsFailureException.class);
                assertThat(((BulkItemsFailureException) error).failures()).containsExactly(
                    new ItemFailure(1, false, "document_parsing_exception"),
                    new ItemFailure(2, true, "es_rejected_execution_exception"),
                    new ItemFailure(3, true, "unavailable_shards_exception")
                );
                return true;
            });
    }

    @Test
    void should_report_the_status_of_a_failed_report_elasticsearch_gives_no_error_for() {
        when(client.bulk(any(Buffer.class))).thenReturn(Single.just(response(true, item(201, null), item(500, null))));

        cut
            .send(BULK)
            .test()
            .assertError(
                error ->
                    error instanceof BulkItemsFailureException failure &&
                    failure.failures().equals(List.of(new ItemFailure(1, true, "status 500")))
            );
    }

    @Test
    void should_fail_with_a_retryable_error_when_a_failed_bulk_does_not_list_its_items() {
        final BulkResponse response = response(true);
        response.setItems(null);
        when(client.bulk(any(Buffer.class))).thenReturn(Single.just(response));

        cut.send(BULK).test().assertError(SendReportException.class);
    }

    @Test
    void should_fail_with_a_retryable_error_when_a_failed_bulk_lists_no_item() {
        when(client.bulk(any(Buffer.class))).thenReturn(Single.just(response(true)));

        cut.send(BULK).test().assertError(SendReportException.class);
    }

    private static BulkResponse response(boolean errors, BulkItemResponse... items) {
        var response = new BulkResponse();
        response.setErrors(errors);
        response.setItems(Arrays.asList(items));
        return response;
    }

    private static BulkItemResponse item(int status, Failure error) {
        var index = new Index();
        index.setStatus(status);
        index.setError(error);
        var item = new BulkItemResponse();
        item.setIndex(index);
        return item;
    }

    private static Failure failure(String type, String reason) {
        var failure = new Failure();
        failure.setType(type);
        failure.setReason(reason);
        return failure;
    }
}
