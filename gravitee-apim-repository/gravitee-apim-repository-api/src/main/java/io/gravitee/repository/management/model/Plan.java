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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Plan {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        PLAN_CREATED,
        PLAN_UPDATED,
        PLAN_DELETED,
        PLAN_PUBLISHED,
        PLAN_CLOSED,
        PLAN_DEPRECATED,
    }

    private String id;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;

    private String name;

    private String description;

    private PlanSecurityType security;

    private String securityDefinition;

    private String selectionRule;

    /**
     * The way to validate subscriptions
     */
    private PlanValidationType validation;

    private PlanType type;
    private PlanMode mode;

    private Status status;

    /**
     * The position of the plan against the other Plans.
     */
    private int order;

    /**
     * The API used by this plan.
     */
    private String api;

    /**
     * Plan creation date
     */
    private Date createdAt;

    /**
     * Plan last update date
     */
    private Date updatedAt;

    /**
     * Plan publication date
     */
    private Date publishedAt;

    /**
     * Plan closing date
     */
    private Date closedAt;

    /**
     * The JSON payload of all policies to apply for this plan
     */
    private String definition;

    private List<String> characteristics;

    /**
     * List of excluded groups
     */
    private List<String> excludedGroups;

    /**
     * last time modification introduced an api redeployment
     */
    private Date needRedeployAt;

    private boolean commentRequired;

    private String commentMessage;

    private String generalConditions;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    public Plan(Plan cloned) {
        this.id = cloned.getId();
        this.crossId = cloned.getCrossId();
        this.name = cloned.getName();
        this.description = cloned.getDescription();
        this.security = cloned.getSecurity();
        this.validation = cloned.getValidation();
        this.type = cloned.getType();
        this.mode = cloned.getMode();
        this.status = cloned.getStatus();
        this.order = cloned.getOrder();
        this.api = cloned.getApi();
        this.createdAt = cloned.getCreatedAt();
        this.updatedAt = cloned.getUpdatedAt();
        this.publishedAt = cloned.getPublishedAt();
        this.closedAt = cloned.getClosedAt();
        this.definition = cloned.getDefinition();
        this.characteristics = cloned.getCharacteristics();
        this.excludedGroups = cloned.getExcludedGroups();
        this.needRedeployAt = cloned.getNeedRedeployAt();
        this.selectionRule = cloned.getSelectionRule();
        this.tags = cloned.getTags();
        this.generalConditions = cloned.getGeneralConditions();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plan plan = (Plan) o;

        return Objects.equals(id, plan.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return (
            "Plan{" +
            "id='" +
            id +
            '\'' +
            "crossId='" +
            crossId +
            '\'' +
            ", api='" +
            api +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", validation=" +
            validation +
            ", excludedGroups=" +
            excludedGroups +
            ", type=" +
            type +
            '}'
        );
    }

    public enum PlanType {
        /**
         * A plan for a single API.
         */
        API,

        /**
         * A plan for a bunch of APIs.
         */
        CATALOG,
    }

    public enum PlanMode {
        /**
         * A plan with the policy security required.
         */
        STANDARD,

        /**
         * Plan with no security required.
         * Used to manage Subscription plan (ex: Webhook)
         */
        PUSH,
    }

    public enum PlanSecurityType {
        /**
         * Plan which is using a key_less (ie. public) security authentication type for incoming HTTP requests.
         */
        KEY_LESS,

        /**
         * Plan which is using an api-key security authentication type for incoming HTTP requests.
         */
        API_KEY,

        /**
         * Plan which is using an OAuth2 security authentication type for incoming HTTP requests.
         */
        OAUTH2,

        /**
         * Plan which is using a JWT security authentication type for incoming HTTP requests.
         */
        JWT,
    }

    public enum PlanValidationType {
        /**
         * Subscription is automatically validated without any human action.
         */
        AUTO,

        /**
         * Subscription requires a human validation.
         */
        MANUAL,
    }

    public enum Status {
        /**
         * Plan is configured but not yet published
         */
        STAGING,

        /**
         * Plan is published to portal and can be used to make calls
         */
        PUBLISHED,

        /**
         * Plan is closed
         */
        CLOSED,

        /**
         * Plan is deprecated
         */
        DEPRECATED,
    }
}
