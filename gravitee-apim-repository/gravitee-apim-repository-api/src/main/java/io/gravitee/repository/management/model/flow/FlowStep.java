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

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowStep {

    /**
     * Step name
     */
    private String name;
    /**
     * Step policy
     */
    private String policy;
    /**
     * Step description
     */
    private String description;
    /**
     * Step policy configuration
     */
    private String configuration;
    /**
     * Step state
     */
    private boolean enabled = true;

    /**
     * Step order
     */
    private int order;

    /**
     * Condition attached to the FlowStep
     */
    private String condition;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return (
            "FlowStep{" +
            "name='" +
            name +
            '\'' +
            ", policy='" +
            policy +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", configuration='" +
            configuration +
            '\'' +
            ", enabled=" +
            enabled +
            ", order=" +
            order +
            ", condition='" +
            condition +
            '\'' +
            '}'
        );
    }
}
