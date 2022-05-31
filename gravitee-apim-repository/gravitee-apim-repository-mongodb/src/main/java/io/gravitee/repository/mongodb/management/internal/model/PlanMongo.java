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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "plans")
public class PlanMongo extends Auditable {

    @Id
    private String id;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;

    private String name;

    private String description;

    /**
     * The way to validate subscriptions
     */
    private String validation;

    private String type;

    private String status;

    private String security;

    private int order;

    /**
     * The API used by this plan.
     */
    private String api;

    /**
     * The JSON payload of all policies to apply for this plan
     */
    private String definition;

    private List<String> characteristics;

    /**
     * Plan publication date
     */
    private Date publishedAt;

    /**
     * Plan closing date
     */
    private Date closedAt;

    private List<String> excludedGroups;

    private String securityDefinition;

    private Date needRedeployAt;

    private boolean commentRequired;

    private String commentMessage;

    private String generalConditions;

    private Set<String> tags;

    private String selectionRule;

    private String flows;

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

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public void setOrder(int order) {
        this.order = order;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public List<String> getExcludedGroups() {
        return excludedGroups;
    }

    public void setExcludedGroups(List<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public String getSecurityDefinition() {
        return securityDefinition;
    }

    public void setSecurityDefinition(String securityDefinition) {
        this.securityDefinition = securityDefinition;
    }

    public Date getNeedRedeployAt() {
        return needRedeployAt;
    }

    public void setNeedRedeployAt(Date needRedeployAt) {
        this.needRedeployAt = needRedeployAt;
    }

    public boolean isCommentRequired() {
        return commentRequired;
    }

    public void setCommentRequired(boolean commentRequired) {
        this.commentRequired = commentRequired;
    }

    public String getCommentMessage() {
        return commentMessage;
    }

    public void setCommentMessage(String commentMessage) {
        this.commentMessage = commentMessage;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getSelectionRule() {
        return selectionRule;
    }

    public void setSelectionRule(String selectionRule) {
        this.selectionRule = selectionRule;
    }

    public String getGeneralConditions() {
        return generalConditions;
    }

    public void setGeneralConditions(String generalConditions) {
        this.generalConditions = generalConditions;
    }

    public String getCrossId() {
        return crossId;
    }

    public void setCrossId(String crossId) {
        this.crossId = crossId;
    }

    public String getFlows() {
        return flows;
    }

    public void setFlows(String flows) {
        this.flows = flows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanMongo planMongo = (PlanMongo) o;

        return id.equals(planMongo.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
