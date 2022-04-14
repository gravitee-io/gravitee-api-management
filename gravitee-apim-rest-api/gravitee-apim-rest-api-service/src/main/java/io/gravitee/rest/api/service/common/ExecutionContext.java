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
package io.gravitee.rest.api.service.common;

import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExecutionContext {

    private final String organizationId;

    private final Optional<String> environmentId;

    public ExecutionContext(String organizationId, String environmentId) {
        this.organizationId = organizationId;
        this.environmentId = Optional.ofNullable(environmentId);
    }

    public ExecutionContext(EnvironmentEntity environment) {
        this(environment.getOrganizationId(), environment.getId());
    }

    public ExecutionContext(Environment environment) {
        this(environment.getOrganizationId(), environment.getId());
    }

    public ExecutionContext(Organization organization) {
        this(organization.getId(), null);
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getEnvironmentId() throws EnvironmentNotFoundException {
        return environmentId.orElseThrow(() -> new EnvironmentNotFoundException(null));
    }

    public boolean hasEnvironmentId() {
        return environmentId.isPresent();
    }

    public GraviteeContext.ReferenceContext getReferenceContext() {
        if (environmentId.isPresent()) {
            return new GraviteeContext.ReferenceContext(environmentId.get(), GraviteeContext.ReferenceContextType.ENVIRONMENT);
        }
        return new GraviteeContext.ReferenceContext(organizationId, GraviteeContext.ReferenceContextType.ORGANIZATION);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext that = (ExecutionContext) o;
        return Objects.equals(organizationId, that.organizationId) && Objects.equals(environmentId, that.environmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId, environmentId);
    }

    @Override
    public String toString() {
        return "ExecutionContext{" + "organizationId='" + organizationId + '\'' + ", environmentId=" + environmentId + '}';
    }
}
