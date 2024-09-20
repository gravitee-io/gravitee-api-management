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

import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record ScoringReportView(String id, String apiId, ZonedDateTime createdAt, List<AssetView> assets, Summary summary) {
    public record Summary(Double score, Long all, Long errors, Long warnings, Long infos, Long hints) {
        public Summary(Double score, Long errors, Long warnings, Long infos, Long hints) {
            this(score, errors + warnings + infos + hints, errors, warnings, infos, hints);
        }
    }
    public record AssetView(String name, ScoringAssetType type, List<ScoringReport.Diagnostic> diagnostics) {}
}
