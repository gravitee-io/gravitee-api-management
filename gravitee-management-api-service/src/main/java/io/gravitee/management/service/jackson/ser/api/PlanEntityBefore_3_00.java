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
package io.gravitee.management.service.jackson.ser.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.gravitee.definition.model.Path;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.PlanSecurityType;
import io.gravitee.management.model.PlanStatus;
import io.gravitee.management.model.PlanType;
import io.gravitee.management.model.PlanValidationType;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanEntityBefore_3_00 {
    private String id;
    private String name;
    private String description;
    private PlanValidationType validation;
    private PlanSecurityType security;
    private String securityDefinition;
    private PlanType type;
    private PlanStatus status;
    private String[] apis;
    private int order;
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonProperty("updated_at")
    private Date updatedAt;
    @JsonProperty("published_at")
    private Date publishedAt;
    @JsonProperty("closed_at")
    private Date closedAt;
    private Map<String, Path> paths = new HashMap<>();
    private List<String> characteristics;
    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;
    private Date needRedeployAt;
    @JsonProperty("comment_required")
    private boolean commentRequired;
    @JsonProperty("comment_message")
    private String commentMessage;
    private Set<String> tags;
    @JsonProperty("selection_rule")
private String selectionRule;

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

    public static PlanEntityBefore_3_00 fromNewPlanEntity(PlanEntity planEntity) {
        PlanEntityBefore_3_00 oldPlanEntity = new PlanEntityBefore_3_00();
        oldPlanEntity.setId(planEntity.getId());
        oldPlanEntity.setName(planEntity.getName());
        oldPlanEntity.setDescription(planEntity.getDescription());
        oldPlanEntity.setValidation(planEntity.getValidation());
        oldPlanEntity.setSecurity(planEntity.getSecurity());
        oldPlanEntity.setSecurityDefinition(planEntity.getSecurityDefinition());
        oldPlanEntity.setType(planEntity.getType());
        oldPlanEntity.setStatus(planEntity.getStatus());
        oldPlanEntity.setApis(Stream.of(planEntity.getApi()).toArray(String[]::new));
        oldPlanEntity.setOrder(planEntity.getOrder());
        oldPlanEntity.setCreatedAt(planEntity.getCreatedAt());
        oldPlanEntity.setUpdatedAt(planEntity.getUpdatedAt());
        oldPlanEntity.setPublishedAt(planEntity.getPublishedAt());
        oldPlanEntity.setClosedAt(planEntity.getClosedAt());
        oldPlanEntity.setPaths(planEntity.getPaths());
        oldPlanEntity.setCharacteristics(planEntity.getCharacteristics());
        oldPlanEntity.setExcludedGroups(planEntity.getExcludedGroups());
        oldPlanEntity.setNeedRedeployAt(planEntity.getNeedRedeployAt());
        oldPlanEntity.setCommentRequired(planEntity.isCommentRequired());
        oldPlanEntity.setCommentMessage(planEntity.getCommentMessage());
        oldPlanEntity.setTags(planEntity.getTags());
        oldPlanEntity.setSelectionRule(planEntity.getSelectionRule());
        return oldPlanEntity;
    }
    
    public String[] getApis() {
        return apis;
    }

    public void setApis(String[] apis) {
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

    public Map<String, Path> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Path> paths) {
        this.paths = paths;
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

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanEntityBefore_3_00 that = (PlanEntityBefore_3_00) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
