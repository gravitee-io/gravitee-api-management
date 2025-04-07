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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
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

    @JsonProperty(value = "paths", required = true)
    private Map<String, List<Rule>> paths = new HashMap<>();

    private List<String> characteristics;

    @NotNull
    private int order;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    private String securityDefinition;

    @JsonProperty("comment_required")
    private boolean commentRequired;

    @JsonProperty("comment_message")
    private String commentMessage;

    @JsonProperty("general_conditions")
    private String generalConditions;

    private Set<String> tags;

    @JsonProperty("selection_rule")
    private String selectionRule;

    @JsonProperty(value = "flows")
    private List<FlowV2> flows;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = HtmlSanitizer.sanitize(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = HtmlSanitizer.sanitize(description);
    }

    public PlanValidationType getValidation() {
        return validation;
    }

    public void setValidation(PlanValidationType validation) {
        this.validation = validation;
    }

    public Map<String, List<Rule>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, List<Rule>> paths) {
        this.paths = paths;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<FlowV2> getFlows() {
        return flows;
    }

    public void setFlows(List<FlowV2> flows) {
        this.flows = flows;
    }

    public String getCrossId() {
        return crossId;
    }

    public void setCrossId(String crossId) {
        this.crossId = crossId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpdatePlanEntity that = (UpdatePlanEntity) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
