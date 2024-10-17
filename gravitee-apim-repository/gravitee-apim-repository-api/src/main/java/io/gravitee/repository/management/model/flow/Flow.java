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
package io.gravitee.repository.management.model.flow;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import java.util.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
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
     * Flow state
     */
    private boolean enabled;

    /**
     * Condition attached to the Flow
     */
    @Deprecated
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
     * Flow pre steps
     */
    @Deprecated
    private List<FlowStep> pre = new ArrayList<>();

    /**
     * Flow post steps
     */
    @Deprecated
    private List<FlowStep> post = new ArrayList<>();

    /**
     * Flow request steps
     */
    private List<FlowStep> request = new ArrayList<>();
    /**
     * Flow response steps
     */
    private List<FlowStep> response = new ArrayList<>();
    /**
     * Flow subscribe steps
     */
    private List<FlowStep> subscribe = new ArrayList<>();
    /**
     * Flow publish steps
     */
    private List<FlowStep> publish = new ArrayList<>();
    /**
     * Flow interact steps
     */
    private List<FlowStep> interact = new ArrayList<>();
    /**
     * Flow connect steps
     */
    private List<FlowStep> connect = new ArrayList<>();

    /**
     * Path operator
     */
    @Deprecated
    private String path;

    /**
     * Flow operator
     */
    @Deprecated
    private FlowOperator operator;

    /**
     * Http methods
     */
    @Deprecated
    private Set<HttpMethod> methods;

    /**
     * Flow order
     */
    @Getter(AccessLevel.NONE)
    private Integer order;

    /**
     * Flow consumers
     */
    @Deprecated
    private List<FlowConsumer> consumers;

    /**
     * Flow tags
     */
    private Set<String> tags;

    /**
     * Flow selectors
     */
    private List<FlowSelector> selectors = new ArrayList<>();

    public Flow(Flow other) {
        this.id = other.id;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.referenceId = other.referenceId;
        this.referenceType = other.referenceType;
        this.enabled = other.enabled;
        this.name = other.name;
        this.order = other.order;
        this.post = other.post;
        this.pre = other.pre;
        this.request = other.request;
        this.response = other.response;
        this.subscribe = other.subscribe;
        this.publish = other.publish;
        this.path = other.path;
        this.operator = other.operator;
        this.methods = other.methods;
        this.tags = other.tags;
        this.consumers = other.consumers;

        this.condition = other.condition;
        this.selectors = other.selectors;
    }

    public int getOrder() {
        return order;
    }

    public Flow setOrder(final int order) {
        this.order = order;
        return this;
    }
}
