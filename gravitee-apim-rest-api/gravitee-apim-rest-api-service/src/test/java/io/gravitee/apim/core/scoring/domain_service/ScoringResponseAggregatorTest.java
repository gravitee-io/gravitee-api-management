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
package io.gravitee.apim.core.scoring.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScoringResponseAggregatorTest {

    private static final ScoringReport.Asset SWAGGER_ASSET = new ScoringReport.Asset(
        "swagger-page",
        ScoringAssetType.SWAGGER,
        List.of(),
        List.of()
    );
    private static final ScoringReport.Asset ASYNCAPI_ASSET = new ScoringReport.Asset(
        "async-page",
        ScoringAssetType.ASYNCAPI,
        List.of(),
        List.of()
    );

    ScoringResponseAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new ScoringResponseAggregator();
    }

    @Test
    void should_return_assets_immediately_when_single_response_expected() {
        var result = aggregator.accumulate("job-id", 1, List.of(SWAGGER_ASSET));

        assertThat(result).contains(List.of(SWAGGER_ASSET));
    }

    @Test
    void should_wait_until_all_partitioned_responses_are_received() {
        assertThat(aggregator.accumulate("job-id", 2, List.of(SWAGGER_ASSET))).isEmpty();
        assertThat(aggregator.accumulate("job-id", 2, List.of(ASYNCAPI_ASSET))).contains(List.of(SWAGGER_ASSET, ASYNCAPI_ASSET));
    }

    @Test
    void should_cleanup_pending_response_and_discard_subsequent_responses() {
        // Accumulate first response
        assertThat(aggregator.accumulate("job-id", 2, List.of(SWAGGER_ASSET))).isEmpty();

        // Cleanup (simulates job timeout/error)
        aggregator.cleanup("job-id");

        // Subsequent response starts fresh accumulation instead of completing the old one
        assertThat(aggregator.accumulate("job-id", 2, List.of(ASYNCAPI_ASSET))).isEmpty();
    }

    @Test
    void should_handle_cleanup_for_nonexistent_job() {
        // Should not throw when cleaning up a job that was never accumulated
        aggregator.cleanup("nonexistent-job");
    }
}
