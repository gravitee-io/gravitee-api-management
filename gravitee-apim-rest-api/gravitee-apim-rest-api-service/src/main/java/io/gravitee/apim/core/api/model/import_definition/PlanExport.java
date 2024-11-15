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

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class PlanExport {

    private String id;
    private String crossId;
    private String name;
    private DefinitionVersion definitionVersion;
    private String description;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private Instant closedAt;

    private Plan.PlanValidationType validation;
    private Plan.PlanType type;
    private PlanMode mode;
    private PlanSecurity security;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    private String selectionRule;
    private PlanStatus status;
    private String apiId;
    private String environmentId;

    private int order;
    private List<String> characteristics;
    private List<String> excludedGroups;
    private boolean commentRequired;
    private String commentMessage;
    private String generalConditions;
    private ApiType apiType;

    private List<Flow> flows;
}
