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

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Api {

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

    /** Context explaining where the API comes from. */
    @Builder.Default
    private OriginContext originContext = new OriginContext.Management();

    /**
     * The api definition version.
     */
    private DefinitionVersion definitionVersion;

    private io.gravitee.definition.model.v4.Api apiDefinitionV4;
    private io.gravitee.definition.model.Api apiDefinition;
    private io.gravitee.definition.model.federation.FederatedApi federatedApiDefinition;

    /**
     * The api type.
     */
    private ApiType type;
    /**
     * The api deployment date
     */
    private ZonedDateTime deployedAt;
    /**
     * The Api creation date
     */
    private ZonedDateTime createdAt;
    /**
     * The Api last updated date
     */
    private ZonedDateTime updatedAt;

    /**
     * The api visibility
     */
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

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
     *
     */
    private List<String> labels;

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

    public boolean isDeprecated() {
        return apiLifecycleState == ApiLifecycleState.DEPRECATED;
    }

    public Set<String> getTags() {
        return switch (definitionVersion) {
            case V4 -> apiDefinitionV4.getTags();
            case V1, V2 -> apiDefinition.getTags();
            case FEDERATED -> Collections.emptySet();
        };
    }

    public Api setTag(Set<String> tags) {
        if (apiDefinitionV4 != null) {
            apiDefinitionV4.setTags(tags);
        }
        if (apiDefinition != null) {
            apiDefinition.setTags(tags);
        }
        return this;
    }

    public Api setId(String id) {
        this.id = id;
        if (apiDefinitionV4 != null) {
            apiDefinitionV4.setId(id);
        }
        if (apiDefinition != null) {
            apiDefinition.setId(id);
        }
        if (federatedApiDefinition != null) {
            federatedApiDefinition.setId(id);
        }
        return this;
    }

    public Api setApiDefinitionV4(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        this.apiDefinitionV4 = apiDefinitionV4;
        this.definitionVersion = apiDefinitionV4.getDefinitionVersion();
        return this;
    }

    public Api setApiDefinition(io.gravitee.definition.model.Api apiDefinition) {
        this.apiDefinition = apiDefinition;
        this.definitionVersion = apiDefinition.getDefinitionVersion();
        return this;
    }

    public Api setFederatedApiDefinition(io.gravitee.definition.model.federation.FederatedApi federatedApiDefinition) {
        this.federatedApiDefinition = federatedApiDefinition;
        this.definitionVersion = federatedApiDefinition.getDefinitionVersion();
        return this;
    }

    public Api setPlans(List<Plan> plans) {
        switch (definitionVersion) {
            case V4 -> apiDefinitionV4.setPlans(plans.stream().map(Plan::getPlanDefinitionV4).collect(Collectors.toList()));
            case V1, V2 -> apiDefinition.setPlans(plans.stream().map(Plan::getPlanDefinitionV2).collect(Collectors.toList()));
            case FEDERATED -> {
                // do nothing
            }
        }
        return this;
    }

    /**
     * Updates the list of properties to include dynamic properties
     *
     * @param dynamicProperties the list of dynamic properties to update the list of property
     * @return true if an update has been done, meaning the Api need to be persisted
     */
    public boolean updateDynamicProperties(List<Property> dynamicProperties) {
        if (definitionVersion != DefinitionVersion.V4) {
            return false;
        }
        final ApiProperties apiProperties = new ApiProperties(this.apiDefinitionV4.getProperties());
        final ApiProperties.DynamicPropertiesResult properties = apiProperties.updateDynamicProperties(dynamicProperties);

        this.getApiDefinitionV4().setProperties(properties.orderedProperties());

        setUpdatedAt(TimeProvider.now());

        return properties.needToUpdate();
    }

    /**
     * Verify if the API belongs to the environment of the request
     * @param envId the environment id of the request
     * @return true if the current API belongs to the environment passed as parameter
     */
    public boolean belongsToEnvironment(String envId) {
        return this.environmentId != null && this.environmentId.equals(envId);
    }

    public Api rollbackTo(io.gravitee.definition.model.v4.Api source) {
        final io.gravitee.definition.model.v4.Api currentDefinition = getApiDefinitionV4();
        return toBuilder()
            .name(source.getName())
            .version(source.getApiVersion())
            .apiDefinitionV4(
                currentDefinition
                    .toBuilder()
                    .tags(source.getTags())
                    .listeners(source.getListeners())
                    .endpointGroups(source.getEndpointGroups())
                    .analytics(source.getAnalytics())
                    .properties(source.getProperties())
                    .resources(source.getResources())
                    .failover(source.getFailover())
                    .flowExecution(source.getFlowExecution())
                    .flows(source.getFlows())
                    .responseTemplates(source.getResponseTemplates())
                    .services(source.getServices())
                    // Ignore plans from definition for API rollback
                    .plans(null)
                    .build()
            )
            .build()
            .setTag(source.getTags());
    }

    public abstract static class ApiBuilder<C extends Api, B extends ApiBuilder<C, B>> {

        public B apiDefinition(io.gravitee.definition.model.Api apiDefinition) {
            this.apiDefinition = apiDefinition;
            if (apiDefinition != null) {
                this.definitionVersion = apiDefinition.getDefinitionVersion();
            }
            return self();
        }

        public B apiDefinitionV4(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
            this.apiDefinitionV4 = apiDefinitionV4;
            if (apiDefinitionV4 != null) {
                this.definitionVersion = apiDefinitionV4.getDefinitionVersion();
            }
            return self();
        }

        public B federatedApiDefinition(io.gravitee.definition.model.federation.FederatedApi federatedApiDefinition) {
            this.federatedApiDefinition = federatedApiDefinition;
            if (federatedApiDefinition != null) {
                this.definitionVersion = federatedApiDefinition.getDefinitionVersion();
            }
            return self();
        }
    }
}
