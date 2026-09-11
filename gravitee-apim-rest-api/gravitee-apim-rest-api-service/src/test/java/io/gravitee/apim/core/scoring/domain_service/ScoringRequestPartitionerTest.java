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

import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoringRequestPartitionerTest {

    private static final ScoreRequest.AssetToScore GRAVITEE = new ScoreRequest.AssetToScore(
        "api-id",
        new ScoreRequest.AssetType(ScoringAssetType.GRAVITEE_DEFINITION, ScoreRequest.Format.GRAVITEE_PROXY),
        "api",
        "{}"
    );
    private static final ScoreRequest.AssetToScore SWAGGER = new ScoreRequest.AssetToScore(
        "swagger-id",
        new ScoreRequest.AssetType(ScoringAssetType.SWAGGER),
        "swagger",
        "openapi: 3.0.0"
    );
    private static final ScoreRequest.AssetToScore ASYNCAPI = new ScoreRequest.AssetToScore(
        "asyncapi-id",
        new ScoreRequest.AssetType(ScoringAssetType.ASYNCAPI),
        "asyncapi",
        "asyncapi: 2.0.0"
    );

    @Test
    void should_keep_single_request_when_no_documentation_format_rulesets() {
        var request = request(
            List.of(GRAVITEE, SWAGGER),
            List.of(
                new ScoreRequest.CustomRuleset("gravitee", ScoringRuleset.Format.GRAVITEE_PROXY),
                new ScoreRequest.CustomRuleset("universal")
            )
        );

        assertThat(ScoringRequestPartitioner.partition(request)).containsExactly(request);
    }

    @Test
    void should_partition_documentation_format_rulesets_with_matching_assets_only() {
        var openApi = new ScoreRequest.CustomRuleset("openapi-rules", ScoringRuleset.Format.OPENAPI);
        var asyncApi = new ScoreRequest.CustomRuleset("asyncapi-rules", ScoringRuleset.Format.ASYNCAPI);
        var gravitee = new ScoreRequest.CustomRuleset("gravitee-rules", ScoringRuleset.Format.GRAVITEE_PROXY);
        var request = request(List.of(GRAVITEE, SWAGGER, ASYNCAPI), List.of(openApi, asyncApi, gravitee));

        var partitions = ScoringRequestPartitioner.partition(request);

        assertThat(partitions).hasSize(3);
        assertThat(partitions)
            .filteredOn(partition -> partition.assets().contains(GRAVITEE))
            .satisfiesOnlyOnce(partition -> assertThat(partition.customRulesets()).containsExactly(gravitee));
        assertThat(partitions)
            .filteredOn(partition -> partition.assets().contains(SWAGGER))
            .satisfiesOnlyOnce(partition -> assertThat(partition.customRulesets()).containsExactly(openApi));
        assertThat(partitions)
            .filteredOn(partition -> partition.assets().contains(ASYNCAPI))
            .satisfiesOnlyOnce(partition -> assertThat(partition.customRulesets()).containsExactly(asyncApi));
    }

    @Test
    void should_include_universal_rulesets_in_all_partitions() {
        var openApi = new ScoreRequest.CustomRuleset("openapi-rules", ScoringRuleset.Format.OPENAPI);
        var asyncApi = new ScoreRequest.CustomRuleset("asyncapi-rules", ScoringRuleset.Format.ASYNCAPI);
        var universal = new ScoreRequest.CustomRuleset("universal-rules"); // null format
        var request = request(List.of(SWAGGER, ASYNCAPI), List.of(openApi, asyncApi, universal));

        var partitions = ScoringRequestPartitioner.partition(request);

        assertThat(partitions).hasSize(2);
        // Universal rulesets should be included in both partitions
        assertThat(partitions)
            .filteredOn(partition -> partition.assets().contains(SWAGGER))
            .satisfiesOnlyOnce(partition -> assertThat(partition.customRulesets()).containsExactlyInAnyOrder(openApi, universal));
        assertThat(partitions)
            .filteredOn(partition -> partition.assets().contains(ASYNCAPI))
            .satisfiesOnlyOnce(partition -> assertThat(partition.customRulesets()).containsExactlyInAnyOrder(asyncApi, universal));
    }

    @Test
    void should_handle_empty_rulesets() {
        var request = request(List.of(SWAGGER, ASYNCAPI), List.of());

        var partitions = ScoringRequestPartitioner.partition(request);

        // With no rulesets, should return original request unchanged
        assertThat(partitions).containsExactly(request);
    }

    @Test
    void should_handle_only_universal_rulesets() {
        var universal1 = new ScoreRequest.CustomRuleset("universal-1");
        var universal2 = new ScoreRequest.CustomRuleset("universal-2");
        var request = request(List.of(SWAGGER, ASYNCAPI), List.of(universal1, universal2));

        var partitions = ScoringRequestPartitioner.partition(request);

        // With only universal rulesets (no format-specific), no partitioning needed
        assertThat(partitions).containsExactly(request);
    }

    @Test
    void should_handle_only_documentation_pages_without_gravitee_definition() {
        var openApi = new ScoreRequest.CustomRuleset("openapi-rules", ScoringRuleset.Format.OPENAPI);
        var gravitee = new ScoreRequest.CustomRuleset("gravitee-rules", ScoringRuleset.Format.GRAVITEE_PROXY);
        var request = request(List.of(SWAGGER), List.of(openApi, gravitee));

        var partitions = ScoringRequestPartitioner.partition(request);

        // Gravitee ruleset should be filtered out since there's no Gravitee definition asset
        assertThat(partitions).hasSize(1);
        assertThat(partitions.get(0).assets()).containsExactly(SWAGGER);
        assertThat(partitions.get(0).customRulesets()).containsExactly(openApi);
    }

    @Test
    void should_filter_out_inapplicable_rulesets_for_asset_types() {
        var asyncApi = new ScoreRequest.CustomRuleset("asyncapi-rules", ScoringRuleset.Format.ASYNCAPI);
        var request = request(List.of(SWAGGER), List.of(asyncApi)); // AsyncAPI ruleset but only Swagger asset

        var partitions = ScoringRequestPartitioner.partition(request);

        // AsyncAPI ruleset should be filtered out, leaving only the original request with empty rulesets
        assertThat(partitions).hasSize(1);
        assertThat(partitions.get(0).customRulesets()).isEmpty();
    }

    private static ScoreRequest request(List<ScoreRequest.AssetToScore> assets, List<ScoreRequest.CustomRuleset> rulesets) {
        return new ScoreRequest("job-id", "org-id", "env-id", "api-id", assets, rulesets, List.of());
    }
}
