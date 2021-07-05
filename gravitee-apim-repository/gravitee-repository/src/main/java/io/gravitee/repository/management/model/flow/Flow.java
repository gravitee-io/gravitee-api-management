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
package io.gravitee.repository.management.model.flow;

import io.gravitee.common.http.HttpMethod;

import java.util.*;

public class Flow {

    /**
     * Flow technical id
     */
    private String id;
    /**
     * The type of reference the flow is attached to (for now, should be ORGANIZATION).
     */
    private FlowReferenceType referenceType;
    /**
     * The id of the reference the flow is attached to (for now, should be the organization id).
     */
    private String referenceId;
    /**
     * Flow name
     */
    private String name;
    /**
     * Flow pre steps
     */
    private List<FlowStep> pre = new ArrayList<>();
    /**
     * Flow post steps
     */
    private List<FlowStep> post = new ArrayList<>();
    /**
     * Flow state
     */
    private boolean enabled;
    /**
     * Condition attached to the Flow
     */
    private String condition;
    /**
     * Flow created date
     */
    private Date createdAt;
    /**
     * Flow updated date
     */
    private Date updatedAt;

    /**
     * Path operator
     */
    private String path;

    /**
     * Flow operator
     */
    private FlowOperator operator;

    /**
     * Http methods
     */
    private Set<HttpMethod> methods;

    /**
     * Flow order
     */
    private Integer order;

    /**
     * Flow consumers
     */
    private List<FlowConsumer> consumers;

    public Flow() {
    }

    public Flow(Flow other) {
        this.id = other.id;
        this.condition = other.condition;
        this.consumers = other.consumers;
        this.createdAt = other.createdAt;
        this.enabled = other.enabled;
        this.methods = other.methods;
        this.name = other.name;
        this.order = other.order;
        this.path = other.path;
        this.operator = other.operator;
        this.post = other.post;
        this.pre = other.pre;
        this.referenceId = other.referenceId;
        this.referenceType = other.referenceType;
        this.updatedAt = other.updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FlowReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(FlowReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FlowStep> getPre() {
        return pre;
    }

    public void setPre(List<FlowStep> pre) {
        this.pre = pre;
    }

    public List<FlowStep> getPost() {
        return post;
    }

    public void setPost(List<FlowStep> post) {
        this.post = post;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public FlowOperator getOperator() {
        return operator;
    }

    public void setOperator(FlowOperator operator) {
        this.operator = operator;
    }

    public Set<HttpMethod> getMethods() {
        return methods;
    }

    public void setMethods(Set<HttpMethod> methods) {
        this.methods = methods;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<FlowConsumer> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<FlowConsumer> consumers) {
        this.consumers = consumers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flow flow = (Flow) o;
        return Objects.equals(id, flow.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Flow{" +
            "id='" + id + '\'' +
            ", referenceType=" + referenceType +
            ", referenceId='" + referenceId + '\'' +
            ", name='" + name + '\'' +
            ", pre=" + pre +
            ", post=" + post +
            ", enabled=" + enabled +
            ", condition='" + condition + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", path='" + path + '\'' +
            ", operator=" + operator +
            ", methods=" + methods +
            ", order=" + order +
            ", consumers=" + consumers +
            '}';
    }
}
