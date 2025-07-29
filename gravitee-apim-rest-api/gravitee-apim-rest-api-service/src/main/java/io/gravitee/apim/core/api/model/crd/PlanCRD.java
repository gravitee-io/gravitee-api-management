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
package io.gravitee.apim.core.api.model.crd;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
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
public class PlanCRD {

    private String id;

    private String crossId;

    private String hrid;

    private String name;

    private String description;

    private PlanSecurity security;

    private List<String> characteristics;

    private List<String> excludedGroups;

    private String generalConditions;

    private int order;

    private String selectionRule;

    private PlanStatus status;

    private Set<String> tags;

    private Plan.PlanType type;

    private Plan.PlanValidationType validation;

    @Builder.Default
    private List<? extends AbstractFlow> flows = List.of();

    private PlanMode mode;
}
