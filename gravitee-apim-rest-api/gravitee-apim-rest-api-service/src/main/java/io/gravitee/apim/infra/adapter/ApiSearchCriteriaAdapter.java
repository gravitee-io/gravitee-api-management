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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.util.stream.Collectors;

public class ApiSearchCriteriaAdapter {

    public static ApiSearchCriteriaAdapter INSTANCE = new ApiSearchCriteriaAdapter();

    private ApiSearchCriteriaAdapter() {}

    public ApiCriteria toCriteriaForRepository(ApiSearchCriteria criteria) {
        var builder = new ApiCriteria.Builder();
        if (criteria != null) {
            if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                builder.ids(criteria.getIds());
            }
            if (criteria.getState() != null) {
                builder.state(LifecycleState.valueOf(criteria.getState().name()));
            }
            if (criteria.getVisibility() != null) {
                builder.visibility(Visibility.valueOf(criteria.getVisibility().name()));
            }
            if (criteria.getLifecycleStates() != null && !criteria.getLifecycleStates().isEmpty()) {
                builder.lifecycleStates(
                    criteria
                        .getLifecycleStates()
                        .stream()
                        .map(s -> ApiLifecycleState.valueOf(s.name()))
                        .collect(Collectors.toList())
                );
            }
            if (criteria.getDefinitionVersion() != null && !criteria.getDefinitionVersion().isEmpty()) {
                builder.definitionVersion(
                    criteria
                        .getDefinitionVersion()
                        .stream()
                        .map(v -> DefinitionVersion.valueOf(v.name()))
                        .toList()
                );
            }
            builder
                .groups(criteria.getGroups())
                .category(criteria.getCategory())
                .label(criteria.getLabel())
                .version(criteria.getVersion())
                .name(criteria.getName())
                .environmentId(criteria.getEnvironmentId())
                .environments(criteria.getEnvironments())
                .crossId(criteria.getCrossId())
                .integrationId(criteria.getIntegrationId());
        }
        return builder.build();
    }
}
