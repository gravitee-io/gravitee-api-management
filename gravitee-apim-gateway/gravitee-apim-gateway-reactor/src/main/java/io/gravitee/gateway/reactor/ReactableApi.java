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
package io.gravitee.gateway.reactor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ReactableApi<T> implements Reactable, Serializable {

    protected T definition;

    private boolean enabled = true;

    private Date deployedAt;

    private String environmentId;

    private String environmentHrid;

    private String organizationId;

    private String organizationHrid;

    private DefinitionContext definitionContext = new DefinitionContext();

    private Map<String, String> deploymentProperties;

    protected ReactableApi() {}

    protected ReactableApi(T definition) {
        this.definition = definition;
    }

    @JsonIgnore
    public abstract String getApiVersion();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean enabled() {
        return isEnabled();
    }

    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentHrid() {
        return environmentHrid;
    }

    public void setEnvironmentHrid(String environmentHrid) {
        this.environmentHrid = environmentHrid;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationHrid() {
        return organizationHrid;
    }

    public void setOrganizationHrid(String organizationHrid) {
        this.organizationHrid = organizationHrid;
    }

    public DefinitionContext getDefinitionContext() {
        return definitionContext;
    }

    public void setDefinitionContext(DefinitionContext definitionContext) {
        this.definitionContext = definitionContext;
    }

    public void setDeploymentProperties(Map<String, String> deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
    }

    public Map<String, String> getDeploymentProperties() {
        return deploymentProperties;
    }

    public T getDefinition() {
        return this.definition;
    }

    @JsonIgnore
    public abstract DefinitionVersion getDefinitionVersion();

    @JsonIgnore
    public abstract Set<String> getTags();

    @JsonIgnore
    public abstract String getId();

    @JsonIgnore
    public abstract String getName();

    @JsonIgnore
    public abstract Set<String> getSubscribablePlans();

    @JsonIgnore
    public abstract Set<String> getApiKeyPlans();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactableApi<?> that = (ReactableApi<?>) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "API " + "id[" + this.getId() + "] name[" + this.getName() + "] version[" + this.getApiVersion() + ']';
    }
}
