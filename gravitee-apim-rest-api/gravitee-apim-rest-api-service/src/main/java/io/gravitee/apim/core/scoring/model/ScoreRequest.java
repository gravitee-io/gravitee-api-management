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
package io.gravitee.apim.core.scoring.model;

import static java.util.Collections.emptyList;

import java.util.List;

public record ScoreRequest(
    String jobId,
    String organizationId,
    String environmentId,
    String apiId,
    List<AssetToScore> assets,
    List<CustomRuleset> customRulesets,
    List<Function> customFunctions
) {
    public ScoreRequest(String jobId, String organizationId, String environmentId, String apiId, List<AssetToScore> assets) {
        this(jobId, organizationId, environmentId, apiId, assets, emptyList(), emptyList());
    }

    public record AssetToScore(String assetId, AssetType assetType, String assetName, String content) {}

    public record AssetType(ScoringAssetType type, Format format) {
        public AssetType(ScoringAssetType type) {
            this(type, null);
        }
    }

    public record CustomRuleset(String content, Format format) {
        public CustomRuleset(String content) {
            this(content, null);
        }
    }

    public record Function(String filename, String content) {}

    public enum Format {
        GRAVITEE_PROXY,
        GRAVITEE_MESSAGE,
        GRAVITEE_FEDERATED,
        GRAVITEE_NATIVE,
        GRAVITEE_V2,
    }
}
