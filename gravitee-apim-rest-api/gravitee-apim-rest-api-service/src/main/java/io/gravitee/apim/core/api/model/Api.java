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
package io.gravitee.apim.core.api.model;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Api {

    /**
     * Indicates that this api comes from Gravitee Management Console.
     */
    public static final String ORIGIN_MANAGEMENT = "management";
    /**
     * Mode indicating the api is fully managed by the origin and so, only the origin should be able to manage the api.
     */
    public static final String MODE_FULLY_MANAGED = "fully_managed";

    /**
     * The api ID.
     */
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
     * The origin of the api (management, kubernetes, ...). Default is {@link io.gravitee.repository.management.model.Api#ORIGIN_MANAGEMENT}.
     */
    @Builder.Default
    private String origin = ORIGIN_MANAGEMENT;

    /**
     * How the api is managed by the origin (fully, api_definition_only, ...).
     * Default is {@link io.gravitee.repository.management.model.Api#MODE_FULLY_MANAGED}.
     */
    @Builder.Default
    private String mode = MODE_FULLY_MANAGED;

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
    @Builder.Default
    private LifecycleState lifecycleState = LifecycleState.STOPPED;

    private List<Listener> listeners;

    private List<EndpointGroup> endpointGroups;

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

    @Builder.Default
    private ApiLifecycleState apiLifecycleState = ApiLifecycleState.CREATED;

    private String background;

    public enum Visibility {
        /**
         * The entity is visible to everyone.
         */
        PUBLIC,

        /**
         * The entity is visible only from its members.
         */
        PRIVATE,
    }

    public enum LifecycleState {
        STARTED,
        STOPPED,
    }

    public enum ApiLifecycleState {
        CREATED,
        PUBLISHED,
        UNPUBLISHED,
        DEPRECATED,
        ARCHIVED,
    }
}
