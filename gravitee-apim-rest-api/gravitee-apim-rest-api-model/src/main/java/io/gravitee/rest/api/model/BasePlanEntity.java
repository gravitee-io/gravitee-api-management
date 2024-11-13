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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.SuperBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@With
@ToString
public class BasePlanEntity implements GenericPlanEntity {

    private String id;

    @Builder.Default
    private DefinitionVersion definitionVersion = DefinitionVersion.V2;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;

    @NotNull
    private String name;

    @NotNull
    private String description;

    /**
     * The way to validate subscriptions
     */
    @NotNull
    private PlanValidationType validation;

    @DeploymentRequired
    @NotNull
    private PlanSecurityType security;

    @DeploymentRequired
    private String securityDefinition;

    @NotNull
    private PlanType type;

    @DeploymentRequired
    @NotNull
    private PlanStatus status;

    @DeploymentRequired
    private String api;

    private String environmentId;

    private int order;

    /**
     * Plan creation date
     */
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * Plan last update date
     */
    @JsonProperty("updated_at")
    private Date updatedAt;

    /**
     * Plan publication date
     */
    @JsonProperty("published_at")
    private Date publishedAt;

    /**
     * Plan closing date
     */
    @JsonProperty("closed_at")
    private Date closedAt;

    @DeploymentRequired
    @JsonProperty(value = "paths", required = true)
    private Map<String, List<Rule>> paths = new HashMap<>();

    private List<String> characteristics;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    /**
     * last time modification introduced an api redeployment
     */
    @JsonIgnore
    private Date needRedeployAt;

    @JsonProperty("comment_required")
    private boolean commentRequired;

    @JsonProperty("comment_message")
    private String commentMessage;

    @JsonProperty("general_conditions")
    private String generalConditions;

    @DeploymentRequired
    private Set<String> tags;

    @DeploymentRequired
    @JsonProperty("selection_rule")
    private String selectionRule;

    @Override
    @JsonIgnore
    public String getApiId() {
        return api;
    }

    @Override
    @JsonIgnore
    public PlanSecurity getPlanSecurity() {
        PlanSecurity planSecurity = new PlanSecurity();
        if (security != null) {
            planSecurity.setType(PlanSecurityType.valueOf(security.name()).name());
        }
        planSecurity.setConfiguration(securityDefinition);
        return planSecurity;
    }

    @Override
    @JsonIgnore
    public io.gravitee.definition.model.v4.plan.PlanStatus getPlanStatus() {
        if (status != null) {
            return io.gravitee.definition.model.v4.plan.PlanStatus.valueOfLabel(status.name());
        }
        return null;
    }

    @Override
    public PlanMode getPlanMode() {
        // V2 API only manage STANDARD mode
        return PlanMode.STANDARD;
    }

    @Override
    @JsonIgnore
    public io.gravitee.rest.api.model.v4.plan.PlanValidationType getPlanValidation() {
        if (validation != null) {
            return io.gravitee.rest.api.model.v4.plan.PlanValidationType.valueOfLabel(validation.name());
        }
        return null;
    }

    @Override
    @JsonIgnore
    public io.gravitee.rest.api.model.v4.plan.PlanType getPlanType() {
        return io.gravitee.rest.api.model.v4.plan.PlanType.API;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasePlanEntity that = (BasePlanEntity) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
