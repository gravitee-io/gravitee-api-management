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
package io.gravitee.apim.core.analytics.model;

import io.gravitee.definition.model.DefinitionVersion;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@Builder
public class AnalyticsQueryParameters {

    @With
    List<String> apiIds;

    long from;
    long to;

    @With
    Collection<DefinitionVersion> definitionVersions;

    /**
     * Create a copy with the given apiIds. Explicit method for compatibility when Lombok @With is not processed. Look again akm
     */
    public AnalyticsQueryParameters withApiIds(List<String> apiIds) {
        return AnalyticsQueryParameters.builder()
            .from(this.from)
            .to(this.to)
            .apiIds(apiIds)
            .definitionVersions(this.definitionVersions)
            .build();
    }

    /**
     * Create a copy with the given definitionVersions. Explicit method for compatibility when Lombok @With is not processed.
     */
    public AnalyticsQueryParameters withDefinitionVersions(Collection<DefinitionVersion> definitionVersions) {
        return AnalyticsQueryParameters.builder()
            .from(this.from)
            .to(this.to)
            .apiIds(this.apiIds)
            .definitionVersions(definitionVersions)
            .build();
    }
}
