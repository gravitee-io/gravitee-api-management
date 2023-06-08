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
package fixtures;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ALL")
public class ApiFixtures {

    private static final DefinitionContext.DefinitionContextBuilder BASE_DEFINITION_CONTEXT = DefinitionContext
        .builder()
        .origin(DefinitionContext.OriginEnum.MANAGEMENT)
        .mode(DefinitionContext.ModeEnum.FULLY_MANAGED);

    private static final io.gravitee.definition.model.DefinitionContext.DefinitionContextBuilder BASE_MODEL_DEFINITION_CONTEXT =
        io.gravitee.definition.model.DefinitionContext
            .builder()
            .origin(io.gravitee.definition.model.DefinitionContext.ORIGIN_MANAGEMENT)
            .mode(io.gravitee.definition.model.DefinitionContext.MODE_FULLY_MANAGED);

    private static final io.gravitee.rest.api.model.api.ApiEntity.ApiEntityBuilder BASE_MODEL_API_V1 =
        io.gravitee.rest.api.model.api.ApiEntity
            .builder()
            .graviteeDefinitionVersion(io.gravitee.definition.model.DefinitionVersion.V1.getLabel())
            .id("my-id")
            .name("my-name")
            .version("v1.0")
            .properties(PropertyFixtures.aModelPropertiesV2())
            .services(new Services())
            .resources(List.of(ResourceFixtures.aResourceEntityV2()))
            .responseTemplates(Map.of("key", new HashMap<>()))
            .updatedAt(new Date())
            .paths(Map.of("path", List.of(new Rule())));

    private static final io.gravitee.rest.api.model.api.ApiEntity.ApiEntityBuilder BASE_MODEL_API_V2 =
        io.gravitee.rest.api.model.api.ApiEntity
            .builder()
            .graviteeDefinitionVersion(io.gravitee.definition.model.DefinitionVersion.V2.getLabel())
            .id("my-id")
            .name("my-name")
            .version("v1.0")
            .properties(PropertyFixtures.aModelPropertiesV2())
            .services(new Services())
            .resources(List.of(ResourceFixtures.aResourceEntityV2()))
            .responseTemplates(Map.of("template-id", Map.of("application/json", new io.gravitee.definition.model.ResponseTemplate())))
            .updatedAt(new Date())
            .flows(List.of(FlowFixtures.aModelFlowV2()));

    private static final ApiEntity.ApiEntityBuilder BASE_MODEL_API_V4 = ApiEntity
        .builder()
        .id("my-id")
        .crossId("my-cross-id")
        .name("my-name")
        .apiVersion("v1.0")
        .definitionVersion(io.gravitee.definition.model.DefinitionVersion.V4)
        .type(io.gravitee.definition.model.v4.ApiType.PROXY)
        .deployedAt(new Date())
        .createdAt(new Date())
        .updatedAt(new Date())
        .description("my-description")
        .tags(Set.of("tag1", "tag2"))
        .listeners(
            List.of(
                ListenerFixtures.aModelHttpListener(),
                ListenerFixtures.aModelSubscriptionListener(),
                ListenerFixtures.aModelTcpListener()
            )
        )
        .endpointGroups(List.of(EndpointFixtures.aModelEndpointGroupV4()))
        .analytics(new io.gravitee.definition.model.v4.analytics.Analytics())
        .properties(List.of(PropertyFixtures.aModelPropertyV4()))
        .resources(List.of(ResourceFixtures.aResourceEntityV4()))
        .flowExecution(new FlowExecution())
        .flows(List.of(FlowFixtures.aModelFlowV4()))
        .responseTemplates(Map.of("template-id", Map.of("application/json", new io.gravitee.definition.model.ResponseTemplate())))
        .services(new io.gravitee.definition.model.v4.service.ApiServices())
        .groups(Set.of("my-group1", "my-group2"))
        .visibility(io.gravitee.rest.api.model.Visibility.PUBLIC)
        .state(Lifecycle.State.STARTED)
        .primaryOwner(PrimaryOwnerFixtures.aPrimaryOwnerEntity())
        .picture("my-picture")
        .pictureUrl("my-picture-url")
        .categories(Set.of("my-category1", "my-category2"))
        .labels(List.of("my-label1", "my-label2"))
        .definitionContext(BASE_MODEL_DEFINITION_CONTEXT.build())
        .metadata(Map.of("key", "value"))
        .lifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED)
        .workflowState(WorkflowState.REVIEW_OK)
        .disableMembershipNotifications(true)
        .background("my-background")
        .backgroundUrl("my-background-url");

    private static final ApiV4.ApiV4Builder BASE_API_V4 = ApiV4
        .builder()
        // BaseApi fields
        .id("my-api")
        .crossId("my-cross-id")
        .name("my-name")
        .description("my-description")
        .apiVersion("v1.0")
        .definitionVersion(DefinitionVersion.V4)
        .deployedAt(OffsetDateTime.now())
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .disableMembershipNotifications(true)
        .groups(List.of("my-group1", "my-group2"))
        .state(GenericApi.StateEnum.STARTED)
        .visibility(Visibility.PUBLIC)
        .labels(List.of("my-label1", "my-label2"))
        .lifecycleState(ApiLifecycleState.CREATED)
        .tags(List.of("my-tag1", "my-tag2"))
        .primaryOwner(PrimaryOwnerFixtures.aPrimaryOwner())
        .categories(List.of("my-category1", "my-category2"))
        .definitionContext(BASE_DEFINITION_CONTEXT.build())
        .workflowState(ApiWorkflowState.REVIEW_OK)
        .responseTemplates(Map.of("key", new HashMap<>()))
        .resources(List.of(ResourceFixtures.aResource()))
        .properties(List.of(PropertyFixtures.aProperty()))
        // ApiV4 specific
        .type(ApiType.PROXY)
        .listeners(
            List.of(
                new Listener(ListenerFixtures.aHttpListener()),
                new Listener(ListenerFixtures.aSubscriptionListener()),
                new Listener(ListenerFixtures.aTcpListener())
            )
        )
        .endpointGroups(List.of(EndpointFixtures.anEndpointGroupV4()))
        .services(new ApiServices())
        .analytics(new Analytics())
        .flows(List.of(FlowFixtures.aFlowV4()));

    private static final UpdateApiV2.UpdateApiV2Builder BASE_UPDATE_API_V2 = UpdateApiV2
        .builder()
        .apiVersion("v1")
        .definitionVersion(DefinitionVersion.V4)
        .name("api-name")
        .description("api-description")
        .visibility(Visibility.PUBLIC)
        .proxy(new Proxy())
        .flowMode(FlowMode.DEFAULT)
        .flows(List.of(FlowFixtures.aFlowV2()))
        .pathMappings(List.of("path-mapping1", "path-mapping2"))
        .executionMode(ExecutionMode.JUPITER);

    private static final UpdateApiV4.UpdateApiV4Builder BASE_UPDATE_API_V4 = UpdateApiV4
        .builder()
        .apiVersion("v1")
        .definitionVersion(DefinitionVersion.V4)
        .type(ApiType.MESSAGE)
        .name("api-name")
        .description("api-description")
        .visibility(Visibility.PUBLIC)
        .tags(List.of("tag1", "tag2"))
        .groups(List.of("group1", "group2"))
        .labels(List.of("label1", "label2"))
        .categories(List.of("category1", "category2"))
        .analytics(new Analytics())
        .listeners(List.of(new Listener(ListenerFixtures.aHttpListener())))
        .endpointGroups(List.of(EndpointFixtures.anEndpointGroupV4()))
        .resources(List.of(ResourceFixtures.aResource()))
        .properties(List.of(PropertyFixtures.aProperty()))
        .flows(List.of(FlowFixtures.aFlowV4()))
        .services(new ApiServices())
        .lifecycleState(ApiLifecycleState.ARCHIVED)
        .disableMembershipNotifications(true)
        .responseTemplates(Map.of("template-id", Map.of("application/json", new ResponseTemplate())));

    public static io.gravitee.rest.api.model.api.ApiEntity aModelApiV1() {
        return BASE_MODEL_API_V1.build();
    }

    public static io.gravitee.rest.api.model.api.ApiEntity aModelApiV2() {
        return BASE_MODEL_API_V2.build();
    }

    public static ApiEntity aModelApiV4() {
        return BASE_MODEL_API_V4.build();
    }

    public static ApiV4 anApiV4() {
        return BASE_API_V4.build();
    }

    public static BaseApi aBaseApi() {
        final ApiV4 apiV4 = anApiV4();
        return BaseApi.builder().id(apiV4.getId()).description(apiV4.getDescription()).name(apiV4.getName()).build();
    }

    public static GenericApiEntity aGenericApiEntity(final io.gravitee.definition.model.DefinitionVersion definitionVersion) {
        switch (definitionVersion) {
            case V1:
                return aModelApiV1();
            case V2:
                return aModelApiV2();
            case V4:
                return aModelApiV4();
            default:
                throw new IllegalArgumentException("Unsupported definition version: " + definitionVersion);
        }
    }

    public static UpdateApiV4 anUpdateApiV4() {
        return BASE_UPDATE_API_V4.build();
    }

    public static UpdateApiV2 anUpdateApiV2() {
        return BASE_UPDATE_API_V2.build();
    }
}
