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

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApiModelFixtures {

    private ApiModelFixtures() {}

    private static final io.gravitee.rest.api.model.api.ApiEntity.ApiEntityBuilder BASE_MODEL_API_V1 =
        io.gravitee.rest.api.model.api.ApiEntity
            .builder()
            .graviteeDefinitionVersion(io.gravitee.definition.model.DefinitionVersion.V1.getLabel())
            .id("my-id")
            .name("my-name")
            .version("v1.0")
            .properties(PropertyModelFixtures.aModelPropertiesV2())
            .services(new Services())
            .resources(List.of(ResourceModelFixtures.aResourceEntityV2()))
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
            .properties(PropertyModelFixtures.aModelPropertiesV2())
            .services(new Services())
            .resources(List.of(ResourceModelFixtures.aResourceEntityV2()))
            .responseTemplates(Map.of("template-id", Map.of("application/json", new io.gravitee.definition.model.ResponseTemplate())))
            .updatedAt(new Date())
            .flows(List.of(FlowModelFixtures.aModelFlowV2()));

    private static final ApiEntity.ApiEntityBuilder BASE_MODEL_API_HTTP_V4 = ApiEntity
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
        .listeners(List.of(ListenerModelFixtures.aModelHttpListener(), ListenerModelFixtures.aModelSubscriptionListener()))
        .endpointGroups(List.of(EndpointModelFixtures.aModelEndpointGroupHttpV4()))
        .analytics(new io.gravitee.definition.model.v4.analytics.Analytics())
        .properties(List.of(PropertyModelFixtures.aModelPropertyV4()))
        .resources(List.of(ResourceModelFixtures.aResourceEntityV4()))
        .flowExecution(new FlowExecution())
        .flows(List.of(FlowModelFixtures.aModelFlowHttpV4()))
        .responseTemplates(Map.of("template-id", Map.of("application/json", new io.gravitee.definition.model.ResponseTemplate())))
        .services(new io.gravitee.definition.model.v4.service.ApiServices())
        .groups(Set.of("my-group1", "my-group2"))
        .visibility(io.gravitee.rest.api.model.Visibility.PUBLIC)
        .state(Lifecycle.State.STARTED)
        .primaryOwner(PrimaryOwnerModelFixtures.aPrimaryOwnerEntity())
        .picture("my-picture")
        .pictureUrl("my-picture-url")
        .categories(Set.of("my-category1", "my-category2"))
        .labels(List.of("my-label1", "my-label2"))
        .originContext(new OriginContext.Management())
        .metadata(Map.of("key", "value"))
        .lifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED)
        .workflowState(WorkflowState.REVIEW_OK)
        .disableMembershipNotifications(true)
        .background("my-background")
        .backgroundUrl("my-background-url");

    private static final ApiEntity.ApiEntityBuilder BASE_MODEL_API_FEDERATED = ApiEntity
        .builder()
        .id("my-id")
        .name("my-name")
        .apiVersion("v1.0")
        .definitionVersion(DefinitionVersion.FEDERATED)
        .deployedAt(new Date())
        .createdAt(new Date())
        .updatedAt(new Date());

    public static io.gravitee.rest.api.model.api.ApiEntity aModelApiV1() {
        return BASE_MODEL_API_V1.build();
    }

    public static io.gravitee.rest.api.model.api.ApiEntity aModelApiV2() {
        return BASE_MODEL_API_V2.build();
    }

    public static ApiEntity aModelHttpApiV4() {
        return BASE_MODEL_API_HTTP_V4.build();
    }

    public static GenericApiEntity aGenericApiEntity(final io.gravitee.definition.model.DefinitionVersion definitionVersion) {
        return switch (definitionVersion) {
            case V1 -> aModelApiV1();
            case V2 -> aModelApiV2();
            case V4 -> aModelHttpApiV4();
            case FEDERATED -> BASE_MODEL_API_FEDERATED.build();
        };
    }
}
