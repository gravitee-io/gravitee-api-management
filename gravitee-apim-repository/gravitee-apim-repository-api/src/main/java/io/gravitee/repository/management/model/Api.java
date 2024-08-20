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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@With
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Api {

    /**
     * Indicates that this api comes Gravitee Kubernetes Operator.
     */
    public static final String ORIGIN_KUBERNETES = "kubernetes";

    /**
     * Indicates that this api comes from Gravitee Management Console.
     */
    public static final String ORIGIN_MANAGEMENT = "management";
    /**
     * Indicates that this api comes from an integration.
     */
    public static final String ORIGIN_INTEGRATION = "integration";

    /**
     * Mode indicating the api is fully managed by the origin and so, only the origin should be able to manage the api.
     */
    public static final String MODE_FULLY_MANAGED = "fully_managed";

    /**
     * Mode indicating the api is partially managed by the origin and so, only the origin should be able to manage the the api definition part of the api.
     * This includes everything regarding the definition of the apis (plans, flows, metadata, ...)
     */
    public static final String MODE_API_DEFINITION_ONLY = "api_definition_only";

    /**
     * The api ID.
     */
    @EqualsAndHashCode.Include
    private String id;

    /**
     * The ID of the environment the api is attached to
     */
    private String environmentId;
    /**
     * The api crossId uniquely identifies an API across environments.
     * Apis promoted between environments will share the same crossId.
     */
    private String crossId;
    /**
     * The api name.
     */
    private String name;
    /**
     * the api description.
     */
    private String description;
    /**
     * The api version.
     */
    private String version;
    /**
     * The origin of the api (management, kubernetes, ...). Default is {@link Api#ORIGIN_MANAGEMENT}.
     */
    private String origin = ORIGIN_MANAGEMENT;

    /**
     * If origin is kubernetes but syncFrom is management, the API will be deployed using a classic db sync
     * instead of a config map.
     */
    private String syncFrom = ORIGIN_MANAGEMENT.toUpperCase();
    /**
     * How the api is managed by the origin (fully, api_definition_only, ...).
     */
    private String mode;
    /** The id of the integration that created the API */
    private String integrationId;
    /**
     * The api definition version.
     */
    private DefinitionVersion definitionVersion;
    /**
     * The api JSON definition
     */
    private String definition;
    /**
     * The api type.
     */
    private ApiType type;
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
     * The api visibility
     */
    private Visibility visibility;
    /**
     * The current runtime life cycle state.
     */
    private LifecycleState lifecycleState;
    /**
     * The api picture
     */
    private String picture;
    /**
     * the api group, may be null
     */
    private Set<String> groups;
    /**
     * The views associated to this API
     */
    private Set<String> categories;
    /**
     */
    private List<String> labels;
    /**
     */
    private boolean disableMembershipNotifications;
    private ApiLifecycleState apiLifecycleState = ApiLifecycleState.CREATED;
    private String background;

    public Api(Api cloned) {
        this.id = cloned.id;
        this.crossId = cloned.crossId;
        this.environmentId = cloned.environmentId;
        this.name = cloned.name;
        this.description = cloned.description;
        this.origin = cloned.origin;
        this.mode = cloned.mode;
        this.version = cloned.version;
        this.type = cloned.type;
        this.definitionVersion = cloned.definitionVersion;
        this.definition = cloned.definition;
        this.deployedAt = cloned.deployedAt;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.visibility = cloned.visibility;
        this.lifecycleState = cloned.lifecycleState;
        this.picture = cloned.picture;
        this.background = cloned.background;
        this.groups = cloned.groups != null ? new HashSet<>(cloned.groups) : null;
        this.categories = cloned.categories;
        this.labels = cloned.labels;
        this.apiLifecycleState = cloned.apiLifecycleState;
        this.disableMembershipNotifications = cloned.disableMembershipNotifications;
        this.integrationId = cloned.integrationId;
        this.syncFrom = cloned.syncFrom;
    }

    public enum AuditEvent implements Audit.ApiAuditEvent {
        API_CREATED,
        API_UPDATED,
        API_DELETED,
        API_ROLLBACKED,
        API_LOGGING_ENABLED,
        API_LOGGING_DISABLED,
        API_LOGGING_UPDATED,
    }

    public boolean addGroup(String group) {
        if (groups == null) {
            groups = new HashSet<>();
        }
        return groups.add(group);
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups != null ? new HashSet<>(groups) : null;
    }
}
