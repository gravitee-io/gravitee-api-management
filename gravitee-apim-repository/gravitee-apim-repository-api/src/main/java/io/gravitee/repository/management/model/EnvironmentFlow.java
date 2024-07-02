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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@With
public class EnvironmentFlow {

    /**
     * The environment flow ID.
     */
    private String id;
    /**
     * The ID of the environment the environment flow is attached to
     */
    private String environmentId;
    /**
     * The environment flow crossId uniquely identifies an environment flow across environments.
     * Apis promoted between environments will share the same crossId.
     */
    private String crossId;
    /**
     * The environment flow name.
     */
    private String name;
    /**
     * the environment flow description.
     */
    private String description;
    /**
     * The environment flow version.
     */
    private Integer version;
    /**
     * The environment flow JSON definition
     */
    private String definition;
    /**
     * The api deployment date
     */
    private Date deployedAt;
    /**
     * The Api creation date
     */
    private Date createdAt;
    /**
     * The Api last updated date
     */
    private Date updatedAt;
    /**
     * The current runtime life cycle state.
     */
    private EnvironmentFlowLifecycleState lifecycleState;

    public EnvironmentFlow(EnvironmentFlow cloned) {
        this.id = cloned.id;
        this.crossId = cloned.crossId;
        this.environmentId = cloned.environmentId;
        this.name = cloned.name;
        this.description = cloned.description;
        this.version = cloned.version;
        this.definition = cloned.definition;
        this.deployedAt = cloned.deployedAt;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.lifecycleState = cloned.lifecycleState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvironmentFlow api = (EnvironmentFlow) o;
        return Objects.equals(id, api.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum AuditEvent implements Audit.AuditEvent {
        ENVIRONMENT_FLOW_CREATED,
        ENVIRONMENT_FLOW_UPDATED,
        ENVIRONMENT_FLOW_DELETED,
    }
}
