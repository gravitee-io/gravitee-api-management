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
package io.gravitee.rest.api.model.v4.plan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.DeploymentRequired;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class BasePlanEntity implements GenericPlanEntity {

    private String id;

    @Builder.Default
    private DefinitionVersion definitionVersion = DefinitionVersion.V4;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;
    private String name;
    private String description;
    private Date createdAt;
    private Date updatedAt;
    private Date publishedAt;
    private Date closedAt;

    @JsonIgnore
    private Date needRedeployAt;

    /**
     * The way to validate subscriptions
     */
    private PlanValidationType validation;

    private PlanType type;

    private PlanMode mode;

    @DeploymentRequired
    private PlanSecurity security;

    @DeploymentRequired
    private String selectionRule;

    @DeploymentRequired
    private Set<String> tags;

    @DeploymentRequired
    private PlanStatus status;

    @DeploymentRequired
    private String apiId;

    private String environmentId;

    private int order;
    private List<String> characteristics;
    private List<String> excludedGroups;
    private boolean commentRequired;
    private String commentMessage;
    private String generalConditions;
    private ApiType apiType;

    @Override
    public PlanType getPlanType() {
        return type;
    }

    @Override
    public PlanMode getPlanMode() {
        return mode;
    }

    @Override
    public PlanSecurity getPlanSecurity() {
        return security;
    }

    @Override
    public PlanStatus getPlanStatus() {
        return status;
    }

    @Override
    public PlanValidationType getPlanValidation() {
        return validation;
    }
}
