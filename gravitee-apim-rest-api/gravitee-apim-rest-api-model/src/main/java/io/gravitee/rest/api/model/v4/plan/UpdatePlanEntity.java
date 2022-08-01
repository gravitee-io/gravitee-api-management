/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.v4.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class UpdatePlanEntity {

    private String id;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;

    @NotNull
    private String name;

    @NotNull
    private String description;

    @NotNull
    private PlanValidationType validation;

    private List<String> characteristics;

    @NotNull
    private int order;

    private List<String> excludedGroups;

    private PlanSecurity security;

    private boolean commentRequired;

    private String commentMessage;

    private String generalConditions;

    private Set<String> tags;

    private String selectionRule;

    private List<Flow> flows;
}
