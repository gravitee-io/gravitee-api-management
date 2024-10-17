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
package io.gravitee.repository.mongodb.management.internal.model;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.model.flow.FlowConsumer;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@EqualsAndHashCode(of = { "id" }, callSuper = false)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}flows")
public class FlowMongo extends Auditable {

    /**
     * Flow technical id
     */
    @Id
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
     * Flow state
     */
    private boolean enabled;

    /**
     * Condition attached to the Flow
     */
    @Deprecated
    private String condition;

    /**
     * Flow Path
     */
    @Deprecated
    private String path;

    /**
     * Flow operator
     */
    @Deprecated
    private FlowOperator operator;

    /**
     * Flow selectors
     */
    private List<FlowSelector> selectors = new ArrayList<>();

    /**
     * Flow Path
     */
    private int order;

    /**
     * Http methods
     */
    @Deprecated
    private Set<HttpMethod> methods;

    @Deprecated
    private List<FlowConsumer> consumers;

    private List<String> tags;

    public FlowMongo setRequest(final List<FlowStep> request) {
        this.request = request;
        return this;
    }

    public FlowMongo setResponse(final List<FlowStep> response) {
        this.response = response;
        return this;
    }

    public FlowMongo setSubscribe(final List<FlowStep> subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    public FlowMongo setPublish(final List<FlowStep> publish) {
        this.publish = publish;
        return this;
    }

    public FlowMongo setInteract(final List<FlowStep> interact) {
        this.interact = interact;
        return this;
    }

    public FlowMongo setConnect(final List<FlowStep> connect) {
        this.connect = connect;
        return this;
    }

    public FlowMongo setTags(final List<String> tags) {
        this.tags = tags;
        return this;
    }

    public FlowMongo setSelectors(final List<FlowSelector> selectors) {
        this.selectors = selectors;
        return this;
    }
}
