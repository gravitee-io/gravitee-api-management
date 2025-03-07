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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.Analytics;
import io.gravitee.rest.api.management.v2.rest.model.ApiLifecycleState;
import io.gravitee.rest.api.management.v2.rest.model.ApiServices;
import io.gravitee.rest.api.management.v2.rest.model.ApiServicesV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ApiWorkflowState;
import io.gravitee.rest.api.management.v2.rest.model.BaseApi;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionContext;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.ExecutionMode;
import io.gravitee.rest.api.management.v2.rest.model.FlowMode;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.Proxy;
import io.gravitee.rest.api.management.v2.rest.model.ResponseTemplate;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Visibility;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ApiFixtures {

    private ApiFixtures() {}

    private static final Supplier<DefinitionContext> BASE_DEFINITION_CONTEXT = () ->
        new DefinitionContext().origin(DefinitionContext.OriginEnum.MANAGEMENT).mode(DefinitionContext.ModeEnum.FULLY_MANAGED);

    private static final Supplier<ApiV4> BASE_API_V4 = () ->
        (ApiV4) new ApiV4()
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
            .flows(List.of(FlowFixtures.aFlowHttpV4()))
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
            .definitionContext(BASE_DEFINITION_CONTEXT.get())
            .workflowState(ApiWorkflowState.REVIEW_OK)
            .responseTemplates(Map.of("key", new HashMap<>()))
            .resources(List.of(ResourceFixtures.aResource()))
            .properties(List.of(PropertyFixtures.aProperty()));

    private static final Supplier<UpdateApiV2> BASE_UPDATE_API_V2 = () ->
        (UpdateApiV2) new UpdateApiV2()
            .executionMode(ExecutionMode.V4_EMULATION_ENGINE)
            .proxy(new Proxy())
            .flowMode(FlowMode.DEFAULT)
            .flows(List.of(FlowFixtures.aFlowV2()))
            .pathMappings(List.of("path-mapping1", "path-mapping2"))
            .services(new ApiServicesV2())
            .executionMode(ExecutionMode.V4_EMULATION_ENGINE)
            .apiVersion("v1")
            .definitionVersion(DefinitionVersion.V2)
            .name("api-name")
            .description("api-description")
            .visibility(Visibility.PUBLIC)
            .tags(List.of("my-tag1", "my-tag2"))
            .resources(List.of(ResourceFixtures.aResource()))
            .responseTemplates(Map.of("key", new HashMap<>()))
            .groups(List.of("my-group1", "my-group2"))
            .categories(List.of("my-category1", "my-category2"))
            .lifecycleState(ApiLifecycleState.CREATED)
            .disableMembershipNotifications(true);

    private static final Supplier<UpdateApiV4> BASE_UPDATE_API_V4 = () ->
        (UpdateApiV4) new UpdateApiV4()
            .type(ApiType.MESSAGE)
            .analytics(new Analytics())
            .listeners(List.of(new Listener(ListenerFixtures.aHttpListener())))
            .endpointGroups(List.of(EndpointFixtures.anEndpointGroupV4()))
            .services(new ApiServices())
            .flows(List.of(FlowFixtures.aFlowHttpV4()))
            .apiVersion("v1")
            .definitionVersion(DefinitionVersion.V4)
            .name("api-name")
            .description("api-description")
            .visibility(Visibility.PUBLIC)
            .tags(List.of("tag1", "tag2"))
            .groups(List.of("group1", "group2"))
            .labels(List.of("label1", "label2"))
            .categories(List.of("category1", "category2"))
            .resources(List.of(ResourceFixtures.aResource()))
            .properties(List.of(PropertyFixtures.aProperty()))
            .lifecycleState(ApiLifecycleState.ARCHIVED)
            .disableMembershipNotifications(true)
            .responseTemplates(Map.of("template-id", Map.of("application/json", new ResponseTemplate())));

    private static final Supplier<UpdateApiFederated> BASE_UPDATE_API_FEDERATED = () ->
        (UpdateApiFederated) new UpdateApiFederated()
            .apiVersion("1.0.0")
            .definitionVersion(DefinitionVersion.FEDERATED)
            .name("api-name")
            .description("api-description")
            .visibility(Visibility.PRIVATE)
            .tags(List.of("tag1", "tag2"))
            .groups(List.of("group1", "group2"))
            .labels(List.of("label1", "label2"))
            .categories(List.of("category1", "category2"))
            .resources(List.of(ResourceFixtures.aResource()))
            .properties(List.of(PropertyFixtures.aProperty()))
            .lifecycleState(ApiLifecycleState.CREATED);

    public static ApiV4 anApiV4() {
        return BASE_API_V4.get();
    }

    public static BaseApi aBaseApi() {
        final ApiV4 apiV4 = anApiV4();
        return new BaseApi().id(apiV4.getId()).description(apiV4.getDescription()).name(apiV4.getName());
    }

    public static UpdateApiV4 anUpdateApiV4() {
        return BASE_UPDATE_API_V4.get();
    }

    public static UpdateApiV2 anUpdateApiV2() {
        return BASE_UPDATE_API_V2.get();
    }

    public static UpdateApiFederated anUpdateApiFederated() {
        return BASE_UPDATE_API_FEDERATED.get();
    }

    public static io.gravitee.rest.api.model.api.ApiEntity aModelApiV1() {
        return ApiModelFixtures.aModelApiV1();
    }

    public static io.gravitee.rest.api.model.api.ApiEntity aModelApiV2() {
        return ApiModelFixtures.aModelApiV2();
    }

    public static ApiEntity aModelHttpApiV4() {
        return ApiModelFixtures.aModelHttpApiV4();
    }

    public static NativeApiEntity aModelNativeApiV4() {
        return ApiModelFixtures.aModelNativeApiV4();
    }
}
