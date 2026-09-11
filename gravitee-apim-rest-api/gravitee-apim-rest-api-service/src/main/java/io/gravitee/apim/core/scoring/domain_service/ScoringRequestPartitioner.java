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

import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Splits scoring requests so documentation-format rulesets are only sent with matching documentation assets.
 * Cockpit applies null-format custom rulesets to every asset; OpenAPI and AsyncAPI rulesets must therefore
 * travel in separate requests when both documentation types are present.
 */
public final class ScoringRequestPartitioner {

    private ScoringRequestPartitioner() {}

    public static List<ScoreRequest> partition(ScoreRequest request) {
        var filtered = filterRulesets(request);
        if (!mustSplit(filtered)) {
            return List.of(filtered);
        }

        var assetsByType = filtered.assets().stream().collect(Collectors.groupingBy(asset -> asset.assetType().type()));
        var graviteeAssets = assetsByType.getOrDefault(ScoringAssetType.GRAVITEE_DEFINITION, List.of());
        var swaggerAssets = assetsByType.getOrDefault(ScoringAssetType.SWAGGER, List.of());
        var asyncApiAssets = assetsByType.getOrDefault(ScoringAssetType.ASYNCAPI, List.of());

        var universalRulesets = rulesetsWithFormat(filtered.customRulesets(), null);
        var graviteeRulesets = graviteeFormatRulesets(filtered.customRulesets());
        var openApiRulesets = rulesetsWithFormat(filtered.customRulesets(), ScoringRuleset.Format.OPENAPI);
        var asyncApiRulesets = rulesetsWithFormat(filtered.customRulesets(), ScoringRuleset.Format.ASYNCAPI);

        List<ScoreRequest> partitions = new ArrayList<>();

        if (!graviteeAssets.isEmpty()) {
            var rulesets = concat(graviteeRulesets, universalRulesets);
            if (!rulesets.isEmpty()) {
                partitions.add(copy(filtered, graviteeAssets, rulesets));
            }
        }
        if (!swaggerAssets.isEmpty()) {
            var rulesets = concat(openApiRulesets, universalRulesets);
            if (!rulesets.isEmpty()) {
                partitions.add(copy(filtered, swaggerAssets, rulesets));
            }
        }
        if (!asyncApiAssets.isEmpty()) {
            var rulesets = concat(asyncApiRulesets, universalRulesets);
            if (!rulesets.isEmpty()) {
                partitions.add(copy(filtered, asyncApiAssets, rulesets));
            }
        }

        return partitions.isEmpty() ? List.of(filtered) : partitions;
    }

    private static boolean mustSplit(ScoreRequest request) {
        return request
            .customRulesets()
            .stream()
            .map(ScoreRequest.CustomRuleset::format)
            .anyMatch(format -> format == ScoringRuleset.Format.OPENAPI || format == ScoringRuleset.Format.ASYNCAPI);
    }

    private static ScoreRequest filterRulesets(ScoreRequest request) {
        var assetTypes = request
            .assets()
            .stream()
            .map(asset -> asset.assetType().type())
            .collect(Collectors.toSet());
        var filteredRulesets = request
            .customRulesets()
            .stream()
            .filter(ruleset -> isApplicable(ruleset, assetTypes))
            .toList();

        if (filteredRulesets.size() == request.customRulesets().size()) {
            return request;
        }
        return new ScoreRequest(
            request.jobId(),
            request.organizationId(),
            request.environmentId(),
            request.apiId(),
            request.assets(),
            filteredRulesets,
            request.customFunctions()
        );
    }

    private static boolean isApplicable(ScoreRequest.CustomRuleset ruleset, Set<ScoringAssetType> assetTypes) {
        if (ruleset.format() == null) {
            return true;
        }
        return switch (ruleset.format()) {
            case OPENAPI -> assetTypes.contains(ScoringAssetType.SWAGGER);
            case ASYNCAPI -> assetTypes.contains(ScoringAssetType.ASYNCAPI);
            case GRAVITEE_FEDERATION, GRAVITEE_MESSAGE, GRAVITEE_PROXY, GRAVITEE_NATIVE, GRAVITEE_V2 -> assetTypes.contains(
                ScoringAssetType.GRAVITEE_DEFINITION
            );
        };
    }

    private static List<ScoreRequest.CustomRuleset> graviteeFormatRulesets(List<ScoreRequest.CustomRuleset> rulesets) {
        return rulesets
            .stream()
            .filter(
                ruleset ->
                    ruleset.format() != null &&
                    ruleset.format() != ScoringRuleset.Format.OPENAPI &&
                    ruleset.format() != ScoringRuleset.Format.ASYNCAPI
            )
            .toList();
    }

    private static List<ScoreRequest.CustomRuleset> rulesetsWithFormat(
        List<ScoreRequest.CustomRuleset> rulesets,
        ScoringRuleset.Format format
    ) {
        return rulesets
            .stream()
            .filter(ruleset -> ruleset.format() == format)
            .toList();
    }

    @SafeVarargs
    private static List<ScoreRequest.CustomRuleset> concat(List<ScoreRequest.CustomRuleset>... lists) {
        return List.of(lists).stream().flatMap(List::stream).toList();
    }

    private static ScoreRequest copy(
        ScoreRequest request,
        List<ScoreRequest.AssetToScore> assets,
        List<ScoreRequest.CustomRuleset> rulesets
    ) {
        return new ScoreRequest(
            request.jobId(),
            request.organizationId(),
            request.environmentId(),
            request.apiId(),
            assets,
            rulesets,
            request.customFunctions()
        );
    }
}
