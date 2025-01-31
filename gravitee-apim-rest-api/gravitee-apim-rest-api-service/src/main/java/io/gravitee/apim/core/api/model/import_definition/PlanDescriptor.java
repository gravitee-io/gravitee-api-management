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
package io.gravitee.apim.core.api.model.import_definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Builder;

public sealed interface PlanDescriptor {
    String id();
    String name();

    PlanStatus status();

    @JsonIgnore
    default boolean closed() {
        return PlanStatus.CLOSED.equals(status());
    }

    @Builder
    record PlanDescriptorV4(
        String id,
        String crossId,
        String name,
        DefinitionVersion definitionVersion,
        String description,

        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant closedAt,

        Plan.PlanValidationType validation,
        Plan.PlanType type,
        PlanMode mode,
        PlanSecurity security,

        Set<String> tags,

        String selectionRule,
        PlanStatus status,
        String apiId,
        String environmentId,

        int order,
        List<String> characteristics,
        List<String> excludedGroups,
        boolean commentRequired,
        String commentMessage,
        String generalConditions,

        List<? extends AbstractFlow> flows
    )
        implements PlanDescriptor {
        @JsonProperty("tags")
        public Set<String> tags() {
            return tags != null ? tags : Set.of();
        }
    }
}
