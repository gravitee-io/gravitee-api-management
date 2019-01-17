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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Plan {
    public enum AuditEvent implements Audit.AuditEvent {
        PLAN_CREATED, PLAN_UPDATED, PLAN_DELETED, PLAN_PUBLISHED, PLAN_CLOSED
    }

    private String id;

    private String name;

    private String description;

    private PlanSecurityType security;

    private String securityDefinition;

    /**
     * The way to validate subscriptions
     */
    private PlanValidationType validation;

    private PlanType type;

    private Status status;

    /**
     * The position of the plan against the other Plans.
     */
    private int order;

    /**
     * List of API used by this plan.
     */
    private Set<String> apis;

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

    public Plan() {
    }

    public Plan(Plan cloned) {
        this.id = cloned.getId();
        this.name = cloned.getName();
        this.description = cloned.getDescription();
        this.security = cloned.getSecurity();
        this.validation = cloned.getValidation();
        this.type = cloned.getType();
        this.status = cloned.getStatus();
        this.order = cloned.getOrder();
        this.apis = cloned.getApis();
        this.createdAt = cloned.getCreatedAt();
        this.updatedAt = cloned.getUpdatedAt();
        this.publishedAt = cloned.getPublishedAt();
        this.closedAt = cloned.getClosedAt();
        this.definition = cloned.getDefinition();
        this.characteristics = cloned.getCharacteristics();
        this.excludedGroups = cloned.getExcludedGroups();
        this.needRedeployAt = cloned.getNeedRedeployAt();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PlanValidationType getValidation() {
        return validation;
    }

    public void setValidation(PlanValidationType validation) {
        this.validation = validation;
    }

    public PlanType getType() {
        return type;
    }

    public void setType(PlanType type) {
        this.type = type;
    }

    public Set<String> getApis() {
        return apis;
    }

    public void setApis(Set<String> apis) {
        this.apis = apis;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public List<String> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<String> characteristics) {
        this.characteristics = characteristics;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public PlanSecurityType getSecurity() {
        return security;
    }

    public void setSecurity(PlanSecurityType security) {
        this.security = security;
    }

    public String getSecurityDefinition() {
        return securityDefinition;
    }

    public void setSecurityDefinition(String securityDefinition) {
        this.securityDefinition = securityDefinition;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }

    public void setExcludedGroups(List<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public Date getNeedRedeployAt() {
        return needRedeployAt;
    }

    public void setNeedRedeployAt(Date needRedeployAt) {
        this.needRedeployAt = needRedeployAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plan plan = (Plan) o;

        return id != null ? id.equals(plan.id) : plan.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", validation=" + validation +
                ", excludedGroups=" + excludedGroups +
                ", type=" + type +
                '}';
    }

    public enum PlanType {
        /**
         * A plan for a single API.
         */
        API,

        /**
         * A plan for a bunch of APIs.
         */
        CATALOG
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
        JWT
    }

    public enum PlanValidationType {
        /**
         * Subscription is automatically validated without any human action.
         */
        AUTO,

        /**
         * Subscription requires a human validation.
         */
        MANUAL
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
        CLOSED
    }
}
