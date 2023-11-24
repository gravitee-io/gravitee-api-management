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

import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Define a Plan for API.
 *
 * <p>⚠️ It is a merged definition of V2 and V4 plans.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
// Implement GenericPlanEntity to ease transition but it should be removed once core elements only use Plan instead of GenericPlanEntity
public class Plan implements GenericPlanEntity {

    private String id;
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

    @Builder.Default
    private PlanMode mode = PlanMode.STANDARD;

    private PlanSecurity security;

    private String selectionRule;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    private PlanStatus status;

    private String apiId;

    private int order;

    @Builder.Default
    private List<String> characteristics = new ArrayList<>();

    @Builder.Default
    private List<String> excludedGroups = new ArrayList<>();

    private boolean commentRequired;
    private String commentMessage;
    private String generalConditions;

    /** Use only for V2 plans */
    private Map<String, List<Rule>> paths;

    @Override
    public io.gravitee.rest.api.model.v4.plan.PlanType getPlanType() {
        return io.gravitee.rest.api.model.v4.plan.PlanType.valueOf(type.name());
    }

    @Override
    public PlanSecurity getPlanSecurity() {
        return this.security;
    }

    @Override
    public PlanStatus getPlanStatus() {
        return this.status;
    }

    @Override
    public io.gravitee.rest.api.model.v4.plan.PlanMode getPlanMode() {
        return io.gravitee.rest.api.model.v4.plan.PlanMode.valueOf(mode.name());
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
}
