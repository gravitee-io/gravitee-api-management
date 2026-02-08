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

import io.gravitee.apim.core.api.model.property.DynamicApiProperties;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@CustomLog
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
    /*
     * The API HRID
     */
    private String hrid;
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

    private ApiDefinition apiDefinitionValue;

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
    @Builder.Default
    private LifecycleState lifecycleState = LifecycleState.STOPPED;

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

    /**
     * When true, allows an application to subscribe to more than one JWT/OAuth2 plan of this API.
     * Only applies to V4 APIs. Selection rules or sharding tags should be used to avoid ambiguous plan selection.
     */
    private boolean allowMultiJwtOauth2Subscriptions;

    @Builder.Default
    private ApiLifecycleState apiLifecycleState = ApiLifecycleState.CREATED;

    private String background;

    public boolean isTcpProxy() {
        return apiDefinitionValue instanceof io.gravitee.definition.model.v4.Api v4Api && v4Api.isTcpProxy();
    }

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
        return apiDefinitionValue != null ? apiDefinitionValue.getTags() : Set.of();
    }

    public Api setTags(Set<String> tags) {
        if (apiDefinitionValue != null) {
            apiDefinitionValue.setTags(tags);
        }
        return this;
    }

    public Api setId(String id) {
        this.id = id;
        if (apiDefinitionValue != null) {
            apiDefinitionValue.setId(id);
        }
        return this;
    }

    /**
     * @deprecated use {@link #getApiDefinitionValue()} instead.
     * @return the api definition value or null.
     */
    @Deprecated
    public io.gravitee.definition.model.v4.Api getApiDefinitionHttpV4() {
        return apiDefinitionValue instanceof io.gravitee.definition.model.v4.Api api ? api : null;
    }

    /**
     * @deprecated use {@link #getApiDefinitionValue()} instead.
     * @return the api definition value or null.
     */
    @Deprecated
    public NativeApi getApiDefinitionNativeV4() {
        return apiDefinitionValue instanceof NativeApi api ? api : null;
    }

    /**
     * @deprecated use {@link #getApiDefinitionValue()} instead.
     * @return the api definition value or null.
     */
    @Deprecated
    public io.gravitee.definition.model.Api getApiDefinition() {
        return apiDefinitionValue instanceof io.gravitee.definition.model.Api api ? api : null;
    }

    public Api setApiDefinitionValue(ApiDefinition apiDefinition) {
        this.apiDefinitionValue = apiDefinition;
        this.definitionVersion = apiDefinition.getDefinitionVersion();
        return this;
    }

    public List<? extends AbstractListener<? extends AbstractEntrypoint>> getApiListeners() {
        return apiDefinitionValue instanceof AbstractApi v4ApiDefinition ? v4ApiDefinition.getListeners() : List.of();
    }

    public boolean isNative() {
        return type == ApiType.NATIVE;
    }

    /**
     * Updates the list of properties to include dynamic properties
     *
     * @param dynamicProperties the list of dynamic properties to update the list of property
     * @return true if an update has been done, meaning the Api need to be persisted
     */
    public boolean updateDynamicProperties(List<Property> dynamicProperties) {
        if (apiDefinitionValue == null) {
            return false;
        }
        var needToUpdate = apiDefinitionValue.updateDynamicProperties(props -> {
            final DynamicApiProperties apiProperties = new DynamicApiProperties(props);
            return apiProperties.updateDynamicProperties(dynamicProperties);
        });

        setUpdatedAt(TimeProvider.now());

        return needToUpdate;
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
        if (apiDefinitionValue instanceof io.gravitee.definition.model.v4.Api currentDefinition) {
            return toBuilder()
                .name(source.getName())
                .version(source.getApiVersion())
                .apiDefinitionValue(
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
                .setTags(source.getTags());
        } else {
            return this;
        }
    }

    public abstract static class ApiBuilder<C extends Api, B extends ApiBuilder<C, B>> {

        public B apiDefinitionValue(ApiDefinition apiDefinition) {
            this.apiDefinitionValue = apiDefinition;
            if (apiDefinition != null) {
                this.definitionVersion = apiDefinition.getDefinitionVersion();
            }
            return self();
        }

        public B apiDefinition(ApiDefinition apiDefinition) {
            return apiDefinitionValue(apiDefinition);
        }

        public B apiDefinitionHttpV4(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
            return apiDefinitionValue(apiDefinitionV4);
        }

        public B federatedApiDefinition(io.gravitee.definition.model.federation.FederatedApi federatedApiDefinition) {
            return apiDefinitionValue(federatedApiDefinition);
        }

        public B apiDefinitionNativeV4(io.gravitee.definition.model.v4.nativeapi.NativeApi nativeApiDefinition) {
            return apiDefinitionValue(nativeApiDefinition);
        }
    }
}
