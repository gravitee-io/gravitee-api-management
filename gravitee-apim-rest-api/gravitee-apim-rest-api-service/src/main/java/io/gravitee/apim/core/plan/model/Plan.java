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
package io.gravitee.apim.core.plan.model;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Define a Plan for API.
 *
 * <p>⚠️ It is a merged definition of V2 and V4 plans.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Setter
// Implement GenericPlanEntity to ease transition but it should be removed once core elements only use Plan instead of GenericPlanEntity
public class Plan implements GenericPlanEntity {

    private String id;
    /**
     * The definition version of the Plan.
     *
     * <p><code>null</code> for V2/V1 plans because it is null for APIs</p>
     */
    private DefinitionVersion definitionVersion;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;
    private String name;
    private String description;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ZonedDateTime publishedAt;
    private ZonedDateTime closedAt;

    private Date needRedeployAt; // java.util.Date required to implement GenericPlanEntity

    /** The way to validate subscriptions */
    private PlanValidationType validation;

    @Builder.Default
    private PlanType type = PlanType.API;

    private String apiId;

    private String environmentId;

    private int order;

    @Builder.Default
    private List<String> characteristics = new ArrayList<>();

    @Builder.Default
    private List<String> excludedGroups = new ArrayList<>();

    private boolean commentRequired;
    private String commentMessage;
    private String generalConditions;

    // FIXME: Kafka Gateway - will not be easy to know if we have to manipulate Plan or NativePlan as it is the ApiType which determines the distinction
    private io.gravitee.definition.model.v4.plan.Plan planDefinitionV4;
    private io.gravitee.definition.model.Plan planDefinitionV2;
    private io.gravitee.definition.model.federation.FederatedPlan federatedPlanDefinition;

    public Plan(String apiId, io.gravitee.definition.model.v4.plan.Plan planDefinitionV4) {
        this.setDefinitionVersion(DefinitionVersion.V4);
        this.setPlanDefinitionV4(planDefinitionV4);
        this.setId(planDefinitionV4.getId());
        this.setName(planDefinitionV4.getName());
        this.setPlanMode(planDefinitionV4.getMode());
        this.setPlanStatus(planDefinitionV4.getStatus());
        this.setPlanTags(planDefinitionV4.getTags());
        this.setType(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
        this.setApiId(apiId);
    }

    @Override
    public io.gravitee.rest.api.model.v4.plan.PlanType getPlanType() {
        return io.gravitee.rest.api.model.v4.plan.PlanType.valueOf(type.name());
    }

    @Override
    public PlanSecurity getPlanSecurity() {
        return switch (definitionVersion) {
            case V4 -> planDefinitionV4.getSecurity();
            case V1, V2 -> new PlanSecurity(planDefinitionV2.getSecurity(), planDefinitionV2.getSecurityDefinition());
            case FEDERATED -> federatedPlanDefinition.getSecurity();
        };
    }

    @Override
    public PlanStatus getPlanStatus() {
        return switch (definitionVersion) {
            case V4 -> planDefinitionV4.getStatus();
            case V1, V2 -> PlanStatus.valueOf(planDefinitionV2.getStatus());
            case FEDERATED -> federatedPlanDefinition.getStatus();
        };
    }

    public Plan setPlanStatus(PlanStatus planStatus) {
        switch (definitionVersion) {
            case V4 -> planDefinitionV4.setStatus(planStatus);
            case V1, V2 -> planDefinitionV2.setStatus(planStatus.name());
            case FEDERATED -> federatedPlanDefinition.setStatus(planStatus);
        }
        return this;
    }

    @Override
    public PlanMode getPlanMode() {
        return switch (definitionVersion) {
            case V4 -> planDefinitionV4.getMode();
            case V1, V2 -> PlanMode.STANDARD;
            case FEDERATED -> federatedPlanDefinition.getMode();
        };
    }

    public Plan setPlanMode(PlanMode planMode) {
        if (definitionVersion == DefinitionVersion.V4) {
            planDefinitionV4.setMode(planMode);
        }
        return this;
    }

    public Plan setPlanId(String id) {
        this.id = id;
        switch (definitionVersion) {
            case V4 -> planDefinitionV4.setId(id);
            case V1, V2 -> planDefinitionV2.setId(id);
            case FEDERATED -> federatedPlanDefinition.setId(id);
        }
        return this;
    }

    public Plan setGeneralConditions(String generalConditions) {
        this.generalConditions = generalConditions;
        return this;
    }

    public Plan setPlanTags(Set<String> tags) {
        switch (definitionVersion) {
            case V4 -> planDefinitionV4.setTags(tags);
            case V1, V2 -> planDefinitionV2.setTags(tags);
            case FEDERATED -> {
                // do nothing
            }
        }
        return this;
    }

    @Override
    public io.gravitee.rest.api.model.v4.plan.PlanValidationType getPlanValidation() {
        return io.gravitee.rest.api.model.v4.plan.PlanValidationType.valueOf(validation.name());
    }

    public enum PlanValidationType {
        /** Subscription is automatically validated without any human action. */
        AUTO,

        /** Subscription requires a human validation. */
        MANUAL,
    }

    public enum PlanType {
        API,
        CATALOG,
    }

    public boolean isFederated() {
        return this.definitionVersion == DefinitionVersion.FEDERATED;
    }

    public boolean isApiKey() {
        return switch (definitionVersion) {
            case V4 -> planDefinitionV4.isApiKey();
            case FEDERATED -> federatedPlanDefinition.isApiKey();
            default -> planDefinitionV2.isApiKey();
        };
    }

    public boolean isClosed() {
        return getPlanStatus() == PlanStatus.CLOSED;
    }

    public boolean isDeprecated() {
        return getPlanStatus() == PlanStatus.DEPRECATED;
    }

    public boolean isPublished() {
        return getPlanStatus() == PlanStatus.PUBLISHED;
    }

    public Plan close() {
        if (isClosed()) {
            throw new ValidationDomainException("Plan " + id + " is already closed");
        }
        final ZonedDateTime closeDateTime = TimeProvider.now();
        return toBuilder()
            .closedAt(closeDateTime)
            .updatedAt(closeDateTime)
            .needRedeployAt(Date.from(closeDateTime.toInstant()))
            .build()
            .setPlanStatus(PlanStatus.CLOSED);
    }

    public Plan update(Plan updated) {
        return toBuilder()
            .name(updated.name)
            .description(updated.description)
            .order(updated.order)
            .updatedAt(TimeProvider.now())
            .planDefinitionV4(updated.planDefinitionV4)
            .planDefinitionV2(updated.planDefinitionV2)
            .federatedPlanDefinition(
                updated.federatedPlanDefinition != null
                    ? federatedPlanDefinition.update(updated.federatedPlanDefinition)
                    : federatedPlanDefinition
            )
            .commentRequired(updated.commentRequired)
            .commentMessage(updated.commentMessage)
            .generalConditions(updated.generalConditions)
            .excludedGroups(updated.excludedGroups)
            .characteristics(updated.characteristics)
            .crossId(updated.crossId == null ? crossId : updated.crossId)
            .validation(updated.validation)
            .build();
    }

    /**
     * Create copy of the Plan (definition included)
     *
     * <p>Useful essentially for testing</p>
     *
     * @return A copy of the Plan
     */
    public Plan copy() {
        return switch (definitionVersion) {
            case V4 -> toBuilder().planDefinitionV4(planDefinitionV4.toBuilder().build()).build();
            case V1, V2 -> toBuilder().planDefinitionV2(planDefinitionV2.toBuilder().build()).build();
            case FEDERATED -> toBuilder().federatedPlanDefinition(federatedPlanDefinition.toBuilder().build()).build();
        };
    }

    public Plan rollbackTo(io.gravitee.definition.model.v4.plan.Plan planDefinitionV4) {
        var existingPlanDefinitionV4 = this.getPlanDefinitionV4();

        // Update plan properties from API definition
        existingPlanDefinitionV4.setName(planDefinitionV4.getName());
        this.setName(planDefinitionV4.getName());
        existingPlanDefinitionV4.setTags(planDefinitionV4.getTags());
        existingPlanDefinitionV4.setSecurity(planDefinitionV4.getSecurity());
        existingPlanDefinitionV4.setFlows(planDefinitionV4.getFlows());
        existingPlanDefinitionV4.setSelectionRule(planDefinitionV4.getSelectionRule());

        // Special case if plan are closed or deprecated we restore status from API definition
        if (this.getPlanStatus() != planDefinitionV4.getStatus()) {
            existingPlanDefinitionV4.setStatus(planDefinitionV4.getStatus());
            this.setClosedAt(null);
            this.setUpdatedAt(TimeProvider.now());
            this.setNeedRedeployAt(Date.from(this.getUpdatedAt().toInstant()));
        }
        return this;
    }

    public abstract static class PlanBuilder<C extends Plan, B extends PlanBuilder<C, B>> {

        public B planDefinition(io.gravitee.definition.model.Plan planDefinition) {
            this.planDefinitionV2 = planDefinition;
            if (planDefinition != null) {
                this.definitionVersion = DefinitionVersion.V2;
            }
            return self();
        }

        public B planDefinitionV4(io.gravitee.definition.model.v4.plan.Plan planDefinitionV4) {
            this.planDefinitionV4 = planDefinitionV4;
            if (planDefinitionV4 != null) {
                this.definitionVersion = DefinitionVersion.V4;
            }
            return self();
        }

        public B federatedPlanDefinition(io.gravitee.definition.model.federation.FederatedPlan federatedPlan) {
            this.federatedPlanDefinition = federatedPlan;
            if (federatedPlan != null) {
                this.definitionVersion = DefinitionVersion.FEDERATED;
            }
            return self();
        }
    }
}
