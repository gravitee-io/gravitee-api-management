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

import io.gravitee.definition.model.v4.ApiType;
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
public class SharedPolicyGroup {

    /**
     * The shared policy group ID
     */
    private String id;
    /**
     * The ID of the environment attached
     */
    private String environmentId;
    /**
     * The ID of the organization attached
     */
    private String organizationId;
    /**
     * The shared policy group crossId uniquely identifies an shared policy group across environments.
     * Apis promoted between environments will share the same crossId. Gateways will use this crossId to identify the shared policy group
     */
    private String crossId;
    /**
     * The shared policy group name.
     */
    private String name;
    /**
     * the shared policy group description.
     */
    private String description;
    /**
     * The shared policy group version.
     */
    private Integer version;
    /**
     * Tha API type compatible with the shared policy group
     */
    private ApiType apiType;
    /**
     * The shared policy group JSON definition
     */
    private String definition;
    /**
     * Deployment date
     */
    private Date deployedAt;
    /**
     * Creation date
     */
    private Date createdAt;
    /**
     * Last updated date
     */
    private Date updatedAt;
    /**
     * The current runtime life cycle state.
     */
    private SharedPolicyGroupLifecycleState lifecycleState;

    public SharedPolicyGroup(SharedPolicyGroup cloned) {
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
        SharedPolicyGroup api = (SharedPolicyGroup) o;
        return Objects.equals(id, api.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum AuditEvent implements Audit.AuditEvent {
        SHARED_POLICY_GROUP_CREATED,
        SHARED_POLICY_GROUP_UPDATED,
        SHARED_POLICY_GROUP_DELETED,
    }
}
