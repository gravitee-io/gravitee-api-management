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
import java.util.Objects;
import java.util.Set;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractReactableApi<T> implements ReactableApi<T>, Serializable {

    protected T definition;

    @Setter
    private boolean enabled = true;

    @Setter
    private Date deployedAt;

    @Setter
    private String environmentId;

    @Setter
    private String environmentHrid;

    @Setter
    private String organizationId;

    @Setter
    private String organizationHrid;

    @Setter
    private String revision;

    private DefinitionContext definitionContext = new DefinitionContext();

    protected AbstractReactableApi() {}

    protected AbstractReactableApi(T definition) {
        this.definition = definition;
    }

    @JsonIgnore
    public abstract String getApiVersion();

    /**
     * @deprecated Use {@link #enabled()} instead.
     */
    @Deprecated(forRemoval = true)
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    public Date getDeployedAt() {
        return deployedAt;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getEnvironmentHrid() {
        return environmentHrid;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationHrid() {
        return organizationHrid;
    }

    public DefinitionContext getDefinitionContext() {
        return definitionContext;
    }

    public String getRevision() {
        return revision;
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
        return switch (o) {
            case ReactableApi<?> that -> getId().equals(that.getId());
            case null, default -> false;
        };
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
