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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.WorkflowQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiPatchNotAllowedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.json_patch.domain_service.JsonPatchDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.domain_service.json_patch.JsonMergePatchServiceImpl;
import io.gravitee.apim.infra.domain_service.json_patch.JsonPatchServiceImpl;
import io.gravitee.apim.infra.json.jackson.JsonMapperFactory;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PatchApiUseCaseTest {

    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    static final ObjectMapper OBJECT_MAPPER = new GraviteeMapper(false);

    private static final PatchApiUseCase.FlowListDeserializer FLOW_DESERIALIZER = node ->
        OBJECT_MAPPER.<List<Flow>>readerFor(OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Flow.class))
            .with(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .readValue(node);

    private static final PatchApiUseCase.FlowListSerializer FLOW_SERIALIZER = flows -> OBJECT_MAPPER.valueToTree(flows);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    WorkflowQueryServiceInMemory workflowQueryService = new WorkflowQueryServiceInMemory();
    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService = mock(ApiPrimaryOwnerDomainService.class);
    UpdateApiDomainService updateApiDomainService = mock(UpdateApiDomainService.class);
    DataEncryptor dataEncryptor = mock(DataEncryptor.class);
    PropertyDomainService propertyDomainService = new PropertyDomainService(dataEncryptor);

    PatchApiUseCase cut;

    @BeforeEach
    void setUp() throws Exception {
        when(dataEncryptor.encrypt(anyString())).thenAnswer(inv -> "enc(" + inv.getArgument(0) + ")");
        cut = new PatchApiUseCase(
            apiCrudService,
            apiPrimaryOwnerDomainService,
            updateApiDomainService,
            new JsonPatchDomainService(new JsonMergePatchServiceImpl(), new JsonPatchServiceImpl()),
            workflowQueryService,
            OBJECT_MAPPER,
            propertyDomainService,
            FLOW_DESERIALIZER,
            FLOW_SERIALIZER,
            flowCrudService
        );

        var primaryOwner = PrimaryOwnerEntity.builder().id(USER_ID).type(PrimaryOwnerEntity.Type.USER).build();
        when(apiPrimaryOwnerDomainService.getApiPrimaryOwner(any(), eq(API_ID))).thenReturn(primaryOwner);

        givenExistingApi(ApiFixtures.aProxyApiV4());
        stubUpdateV4ReturnsArgument();
        stubValidateV4ReturnsArgument();
    }

    void givenExistingApi(Api api) {
        apiCrudService.initWith(List.of(api));
    }

    void stubUpdateV4ReturnsArgument() {
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> {
            Api api = inv.getArgument(0);
            if (api != null) {
                var httpV4 = api.getApiDefinitionHttpV4();
                if (httpV4 != null && httpV4.getFlows() != null) {
                    flowCrudService.saveApiFlows(api.getId(), httpV4.getFlows());
                }
            }
            return api;
        });
    }

    void stubValidateV4ReturnsArgument() {
        when(updateApiDomainService.validateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    PatchApiUseCase.Output execute(PatchApiUseCase.PatchType type, String body, boolean dryRun) {
        return cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(type)
                .patchBody(body)
                .dryRun(dryRun)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );
    }

    static String mergePatch(String key, Object value) {
        var node = OBJECT_MAPPER.createObjectNode();
        if (value == null) {
            node.putNull(key);
        } else {
            node.set(key, OBJECT_MAPPER.valueToTree(value));
        }
        return node.toString();
    }

    static String patch(String op, String path) {
        return patchNodes(patchOp(op, path));
    }

    static String patch(String op, String path, Object value) {
        return patchNodes(patchOp(op, path, value));
    }

    static String copyPatch(String from, String path) {
        return patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "copy").put("from", from).put("path", path));
    }

    static String movePatch(String from, String path) {
        return patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "move").put("from", from).put("path", path));
    }

    static String patchNodes(ObjectNode... nodes) {
        var arr = OBJECT_MAPPER.createArrayNode();
        for (var n : nodes) {
            arr.add(n);
        }
        return arr.toString();
    }

    static ObjectNode patchOp(String op, String path) {
        return OBJECT_MAPPER.createObjectNode().put("op", op).put("path", path);
    }

    static ObjectNode patchOp(String op, String path, Object value) {
        var node = patchOp(op, path);
        if (value == null) {
            node.putNull("value");
        } else {
            node.set(
                "value",
                JsonMapperFactory.jsonBuilder().serializationInclusion(JsonInclude.Include.ALWAYS).build().valueToTree(value)
            );
        }
        return node;
    }

    static String setField(PatchApiUseCase.PatchType type, String field, Object value) {
        return switch (type) {
            case MERGE_PATCH -> mergePatch(field, value);
            case JSON_PATCH -> patch("replace", "/" + field, value);
        };
    }

    static Stream<Arguments> clearFieldVariants(String field) {
        return Stream.of(
            Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch(field, null)),
            Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/" + field, null)),
            Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/" + field))
        );
    }

    static Stream<Arguments> clearAnalyticsVariants() {
        return clearFieldVariants("analytics");
    }

    static Stream<Arguments> clearFailoverVariants() {
        return clearFieldVariants("failover");
    }

    static Stream<Arguments> clearFlowExecutionVariants() {
        return clearFieldVariants("flowExecution");
    }

    static Stream<Arguments> clearServicesVariants() {
        return clearFieldVariants("services");
    }

    static Stream<Arguments> clearResponseTemplatesVariants() {
        return clearFieldVariants("responseTemplates");
    }

    static Stream<Arguments> clearPropertiesVariants() {
        return clearFieldVariants("properties");
    }

    static Stream<Arguments> clearLabelsVariants() {
        return clearFieldVariants("labels");
    }

    static Stream<Arguments> clearCategoriesVariants() {
        return clearFieldVariants("categories");
    }

    static Stream<Arguments> clearTagsVariants() {
        return clearFieldVariants("tags");
    }

    static Stream<Arguments> clearDescriptionVariants() {
        return clearFieldVariants("description");
    }

    static Api apiWithServices(ApiServices services) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().services(services).build()).build();
    }

    static Api apiWithResponseTemplates(Map<String, Map<String, ResponseTemplate>> templates) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().responseTemplates(templates).build()).build();
    }

    static Api apiWithProperties(List<Property> properties) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().properties(properties).build()).build();
    }

    static Api apiWithFlows(List<Flow> flows) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().flows(flows).build()).build();
    }

    static Api apiWithListeners(List<Listener> listeners) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().listeners(listeners).build()).build();
    }

    static HttpListener anHttpListener(String path) {
        return HttpListener.builder()
            .paths(List.of(Path.builder().path(path).build()))
            .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
            .build();
    }

    static Map<String, Object> listenerMap(String path) {
        return Map.of(
            "type",
            "http",
            "paths",
            List.of(Map.of("path", path)),
            "entrypoints",
            List.of(Map.of("type", "http-proxy", "configuration", "{}"))
        );
    }

    static Api apiWithResources(List<Resource> resources) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().resources(resources).build()).build();
    }

    static Map<String, Object> resourceMap(String name, String type) {
        return Map.of("name", name, "type", type, "configuration", Map.of(), "enabled", true);
    }

    static Api apiWithEndpointGroups(List<EndpointGroup> groups) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().endpointGroups(groups).build()).build();
    }

    private static io.gravitee.definition.model.v4.Api httpV4Def(Api api) {
        return (io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue();
    }

    static Step aStep(String name, String policy) {
        return Step.builder().name(name).policy(policy).enabled(true).build();
    }

    static Flow aFlow(String name, List<Step> request) {
        return Flow.builder().name(name).enabled(true).request(request).build();
    }

    static Map<String, Object> stepMap(String name, String policy) {
        return Map.of("name", name, "policy", policy, "enabled", true);
    }

    static Map<String, Object> flowMap(String name, List<Map<String, Object>> request) {
        return Map.of("name", name, "enabled", true, "request", request);
    }

    static void assertNonPatchedFieldsPreserved(Api api, Api existing, String expectedName) {
        var httpV4 = httpV4Def(api);
        var originalHttpV4 = httpV4Def(existing);

        assertThat(api.getName()).isEqualTo(expectedName);
        assertThat(api.getDescription()).isEqualTo(existing.getDescription());
        assertThat(api.getVersion()).isEqualTo(existing.getVersion());
        assertThat(api.getVisibility()).isEqualTo(existing.getVisibility());
        assertThat(api.getApiLifecycleState()).isEqualTo(existing.getApiLifecycleState());
        assertThat(api.getLabels()).isEqualTo(existing.getLabels());
        assertThat(api.getCategories()).isEqualTo(existing.getCategories());
        assertThat(api.getGroups()).isEqualTo(existing.getGroups());
        assertThat(api.isDisableMembershipNotifications()).isEqualTo(existing.isDisableMembershipNotifications());
        assertThat(api.isAllowMultiJwtOauth2Subscriptions()).isEqualTo(existing.isAllowMultiJwtOauth2Subscriptions());
        assertThat(httpV4.getTags()).isEqualTo(originalHttpV4.getTags());
        assertThat(httpV4.getAnalytics()).isEqualTo(originalHttpV4.getAnalytics());
        assertThat(httpV4.getFailover()).isEqualTo(originalHttpV4.getFailover());
        assertThat(httpV4.getFlowExecution()).isEqualTo(originalHttpV4.getFlowExecution());
        assertThat(httpV4.getServices()).isEqualTo(originalHttpV4.getServices());
        assertThat(httpV4.getAllowedInApiProducts()).isEqualTo(originalHttpV4.getAllowedInApiProducts());
        assertThat(httpV4.getProperties()).isEqualTo(originalHttpV4.getProperties());
        assertThat(httpV4.getResponseTemplates()).isEqualTo(originalHttpV4.getResponseTemplates());
        assertThat(httpV4.getFlows()).isEqualTo(originalHttpV4.getFlows());
        assertThat(httpV4.getEndpointGroups()).isEqualTo(originalHttpV4.getEndpointGroups());
    }

    @Nested
    class ApiScopeValidation {

        @Test
        void non_v4_api_is_rejected() {
            givenExistingApi(ApiFixtures.aProxyApiV2());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false)).isInstanceOf(
                ApiInvalidDefinitionVersionException.class
            );
        }

        @Test
        void v4_message_api_is_rejected() {
            givenExistingApi(ApiFixtures.aMessageApiV4());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false)).isInstanceOf(
                ApiInvalidTypeException.class
            );
        }

        @Test
        void federated_api_is_rejected() {
            givenExistingApi(ApiFixtures.aFederatedApi());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false)).isInstanceOf(
                ApiInvalidDefinitionVersionException.class
            );
        }
    }

    @Nested
    class UseCaseBehavior {

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void updates_allowed_field_and_persists(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "name", "patched-name"), false);

            assertThat(output.api().getName()).isEqualTo("patched-name");
            verify(updateApiDomainService).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_returns_result_without_persistence(PatchApiUseCase.PatchType type) {
            stubValidateV4ReturnsArgument();

            var output = execute(type, setField(type, "name", "dry-run-name"), true);

            assertThat(output.api().getName()).isEqualTo("dry-run-name");
            verify(updateApiDomainService).validateV4(any(), any());
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_invokes_validation_with_patched_api(PatchApiUseCase.PatchType type) {
            stubValidateV4ReturnsArgument();

            execute(type, setField(type, "description", "dry-run-desc"), true);

            var apiCaptor = ArgumentCaptor.forClass(Api.class);
            verify(updateApiDomainService).validateV4(apiCaptor.capture(), any());
            assertThat(apiCaptor.getValue().getDescription()).isEqualTo("dry-run-desc");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_propagates_validation_failure(PatchApiUseCase.PatchType type) {
            doThrow(new ValidationDomainException("tag not allowed")).when(updateApiDomainService).validateV4(any(), any());

            assertThatThrownBy(() -> execute(type, setField(type, "tags", List.of("forbidden")), true))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("tag not allowed");
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @Test
        void update_domain_service_is_called_when_not_dry_run() {
            execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false);

            verify(updateApiDomainService).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void update_domain_service_is_not_called_when_dry_run(PatchApiUseCase.PatchType type) {
            execute(type, setField(type, "name", "new-name"), true);

            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void primary_owner_is_included_in_output(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "name", "new-name"), false);

            assertThat(output.primaryOwner()).isNotNull();
            assertThat(output.primaryOwner().id()).isEqualTo(USER_ID);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void audit_info_is_forwarded_to_update(PatchApiUseCase.PatchType type) {
            var auditInfoCaptor = ArgumentCaptor.forClass(AuditInfo.class);

            execute(type, setField(type, "name", "patched-name"), false);

            verify(updateApiDomainService).updateV4(any(), auditInfoCaptor.capture());
            var auditInfo = auditInfoCaptor.getValue();
            assertThat(auditInfo.organizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(auditInfo.environmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(auditInfo.actor().userId()).isEqualTo(USER_ID);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void validation_failure_after_patch_surfaces_as_400(PatchApiUseCase.PatchType type) {
            doThrow(new ValidationDomainException("name must not be blank", Map.of("name", "must not be blank")))
                .when(updateApiDomainService)
                .updateV4(any(), any());

            assertThatThrownBy(() -> execute(type, setField(type, "name", ""), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("name");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void preserves_every_non_patched_field(PatchApiUseCase.PatchType type) {
            var existing = apiCrudService.storage().getFirst();

            var output = execute(type, setField(type, "name", "only-name-changed"), false);

            assertNonPatchedFieldsPreserved(output.api(), existing, "only-name-changed");
        }
    }

    @Nested
    class BlockedFields {

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void state_field_is_rejected_with_hint(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "state", "STARTED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state")
                .hasMessageContaining("_start");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void definitionVersion_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "definitionVersion", "V4"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("definitionVersion");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void type_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "type", "MESSAGE"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("type");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void groups_field_is_rejected(PatchApiUseCase.PatchType type) {
            givenExistingApi(ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(List.of("g-1"))).build());

            assertThatThrownBy(() -> execute(type, setField(type, "groups", List.of("g-2")), false)).isInstanceOf(
                ValidationDomainException.class
            );
        }
    }

    @Nested
    class AllowedFields {

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void name_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "name", "new-name"), false);

            assertThat(output.api().getName()).isEqualTo("new-name");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearDescriptionVariants")
        void description_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getDescription()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void visibility_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "visibility", "PRIVATE"), false);

            assertThat(output.api().getVisibility()).isEqualTo(Api.Visibility.PRIVATE);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void invalid_visibility_value_produces_400(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "visibility", "NOT_A_VISIBILITY"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("visibility")
                .hasMessageContaining("NOT_A_VISIBILITY");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void lifecycleState_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "lifecycleState", "DEPRECATED"), false);

            assertThat(output.api().getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.DEPRECATED);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void invalid_lifecycleState_value_produces_400(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "lifecycleState", "NOT_A_LIFECYCLE_STATE"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("lifecycleState")
                .hasMessageContaining("NOT_A_LIFECYCLE_STATE");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void allowMultiJwtOauth2Subscriptions_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "allowMultiJwtOauth2Subscriptions", true), false);

            assertThat(output.api().isAllowMultiJwtOauth2Subscriptions()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void disableMembershipNotifications_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "disableMembershipNotifications", false), false);

            assertThat(output.api().isDisableMembershipNotifications()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearLabelsVariants")
        void labels_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getLabels()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearCategoriesVariants")
        void categories_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getCategories()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearTagsVariants")
        void tags_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getTags()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearAnalyticsVariants")
        void analytics_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getAnalytics()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void analytics_sampling_sets_message_sampling(PatchApiUseCase.PatchType type) {
            var base = ApiFixtures.aProxyApiV4();
            givenExistingApi(
                base
                    .toBuilder()
                    .apiDefinitionValue(httpV4Def(base).toBuilder().analytics(Analytics.builder().enabled(true).build()).build())
                    .build()
            );
            var body = switch (type) {
                case MERGE_PATCH -> mergePatch("analytics", Map.of("sampling", Map.of("type", "count", "value", "50")));
                case JSON_PATCH -> patch("add", "/analytics/sampling", Map.of("type", "count", "value", "50"));
            };

            var output = execute(type, body, false);

            var messageSampling = httpV4Def(output.api()).getAnalytics().getMessageSampling();
            assertThat(messageSampling).isNotNull();
            assertThat(messageSampling.getType()).isEqualTo(SamplingType.COUNT);
            assertThat(messageSampling.getValue()).isEqualTo("50");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearFailoverVariants")
        void failover_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getFailover()).isNull();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearFlowExecutionVariants")
        void flowExecution_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getFlowExecution()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void flowExecution_mode_is_updated(PatchApiUseCase.PatchType type) {
            var body = switch (type) {
                case MERGE_PATCH -> mergePatch("flowExecution", Map.of("mode", "BEST_MATCH"));
                case JSON_PATCH -> patch("replace", "/flowExecution/mode", "BEST_MATCH");
            };

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getFlowExecution().getMode()).isEqualTo(FlowMode.BEST_MATCH);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearServicesVariants")
        void services_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithServices(new ApiServices()));

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getServices()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void responseTemplates_map_is_replaced(PatchApiUseCase.PatchType type) {
            var value = Map.of("DEFAULT", Map.of("application/json", Map.of("statusCode", 400, "body", "patched")));

            var output = execute(type, setField(type, "responseTemplates", value), false);

            var templates = httpV4Def(output.api()).getResponseTemplates();
            assertThat(templates).containsKey("DEFAULT");
            assertThat(templates.get("DEFAULT").get("application/json").getStatusCode()).isEqualTo(400);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearResponseTemplatesVariants")
        void responseTemplates_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithResponseTemplates(Map.of("KEY", Map.of("application/json", new ResponseTemplate()))));

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getResponseTemplates()).isNull();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearPropertiesVariants")
        void properties_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var existing = new Property();
            existing.setKey("k1");
            existing.setValue("v1");
            givenExistingApi(apiWithProperties(List.of(existing)));

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getProperties()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void resources_field_is_allowed(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "resources", List.of(resourceMap("my-cache", "cache"))), false);

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
        }
    }

    @Nested
    class JsonPatchSpecific {

        @Test
        void malformed_body_produces_validation_exception() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, "not valid json", false)).isInstanceOf(
                ValidationDomainException.class
            );
        }

        @Test
        void operation_without_path_field_is_rejected() {
            var noPathOp = OBJECT_MAPPER.createObjectNode().put("op", "replace").put("value", "x");

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patchNodes(noPathOp), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("path");
        }

        @Test
        void path_without_leading_slash_produces_validation_exception() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "name", "test"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("/");
        }

        @Test
        void root_pointer_path_produces_clear_validation_error() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "", "test"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not target a specific field");
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/", "test"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not target a specific field");
        }

        @Test
        void more_than_200_operations_is_rejected() {
            var ops = new ObjectNode[201];
            for (int i = 0; i < 201; i++) {
                ops[i] = patchOp("replace", "/name", "name-" + i);
            }

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patchNodes(ops), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("exceeds maximum");
        }

        @Test
        void move_op_without_from_field_is_rejected() {
            assertThatThrownBy(() ->
                execute(
                    PatchApiUseCase.PatchType.JSON_PATCH,
                    patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "move").put("path", "/name")),
                    false
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("missing");
        }

        @Test
        void copy_op_without_from_field_is_rejected() {
            assertThatThrownBy(() ->
                execute(
                    PatchApiUseCase.PatchType.JSON_PATCH,
                    patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "copy").put("path", "/name")),
                    false
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("missing");
        }

        @Test
        void move_op_with_non_string_from_field_is_rejected() {
            assertThatThrownBy(() ->
                execute(
                    PatchApiUseCase.PatchType.JSON_PATCH,
                    patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "move").put("path", "/name").put("from", 123)),
                    false
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must be a string");
        }

        @Test
        void move_op_with_disallowed_from_field_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, movePatch("/state", "/name"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void copy_op_with_disallowed_from_field_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, copyPatch("/state", "/name"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void move_op_with_disallowed_destination_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, movePatch("/name", "/state"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void copy_op_with_disallowed_destination_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, copyPatch("/name", "/state"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void disallowed_field_via_path_segment_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/state/running", "x"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void test_op_passes_when_assertion_matches() {
            var existing = ApiFixtures.aProxyApiV4();
            givenExistingApi(existing);

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patchNodes(patchOp("test", "/name", existing.getName()), patchOp("replace", "/name", "new-name")),
                false
            );

            assertThat(output.api().getName()).isEqualTo("new-name");
        }

        @Test
        void test_op_throws_when_assertion_fails() {
            assertThatThrownBy(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("test", "/name", "wrong-value"), false)
            ).isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void test_op_on_blocked_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("test", "/state", "STOPPED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void add_op_updates_field_like_replace() {
            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/name", "new-name-via-add"), false);

            assertThat(output.api().getName()).isEqualTo("new-name-via-add");
        }

        @Test
        void copy_op_copies_value_to_target_path() {
            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, copyPatch("/description", "/name"), false);

            assertThat(output.api().getName()).isEqualTo("api-description");
            assertThat(output.api().getDescription()).isEqualTo("api-description");
        }

        @Test
        void remove_op_on_name_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/name"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("name");
        }

        @Test
        void remove_op_on_apiVersion_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/apiVersion"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("apiVersion");
        }

        @Test
        void add_op_on_blocked_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/state", "STARTED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void add_op_on_state_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/state", "STARTED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state")
                .hasMessageContaining("_start");
        }

        @Test
        void null_flowExecution_is_preserved_when_patching_other_fields() {
            var base = ApiFixtures.aProxyApiV4();
            givenExistingApi(base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().flowExecution(null).build()).build());

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/name", "patched-name"), false);

            assertThat(httpV4Def(output.api()).getFlowExecution()).isNull();
        }

        @Test
        void replace_null_on_null_responseTemplates_passes_null_to_domain_service() {
            givenExistingApi(apiWithResponseTemplates(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/responseTemplates", null), false);

            assertThat(httpV4Def(output.api()).getResponseTemplates()).isNull();
        }

        @Test
        void remove_op_on_null_responseTemplates_sets_responseTemplates_to_null() {
            givenExistingApi(apiWithResponseTemplates(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/responseTemplates"), false);

            assertThat(httpV4Def(output.api()).getResponseTemplates()).isNull();
        }

        @Test
        void add_op_on_null_responseTemplates_sets_them() {
            givenExistingApi(apiWithResponseTemplates(null));
            var value = Map.of("DEFAULT", Map.of("application/json", Map.of("statusCode", 201, "body", "created")));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/responseTemplates", value), false);

            var templates = httpV4Def(output.api()).getResponseTemplates();
            assertThat(templates).containsKey("DEFAULT");
            assertThat(templates.get("DEFAULT").get("application/json").getStatusCode()).isEqualTo(201);
        }

        @Test
        void add_null_inner_map_for_responseTemplates_key_does_not_throw_npe() {
            givenExistingApi(apiWithResponseTemplates(null));
            var value = new HashMap<String, Object>();
            value.put("DEFAULT", null);

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/responseTemplates", value), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void any_patch_on_api_with_null_inner_responseTemplates_map_in_storage_does_not_throw_npe() {
            var templates = new HashMap<String, Map<String, ResponseTemplate>>();
            templates.put("DEFAULT", null);
            givenExistingApi(apiWithResponseTemplates(templates));

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/name", "patched-name"), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void replace_responseTemplates_with_null_leaf_template_does_not_throw_npe() {
            var innerMap = new HashMap<String, Object>();
            innerMap.put("application/json", null);
            var value = new HashMap<String, Object>();
            value.put("DEFAULT", innerMap);

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/responseTemplates", value), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void any_patch_on_api_with_null_leaf_responseTemplate_in_storage_does_not_throw_npe() {
            var innerMap = new HashMap<String, ResponseTemplate>();
            innerMap.put("application/json", null);
            var templates = new HashMap<String, Map<String, ResponseTemplate>>();
            templates.put("DEFAULT", innerMap);
            givenExistingApi(apiWithResponseTemplates(templates));

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/name", "patched-name"), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void replace_null_on_null_services_passes_null_to_domain_service() {
            givenExistingApi(apiWithServices(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/services", null), false);

            assertThat(httpV4Def(output.api()).getServices()).isNull();
        }

        @Test
        void remove_op_on_null_services_sets_services_to_null() {
            givenExistingApi(apiWithServices(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/services"), false);

            assertThat(httpV4Def(output.api()).getServices()).isNull();
        }

        @Test
        void replace_on_null_properties_sets_properties() {
            givenExistingApi(apiWithProperties(null));

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patch("replace", "/properties", List.of(Map.of("key", "k", "value", "v"))),
                false
            );

            assertThat(httpV4Def(output.api()).getProperties()).hasSize(1);
        }

        @Test
        void replace_null_on_null_properties_is_a_noop() {
            givenExistingApi(apiWithProperties(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/properties", null), false);

            assertThat(httpV4Def(output.api()).getProperties()).isNull();
        }

        @Test
        void remove_op_on_null_categories_clears_categories() {
            givenExistingApi(ApiFixtures.aProxyApiV4().toBuilder().categories(null).build());

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/categories"), false);

            assertThat(output.api().getCategories()).isEmpty();
        }

        static Stream<Arguments> deepPathUnderResourceConfigurationCases() {
            return Stream.of(
                Arguments.of(patch("replace", "/resources/0/configuration/someField", "value"), "/resources/0/configuration/someField"),
                Arguments.of(patch("replace", "/resources/0/configuration/a/b", "value"), "/resources/0/configuration/a/b"),
                Arguments.of(patch("remove", "/resources/0/configuration/someField"), "/resources/0/configuration/someField"),
                Arguments.of(movePatch("/resources/0/configuration/key", "/name"), "/resources/0/configuration/key"),
                Arguments.of(copyPatch("/resources/0/configuration/key", "/name"), "/resources/0/configuration/key"),
                Arguments.of(patch("add", "/resources/-/configuration/someKey", "value"), "/resources/-/configuration/someKey")
            );
        }

        @ParameterizedTest
        @MethodSource("deepPathUnderResourceConfigurationCases")
        void deep_path_under_resource_configuration_is_rejected(String patchJson, String expectedPath) {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patchJson, false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining(expectedPath);
        }

        static Stream<Arguments> deepPathUnderEndpointGroupsConfigurationCases() {
            return Stream.of(
                Arguments.of(
                    patch("replace", "/endpointGroups/0/endpoints/0/configuration/someField", "value"),
                    "/endpointGroups/0/endpoints/0/configuration/someField"
                ),
                Arguments.of(
                    patch("replace", "/endpointGroups/0/endpoints/0/configuration/a/b", "value"),
                    "/endpointGroups/0/endpoints/0/configuration/a/b"
                ),
                Arguments.of(
                    patch("remove", "/endpointGroups/0/endpoints/0/configuration/someField"),
                    "/endpointGroups/0/endpoints/0/configuration/someField"
                ),
                Arguments.of(
                    movePatch("/endpointGroups/0/endpoints/0/configuration/key", "/name"),
                    "/endpointGroups/0/endpoints/0/configuration/key"
                ),
                Arguments.of(
                    copyPatch("/endpointGroups/0/endpoints/0/configuration/key", "/name"),
                    "/endpointGroups/0/endpoints/0/configuration/key"
                ),
                Arguments.of(
                    patch("add", "/endpointGroups/-/endpoints/-/configuration/someKey", "value"),
                    "/endpointGroups/-/endpoints/-/configuration/someKey"
                ),
                Arguments.of(
                    patch("replace", "/endpointGroups/0/endpoints/0/sharedConfigurationOverride/someField", "value"),
                    "/endpointGroups/0/endpoints/0/sharedConfigurationOverride/someField"
                ),
                Arguments.of(
                    patch("remove", "/endpointGroups/0/endpoints/0/sharedConfigurationOverride/someField"),
                    "/endpointGroups/0/endpoints/0/sharedConfigurationOverride/someField"
                ),
                Arguments.of(
                    movePatch("/endpointGroups/0/endpoints/0/sharedConfigurationOverride/key", "/name"),
                    "/endpointGroups/0/endpoints/0/sharedConfigurationOverride/key"
                ),
                Arguments.of(
                    copyPatch("/endpointGroups/0/endpoints/0/sharedConfigurationOverride/key", "/name"),
                    "/endpointGroups/0/endpoints/0/sharedConfigurationOverride/key"
                ),
                Arguments.of(
                    patch("replace", "/endpointGroups/0/sharedConfiguration/someField", "value"),
                    "/endpointGroups/0/sharedConfiguration/someField"
                ),
                Arguments.of(
                    patch("remove", "/endpointGroups/0/sharedConfiguration/someField"),
                    "/endpointGroups/0/sharedConfiguration/someField"
                ),
                Arguments.of(movePatch("/endpointGroups/0/sharedConfiguration/key", "/name"), "/endpointGroups/0/sharedConfiguration/key"),
                Arguments.of(copyPatch("/endpointGroups/0/sharedConfiguration/key", "/name"), "/endpointGroups/0/sharedConfiguration/key"),
                Arguments.of(
                    patch("replace", "/endpointGroups/0/services/healthCheck/configuration/timeout", "5000"),
                    "/endpointGroups/0/services/healthCheck/configuration/timeout"
                ),
                Arguments.of(
                    patch("replace", "/endpointGroups/0/endpoints/0/services/healthCheck/configuration/timeout", "5000"),
                    "/endpointGroups/0/endpoints/0/services/healthCheck/configuration/timeout"
                )
            );
        }

        @ParameterizedTest
        @MethodSource("deepPathUnderEndpointGroupsConfigurationCases")
        void deep_path_under_endpoint_groups_configuration_is_rejected(String patchJson, String expectedPath) {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patchJson, false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining(expectedPath);
        }

        @Test
        void json_patch_exact_configuration_path_is_accepted() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patch("replace", "/resources/0/configuration", Map.of("ttl", 300)),
                false
            );

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
        }
    }

    @Nested
    class MergePatchSpecific {

        @Test
        void unknown_field_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("unknownField", "value"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("unknownField");
        }

        @Test
        void validation_failure_during_dry_run_still_surfaces() {
            assertThatThrownBy(() ->
                execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("responseTemplates", 42), true)
            ).isInstanceOf(ValidationDomainException.class);
        }
    }

    @Nested
    class PatchableResponseTemplateFactory {

        @Test
        void from_returns_null_when_domain_is_null() {
            assertThat(PatchApiUseCase.PatchableResponseTemplate.from(null)).isNull();
        }
    }

    @Nested
    class PatchablePropertyFactory {

        @Test
        void from_returns_null_when_property_is_null() {
            assertThat(PatchApiUseCase.PatchableProperty.from(null)).isNull();
        }

        @Test
        void fromList_returns_null_when_list_is_null() {
            assertThat(PatchApiUseCase.PatchableProperty.fromList(null)).isNull();
        }
    }

    @Nested
    class PropertiesEncryption {

        @Test
        void merge_patch_adding_plaintext_property_returns_it_in_real_update_response() {
            var body = mergePatch("properties", List.of(Map.of("key", "k1", "value", "v1")));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, body, false);

            var properties = httpV4Def(output.api()).getProperties();
            assertThat(properties).hasSize(1);
            assertThat(properties.getFirst().getKey()).isEqualTo("k1");
            assertThat(properties.getFirst().getValue()).isEqualTo("v1");
        }

        @Test
        void merge_patch_null_clears_existing_properties_in_real_update_response() {
            var existing = new Property();
            existing.setKey("old");
            existing.setValue("secret");
            existing.setEncrypted(true);
            givenExistingApi(apiWithProperties(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("properties", null), false);

            assertThat(httpV4Def(output.api()).getProperties()).isNull();
        }

        @Test
        void encryptable_property_is_returned_encrypted_on_dry_run() {
            stubValidateV4ReturnsArgument();
            var body = mergePatch("properties", List.of(Map.of("key", "secret-key", "value", "plaintext", "encryptable", true)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, body, true);

            var properties = httpV4Def(output.api()).getProperties();
            assertThat(properties).hasSize(1);
            assertThat(properties.getFirst().getKey()).isEqualTo("secret-key");
            assertThat(properties.getFirst().isEncrypted()).isTrue();
            assertThat(properties.getFirst().getValue()).isEqualTo("enc(plaintext)");
        }

        @Test
        void encryptable_property_is_returned_encrypted_on_real_update() {
            var body = mergePatch("properties", List.of(Map.of("key", "secret-key", "value", "plaintext", "encryptable", true)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, body, false);

            var properties = httpV4Def(output.api()).getProperties();
            assertThat(properties).hasSize(1);
            assertThat(properties.getFirst().getKey()).isEqualTo("secret-key");
            assertThat(properties.getFirst().isEncrypted()).isTrue();
            assertThat(properties.getFirst().getValue()).isEqualTo("enc(plaintext)");
        }

        @Test
        void merge_patch_adding_plaintext_property_returns_it_in_dry_run_response() {
            stubValidateV4ReturnsArgument();
            var body = mergePatch("properties", List.of(Map.of("key", "k1", "value", "v1")));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, body, true);

            var properties = httpV4Def(output.api()).getProperties();
            assertThat(properties).hasSize(1);
            assertThat(properties.getFirst().getKey()).isEqualTo("k1");
            assertThat(properties.getFirst().getValue()).isEqualTo("v1");
        }

        @Test
        void merge_patch_null_clears_existing_properties_in_dry_run_response() {
            stubValidateV4ReturnsArgument();
            var existing = new Property();
            existing.setKey("old");
            existing.setValue("secret");
            existing.setEncrypted(true);
            givenExistingApi(apiWithProperties(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("properties", null), true);

            assertThat(httpV4Def(output.api()).getProperties()).isNull();
        }

        @Test
        void patching_unrelated_field_does_not_re_encrypt_already_encrypted_property() throws Exception {
            var encrypted = new Property();
            encrypted.setKey("secret-key");
            encrypted.setValue("already-encrypted-value");
            encrypted.setEncrypted(true);
            givenExistingApi(apiWithProperties(List.of(encrypted)));

            execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false);

            verify(dataEncryptor, never()).encrypt(any());
        }

        @Test
        void json_patch_add_op_with_encryptable_property_encrypts_value() {
            var body = patch("add", "/properties", List.of(Map.of("key", "secret-key", "value", "plaintext", "encryptable", true)));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, body, false);

            var properties = httpV4Def(output.api()).getProperties();
            assertThat(properties).hasSize(1);
            assertThat(properties.getFirst().getKey()).isEqualTo("secret-key");
            assertThat(properties.getFirst().isEncrypted()).isTrue();
            assertThat(properties.getFirst().getValue()).isEqualTo("enc(plaintext)");
        }
    }

    @Nested
    class FlowsResolution {

        @Test
        void merge_patch_with_flows_null_clears_flows_on_rebuilt_definition() {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", null), false);

            assertThat(httpV4Def(output.api()).getFlows()).isNull();
        }

        @Test
        void merge_patch_with_populated_flows_replaces_existing_list_preserving_order() {
            givenExistingApi(apiWithFlows(List.of(aFlow("old", List.of(aStep("s", "p"))))));
            var supplied = List.of(
                flowMap("first", List.of(stepMap("s1", "policy-a"))),
                flowMap("second", List.of(stepMap("s2", "policy-b")))
            );

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", supplied), false);

            var flows = httpV4Def(output.api()).getFlows();
            assertThat(flows).hasSize(2);
            assertThat(flows.getFirst().getName()).isEqualTo("first");
            assertThat(flows.get(1).getName()).isEqualTo("second");
            assertThat(flows.getFirst().getRequest().getFirst().getPolicy()).isEqualTo("policy-a");
        }

        @Test
        void json_patch_remove_flows_clears_flows_on_rebuilt_definition() {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/flows"), false);

            assertThat(httpV4Def(output.api()).getFlows()).isNull();
        }

        @Test
        void json_patch_replace_then_remove_flows_clears_flows() {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))))));
            var supplied = List.of(flowMap("brand-new", List.of(stepMap("s1", "policy-a"))));

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patchNodes(patchOp("replace", "/flows", supplied), patchOp("remove", "/flows")),
                false
            );

            assertThat(httpV4Def(output.api()).getFlows()).isNull();
        }

        @Test
        void json_patch_replace_flows_with_fresh_array_replaces_existing_list() {
            givenExistingApi(apiWithFlows(List.of(aFlow("old", List.of(aStep("s", "p"))))));
            var supplied = List.of(flowMap("brand-new", List.of(stepMap("s1", "policy-a"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/flows", supplied), false);

            var flows = httpV4Def(output.api()).getFlows();
            assertThat(flows).hasSize(1);
            assertThat(flows.getFirst().getName()).isEqualTo("brand-new");
        }

        @Test
        void patch_not_addressing_flows_leaves_existing_list_intact() {
            var existing = List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))), aFlow("f2", List.of(aStep("s2", "policy-b"))));
            givenExistingApi(apiWithFlows(existing));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "renamed"), false);

            assertThat(httpV4Def(output.api()).getFlows()).isEqualTo(existing);
        }

        @Test
        void blocked_field_combined_with_allowed_flows_patch_is_still_rejected() {
            var body = OBJECT_MAPPER.createObjectNode();
            body.set("flows", OBJECT_MAPPER.valueToTree(List.of(flowMap("f", List.of()))));
            body.set("state", OBJECT_MAPPER.valueToTree("STARTED"));

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, body.toString(), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void merge_patch_with_empty_flows_array_clears_flows() {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", List.of()), false);

            assertThat(httpV4Def(output.api()).getFlows()).isEmpty();
        }

        @Test
        void selector_polymorphism_round_trips_through_patch_view() {
            var http = HttpSelector.builder().path("/foo").build();
            var flowWithSelector = Flow.builder().name("f1").selectors(List.of(http)).request(List.of(aStep("s1", "policy-a"))).build();
            givenExistingApi(apiWithFlows(List.of(flowWithSelector)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "renamed"), false);

            var flows = httpV4Def(output.api()).getFlows();
            assertThat(flows).hasSize(1);
            assertThat(flows.getFirst().getSelectors()).hasSize(1);
            assertThat(flows.getFirst().getSelectors().getFirst()).isInstanceOf(HttpSelector.class);
            assertThat(((HttpSelector) flows.getFirst().getSelectors().getFirst()).getPath()).isEqualTo("/foo");
        }

        static Stream<Arguments> reorderFlowsVariants() {
            var reordered = List.of(flowMap("second", List.of(stepMap("s2", "p2"))), flowMap("first", List.of(stepMap("s1", "p1"))));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", reordered)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/flows", reordered))
            );
        }

        @ParameterizedTest
        @MethodSource("reorderFlowsVariants")
        void reordering_flows_reflects_new_order(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(
                apiWithFlows(List.of(aFlow("first", List.of(aStep("s1", "p1"))), aFlow("second", List.of(aStep("s2", "p2")))))
            );

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getFlows()).extracting(Flow::getName).containsExactly("second", "first");
        }

        static Stream<Arguments> addStepVariants() {
            var twoSteps = List.of(flowMap("f1", List.of(stepMap("s1", "p1"), stepMap("s2", "p2"))));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", twoSteps)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/flows/0/request/-", stepMap("s2", "p2")))
            );
        }

        @ParameterizedTest
        @MethodSource("addStepVariants")
        void adding_a_step_places_it_at_the_requested_index(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "p1"))))));

            var output = execute(type, body, false);

            var steps = httpV4Def(output.api()).getFlows().getFirst().getRequest();
            assertThat(steps).extracting(Step::getName).containsExactly("s1", "s2");
        }

        static Stream<Arguments> removeStepVariants() {
            var oneStep = List.of(flowMap("f1", List.of(stepMap("s1", "p1"))));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", oneStep)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/flows/0/request/1"))
            );
        }

        @ParameterizedTest
        @MethodSource("removeStepVariants")
        void removing_a_step_removes_it_from_the_list(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "p1"), aStep("s2", "p2"))))));

            var output = execute(type, body, false);

            var steps = httpV4Def(output.api()).getFlows().getFirst().getRequest();
            assertThat(steps).extracting(Step::getName).containsExactly("s1");
        }

        static Stream<Arguments> reorderStepsVariants() {
            var reordered = List.of(flowMap("f1", List.of(stepMap("s2", "p2"), stepMap("s1", "p1"))));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", reordered)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/flows", reordered))
            );
        }

        @ParameterizedTest
        @MethodSource("reorderStepsVariants")
        void reordering_steps_reflects_new_order(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "p1"), aStep("s2", "p2"))))));

            var output = execute(type, body, false);

            var steps = httpV4Def(output.api()).getFlows().getFirst().getRequest();
            assertThat(steps).extracting(Step::getName).containsExactly("s2", "s1");
        }

        static Stream<Arguments> stepFieldEditCases() {
            var fieldDefaults = Map.of(
                "name",
                "renamed",
                "policy",
                "policy-z",
                "condition",
                "{#new}",
                "messageCondition",
                "{#msg}",
                "description",
                "new-desc"
            );
            return fieldDefaults
                .entrySet()
                .stream()
                .flatMap(e ->
                    Stream.of(PatchApiUseCase.PatchType.MERGE_PATCH, PatchApiUseCase.PatchType.JSON_PATCH).map(t ->
                        Arguments.of(t, e.getKey(), e.getValue())
                    )
                );
        }

        @ParameterizedTest
        @MethodSource("stepFieldEditCases")
        void modifying_step_string_field_changes_only_that_field(PatchApiUseCase.PatchType type, String field, String newValue) {
            var existingStep = Step.builder()
                .name("s1")
                .policy("policy-a")
                .enabled(true)
                .description("orig-desc")
                .condition("{#orig}")
                .messageCondition("{#orig-msg}")
                .build();
            var siblingStep = aStep("sibling", "p-sib");
            givenExistingApi(
                apiWithFlows(
                    List.of(
                        Flow.builder().name("f1").enabled(true).request(List.of(existingStep, siblingStep)).build(),
                        aFlow("other", List.of(aStep("ss", "ps")))
                    )
                )
            );

            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/flows/0/request/0/" + field, newValue)
                : buildMergeStepEdit(field, newValue);

            var output = execute(type, body, false);

            var flows = httpV4Def(output.api()).getFlows();
            var patchedStep = flows.getFirst().getRequest().getFirst();
            switch (field) {
                case "name" -> assertThat(patchedStep.getName()).isEqualTo(newValue);
                case "policy" -> assertThat(patchedStep.getPolicy()).isEqualTo(newValue);
                case "condition" -> assertThat(patchedStep.getCondition()).isEqualTo(newValue);
                case "messageCondition" -> assertThat(patchedStep.getMessageCondition()).isEqualTo(newValue);
                case "description" -> assertThat(patchedStep.getDescription()).isEqualTo(newValue);
            }
            if (!"name".equals(field)) {
                assertThat(patchedStep.getName()).isEqualTo("s1");
            }
            if (!"policy".equals(field)) {
                assertThat(patchedStep.getPolicy()).isEqualTo("policy-a");
            }
            assertThat(flows).hasSize(2);
            assertThat(flows.getFirst().getRequest().get(1).getName()).isEqualTo("sibling");
            assertThat(flows.get(1).getName()).isEqualTo("other");
        }

        private static String buildMergeStepEdit(String field, String value) {
            var firstStep = new LinkedHashMap<String, Object>();
            firstStep.put("name", "s1");
            firstStep.put("policy", "policy-a");
            firstStep.put("enabled", true);
            firstStep.put("description", "orig-desc");
            firstStep.put("condition", "{#orig}");
            firstStep.put("messageCondition", "{#orig-msg}");
            firstStep.put(field, value);
            var flows = List.of(
                Map.of("name", "f1", "enabled", true, "request", List.of(firstStep, stepMap("sibling", "p-sib"))),
                flowMap("other", List.of(stepMap("ss", "ps")))
            );
            return mergePatch("flows", flows);
        }

        static Stream<Arguments> stepEnabledVariants() {
            return Stream.of(true, false).flatMap(enabled -> {
                var step = Map.<String, Object>of("name", "s1", "policy", "p1", "enabled", enabled);
                var flow = Map.of("name", "f1", "enabled", true, "request", List.of(step));
                return Stream.of(
                    Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/flows/0/request/0/enabled", enabled), enabled),
                    Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", List.of(flow)), enabled)
                );
            });
        }

        @ParameterizedTest
        @MethodSource("stepEnabledVariants")
        void modifying_step_enabled_changes_only_that_field(PatchApiUseCase.PatchType type, String body, boolean expected) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "p1"))))));

            var output = execute(type, body, false);

            var patchedStep = httpV4Def(output.api()).getFlows().getFirst().getRequest().getFirst();
            assertThat(patchedStep.isEnabled()).isEqualTo(expected);
            assertThat(patchedStep.getName()).isEqualTo("s1");
            assertThat(patchedStep.getPolicy()).isEqualTo("p1");
        }

        static Stream<Arguments> stepConfigurationVariants() {
            var newConfig = Map.of("threshold", 42, "mode", "strict", "tags", List.of("a", "b"));
            var step = Map.of("name", "s1", "policy", "policy-a", "enabled", true, "configuration", newConfig);
            var flow = Map.of("name", "f1", "enabled", true, "request", List.of(step));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/flows/0/request/0/configuration", newConfig)),
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", List.of(flow)))
            );
        }

        @ParameterizedTest
        @MethodSource("stepConfigurationVariants")
        void modifying_step_configuration_object_preserves_value(PatchApiUseCase.PatchType type, String body) {
            var existingStep = Step.builder().name("s1").policy("policy-a").enabled(true).configuration("{}").build();
            givenExistingApi(apiWithFlows(List.of(Flow.builder().name("f1").enabled(true).request(List.of(existingStep)).build())));

            var output = execute(type, body, false);

            var config = httpV4Def(output.api()).getFlows().getFirst().getRequest().getFirst().getConfiguration();
            assertThat(config).contains("\"threshold\":42").contains("\"mode\":\"strict\"").contains("\"a\"").contains("\"b\"");
        }

        static Stream<Arguments> addSelectorVariants() {
            var selector = Map.<String, Object>of("type", "http", "path", "/api", "pathOperator", "EQUALS");
            var flow = Map.of("name", "f1", "enabled", true, "selectors", List.of(selector), "request", List.of(stepMap("s1", "p1")));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", List.of(flow))),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/flows/0/selectors", List.of(selector)))
            );
        }

        @ParameterizedTest
        @MethodSource("addSelectorVariants")
        void adding_a_selector_makes_it_present(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "p1"))))));

            var output = execute(type, body, false);

            var selectors = httpV4Def(output.api()).getFlows().getFirst().getSelectors();
            assertThat(selectors).hasSize(1);
            assertThat(selectors.getFirst()).isInstanceOf(HttpSelector.class);
        }

        static Stream<Arguments> removeSelectorVariants() {
            var withoutSelector = List.of(flowMap("f1", List.of(stepMap("s1", "p1"))));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", withoutSelector)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/flows/0/selectors"))
            );
        }

        @ParameterizedTest
        @MethodSource("removeSelectorVariants")
        void removing_a_selector_makes_it_absent(PatchApiUseCase.PatchType type, String body) {
            var http = HttpSelector.builder().path("/api").pathOperator(Operator.EQUALS).build();
            givenExistingApi(
                apiWithFlows(
                    List.of(Flow.builder().name("f1").enabled(true).selectors(List.of(http)).request(List.of(aStep("s1", "p1"))).build())
                )
            );

            var output = execute(type, body, false);

            var selectors = httpV4Def(output.api()).getFlows().getFirst().getSelectors();
            assertThat(selectors).isNullOrEmpty();
        }

        private static Stream<Arguments> selectorVariants(Map<String, Object> selector, String jsonPatchPath, Object jsonPatchValue) {
            var flow = Map.of(
                "name",
                "f1",
                "enabled",
                true,
                "request",
                List.of(Map.<String, Object>of("name", "s1", "policy", "p1", "enabled", true)),
                "selectors",
                List.of(selector)
            );
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", List.of(flow))),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", jsonPatchPath, jsonPatchValue))
            );
        }

        static Stream<Arguments> httpSelectorPathReplaceVariants() {
            return selectorVariants(
                Map.of("type", "http", "path", "/new", "pathOperator", "EQUALS", "methods", List.of("GET")),
                "/flows/0/selectors/0/path",
                "/new"
            );
        }

        @ParameterizedTest
        @MethodSource("httpSelectorPathReplaceVariants")
        void replacing_http_selector_path_preserves_siblings(PatchApiUseCase.PatchType type, String body) {
            var http = HttpSelector.builder()
                .path("/old")
                .pathOperator(Operator.EQUALS)
                .methods(new LinkedHashSet<>(Set.of(HttpMethod.GET)))
                .build();
            givenExistingApi(
                apiWithFlows(
                    List.of(Flow.builder().name("f1").enabled(true).selectors(List.of(http)).request(List.of(aStep("s1", "p1"))).build())
                )
            );

            var output = execute(type, body, false);

            var selector = (HttpSelector) httpV4Def(output.api()).getFlows().getFirst().getSelectors().getFirst();
            assertThat(selector.getPath()).isEqualTo("/new");
            assertThat(selector.getPathOperator()).isEqualTo(Operator.EQUALS);
            assertThat(selector.getMethods()).contains(HttpMethod.GET);
        }

        static Stream<Arguments> httpSelectorPathOperatorReplaceVariants() {
            return selectorVariants(
                Map.of("type", "http", "path", "/old", "pathOperator", "STARTS_WITH"),
                "/flows/0/selectors/0/pathOperator",
                "STARTS_WITH"
            );
        }

        @ParameterizedTest
        @MethodSource("httpSelectorPathOperatorReplaceVariants")
        void replacing_http_selector_pathOperator_preserves_siblings(PatchApiUseCase.PatchType type, String body) {
            var http = HttpSelector.builder().path("/old").pathOperator(Operator.EQUALS).build();
            givenExistingApi(
                apiWithFlows(
                    List.of(Flow.builder().name("f1").enabled(true).selectors(List.of(http)).request(List.of(aStep("s1", "p1"))).build())
                )
            );

            var output = execute(type, body, false);

            var selector = (HttpSelector) httpV4Def(output.api()).getFlows().getFirst().getSelectors().getFirst();
            assertThat(selector.getPathOperator()).isEqualTo(Operator.STARTS_WITH);
            assertThat(selector.getPath()).isEqualTo("/old");
        }

        static Stream<Arguments> httpSelectorMethodsReplaceVariants() {
            return selectorVariants(
                Map.of("type", "http", "path", "/api", "pathOperator", "EQUALS", "methods", List.of("POST", "PUT")),
                "/flows/0/selectors/0/methods",
                List.of("POST", "PUT")
            );
        }

        @ParameterizedTest
        @MethodSource("httpSelectorMethodsReplaceVariants")
        void replacing_http_selector_methods_updates_method_set(PatchApiUseCase.PatchType type, String body) {
            var http = HttpSelector.builder()
                .path("/api")
                .pathOperator(Operator.EQUALS)
                .methods(new LinkedHashSet<>(Set.of(HttpMethod.GET)))
                .build();
            givenExistingApi(
                apiWithFlows(
                    List.of(Flow.builder().name("f1").enabled(true).selectors(List.of(http)).request(List.of(aStep("s1", "p1"))).build())
                )
            );

            var output = execute(type, body, false);

            var selector = (HttpSelector) httpV4Def(output.api()).getFlows().getFirst().getSelectors().getFirst();
            assertThat(selector.getMethods()).containsExactlyInAnyOrder(HttpMethod.POST, HttpMethod.PUT);
        }

        static Stream<Arguments> conditionSelectorReplaceVariants() {
            return selectorVariants(Map.of("type", "condition", "condition", "{#new}"), "/flows/0/selectors/0/condition", "{#new}");
        }

        @ParameterizedTest
        @MethodSource("conditionSelectorReplaceVariants")
        void replacing_condition_selector_condition_reflects_new_value(PatchApiUseCase.PatchType type, String body) {
            var cond = ConditionSelector.builder().condition("{#old}").build();
            givenExistingApi(
                apiWithFlows(
                    List.of(Flow.builder().name("f1").enabled(true).selectors(List.of(cond)).request(List.of(aStep("s1", "p1"))).build())
                )
            );

            var output = execute(type, body, false);

            var selector = (ConditionSelector) httpV4Def(output.api()).getFlows().getFirst().getSelectors().getFirst();
            assertThat(selector.getCondition()).isEqualTo("{#new}");
        }

        static Stream<Arguments> roundTripVariants() {
            var step = new LinkedHashMap<String, Object>();
            step.put("name", "s1");
            step.put("policy", "policy-a");
            step.put("enabled", true);
            step.put("condition", "{#cond}");
            var flow = Map.of(
                "name",
                "f1",
                "enabled",
                true,
                "selectors",
                List.of(Map.of("type", "http", "path", "/api", "pathOperator", "EQUALS", "methods", List.of("GET", "POST"))),
                "request",
                List.of(step)
            );
            var flows = List.of(flow);
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", flows)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/flows", flows))
            );
        }

        @ParameterizedTest
        @MethodSource("roundTripVariants")
        void round_trip_preserves_nested_step_and_selector_fields(PatchApiUseCase.PatchType type, String body) {
            var http = HttpSelector.builder()
                .path("/api")
                .pathOperator(Operator.EQUALS)
                .methods(new LinkedHashSet<>(Set.of(HttpMethod.GET, HttpMethod.POST)))
                .build();
            var richStep = Step.builder().name("s1").policy("policy-a").enabled(true).condition("{#cond}").build();
            givenExistingApi(
                apiWithFlows(List.of(Flow.builder().name("f1").enabled(true).selectors(List.of(http)).request(List.of(richStep)).build()))
            );

            var output = execute(type, body, false);

            var flows = httpV4Def(output.api()).getFlows();
            assertThat(flows).hasSize(1);
            var f = flows.getFirst();
            assertThat(f.getName()).isEqualTo("f1");
            assertThat(f.getSelectors()).hasSize(1);
            var sel = (HttpSelector) f.getSelectors().getFirst();
            assertThat(sel.getPath()).isEqualTo("/api");
            assertThat(sel.getMethods()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
            assertThat(f.getRequest()).hasSize(1);
            assertThat(f.getRequest().getFirst().getName()).isEqualTo("s1");
            assertThat(f.getRequest().getFirst().getPolicy()).isEqualTo("policy-a");
            assertThat(f.getRequest().getFirst().getCondition()).isEqualTo("{#cond}");
        }

        @Test
        void successful_flows_only_patch_delegates_to_audited_update_with_old_and_new_state() {
            var oldFlow = aFlow("old", List.of(aStep("s-old", "policy-old")));
            givenExistingApi(apiWithFlows(List.of(oldFlow)));
            var preFlows = httpV4Def(apiCrudService.storage().getFirst()).getFlows();

            var addedFlows = List.of(flowMap("added", List.of(stepMap("s1", "policy-a"))));
            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("flows", addedFlows), false);

            assertThat(httpV4Def(output.api()).getFlows()).extracting(Flow::getName).containsExactly("added");
            var captor = ArgumentCaptor.forClass(Api.class);
            verify(updateApiDomainService).updateV4(captor.capture(), any());
            assertThat(httpV4Def(captor.getValue()).getFlows()).extracting(Flow::getName).containsExactly("added");
            assertThat(httpV4Def(captor.getValue()).getFlows()).isNotEqualTo(preFlows);
        }

        @Test
        void flows_only_patch_preserves_existing_flow_execution() {
            var flowExecution = new FlowExecution();
            flowExecution.setMode(FlowMode.BEST_MATCH);
            var base = apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a")))));
            givenExistingApi(base.toBuilder().apiDefinitionValue(httpV4Def(base).toBuilder().flowExecution(flowExecution).build()).build());

            var output = execute(
                PatchApiUseCase.PatchType.MERGE_PATCH,
                mergePatch("flows", List.of(flowMap("f2", List.of(stepMap("s2", "policy-b"))))),
                false
            );

            assertThat(httpV4Def(output.api()).getFlowExecution()).isEqualTo(flowExecution);
        }

        @Test
        void json_patch_not_touching_flows_preserves_null_flows_without_calling_deserializer() {
            var sentinel = List.of(aFlow("sentinel", List.of()));
            var includeNullsMapper = new GraviteeMapper(false);
            includeNullsMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            var localCut = new PatchApiUseCase(
                apiCrudService,
                apiPrimaryOwnerDomainService,
                updateApiDomainService,
                new JsonPatchDomainService(new JsonMergePatchServiceImpl(), new JsonPatchServiceImpl()),
                workflowQueryService,
                includeNullsMapper,
                propertyDomainService,
                node -> sentinel,
                flows -> includeNullsMapper.valueToTree(flows),
                flowCrudService
            );
            givenExistingApi(apiWithFlows(null));

            var output = localCut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                    .patchBody(patch("replace", "/name", "patched-name"))
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            );

            assertThat(httpV4Def(output.api()).getFlows()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void caller_supplied_flow_id_is_stripped_on_dry_run_response(PatchApiUseCase.PatchType type) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))))));
            var supplied = List.of(
                Map.of("id", "my-custom-id", "name", "f1", "enabled", true, "request", List.of(stepMap("s1", "policy-a")))
            );

            var output = execute(type, setField(type, "flows", supplied), true);

            assertThat(httpV4Def(output.api()).getFlows()).extracting(Flow::getId).containsOnlyNulls();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void real_patch_response_returns_db_generated_flow_id(PatchApiUseCase.PatchType type) {
            givenExistingApi(apiWithFlows(List.of(aFlow("f1", List.of(aStep("s1", "policy-a"))))));
            var dbFlow = Flow.builder().id("server-uuid").name("f1").enabled(true).request(List.of(aStep("s1", "policy-a"))).build();
            when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> {
                flowCrudService.saveApiFlows(API_ID, List.of(dbFlow));
                return inv.getArgument(0);
            });
            var supplied = List.of(
                Map.of("id", "my-custom-id", "name", "f1", "enabled", true, "request", List.of(stepMap("s1", "policy-a")))
            );

            var output = execute(type, setField(type, "flows", supplied), false);

            assertThat(httpV4Def(output.api()).getFlows()).extracting(Flow::getId).containsExactly("server-uuid");
        }

        @Test
        void patch_not_touching_flows_returns_db_stored_flow_id_in_response() {
            var preExisting = Flow.builder()
                .id("preexisting-id")
                .name("f1")
                .enabled(true)
                .request(List.of(aStep("s1", "policy-a")))
                .build();
            givenExistingApi(apiWithFlows(List.of(preExisting)));
            flowCrudService.saveApiFlows(API_ID, List.of(preExisting));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "renamed"), false);

            assertThat(httpV4Def(output.api()).getFlows()).extracting(Flow::getId).containsExactly("preexisting-id");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void flows_patch_preserves_existing_flow_id_when_passed_to_update_domain_service(PatchApiUseCase.PatchType type) {
            var existingFlow = Flow.builder()
                .id("existing-uuid")
                .name("f1")
                .enabled(true)
                .request(List.of(aStep("s1", "policy-a")))
                .build();
            givenExistingApi(apiWithFlows(List.of(existingFlow)));
            flowCrudService.saveApiFlows(API_ID, List.of(existingFlow));

            var supplied = List.of(
                Map.of("id", "existing-uuid", "name", "f1", "enabled", true, "request", List.of(stepMap("s1", "policy-a")))
            );
            execute(type, setField(type, "flows", supplied), false);

            var captor = ArgumentCaptor.forClass(Api.class);
            verify(updateApiDomainService).updateV4(captor.capture(), any());
            assertThat(httpV4Def(captor.getValue()).getFlows().getFirst().getId()).isEqualTo("existing-uuid");
        }

        @Test
        void dry_run_patch_not_touching_flows_preserves_flow_ids() {
            var existingFlow = Flow.builder()
                .id("preexisting-uuid")
                .name("f1")
                .enabled(true)
                .request(List.of(aStep("s1", "policy-a")))
                .build();
            givenExistingApi(apiWithFlows(List.of(existingFlow)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "renamed"), true);

            assertThat(httpV4Def(output.api()).getFlows()).extracting(Flow::getId).containsExactly("preexisting-uuid");
        }
    }

    @Nested
    class ResourcesResolution {

        @Test
        void merge_patch_replaces_resources_list() {
            var existing = Resource.builder().name("old").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(
                PatchApiUseCase.PatchType.MERGE_PATCH,
                mergePatch("resources", List.of(resourceMap("new-r", "oauth2"))),
                false
            );

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
            assertThat(httpV4Def(output.api()).getResources().getFirst().getName()).isEqualTo("new-r");
        }

        @Test
        void merge_patch_with_null_erases_resources() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("resources", null), false);

            assertThat(httpV4Def(output.api()).getResources()).isNull();
        }

        @Test
        void merge_patch_with_empty_array_clears_resources() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("resources", List.of()), false);

            assertThat(httpV4Def(output.api()).getResources()).isEmpty();
        }

        @Test
        void merge_patch_omitting_resources_leaves_them_unchanged() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "renamed"), false);

            assertThat(httpV4Def(output.api()).getResources()).containsExactly(existing);
        }

        @Test
        void merge_patch_sets_resources_when_previously_absent() {
            givenExistingApi(apiWithResources(null));

            var output = execute(
                PatchApiUseCase.PatchType.MERGE_PATCH,
                mergePatch("resources", List.of(resourceMap("r1", "cache"))),
                false
            );

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
            assertThat(httpV4Def(output.api()).getResources().getFirst().getName()).isEqualTo("r1");
        }

        @ParameterizedTest
        @ValueSource(strings = { "SOME_UNKNOWN_TYPE", "CACHE" })
        void merge_patch_accepts_resource_type_verbatim(String type) {
            stubValidateV4ReturnsArgument();
            givenExistingApi(apiWithResources(null));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("resources", List.of(resourceMap("r1", type))), false);

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
            assertThat(httpV4Def(output.api()).getResources().getFirst().getType()).isEqualTo(type);
        }

        @Test
        void json_patch_replace_resources_list() {
            var existing = Resource.builder().name("old").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patch("replace", "/resources", List.of(resourceMap("new-r", "oauth2"))),
                false
            );

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
            assertThat(httpV4Def(output.api()).getResources().getFirst().getName()).isEqualTo("new-r");
        }

        @Test
        void json_patch_add_single_resource() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/resources/0", resourceMap("r0", "oauth2")), false);

            assertThat(httpV4Def(output.api()).getResources()).hasSize(2);
            assertThat(httpV4Def(output.api()).getResources().getFirst().getName()).isEqualTo("r0");
        }

        @Test
        void json_patch_append_resource_using_dash() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/resources/-", resourceMap("r2", "oauth2")), false);

            assertThat(httpV4Def(output.api()).getResources()).hasSize(2);
            assertThat(httpV4Def(output.api()).getResources().get(1).getName()).isEqualTo("r2");
        }

        @Test
        void json_patch_remove_single_resource() {
            var r1 = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            var r2 = Resource.builder().name("r2").type("oauth2").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(r1, r2)));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/resources/0"), false);

            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
            assertThat(httpV4Def(output.api()).getResources().getFirst().getName()).isEqualTo("r2");
        }

        @Test
        void json_patch_replace_resource_configuration_atomically() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{\"oldKey\":\"oldValue\"}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patch("replace", "/resources/0/configuration", Map.of("ttl", 300)),
                false
            );

            var config = httpV4Def(output.api()).getResources().getFirst().getConfiguration();
            assertThat(httpV4Def(output.api()).getResources()).hasSize(1);
            assertThat(config).contains("\"ttl\"");
            assertThat(config).doesNotContain("oldKey");
        }

        @Test
        void json_patch_replace_resource_non_configuration_field() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/resources/0/name", "renamed"), false);

            assertThat(httpV4Def(output.api()).getResources().getFirst().getName()).isEqualTo("renamed");
        }

        @Test
        void dry_run_resources_not_persisted() {
            stubValidateV4ReturnsArgument();

            execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("resources", List.of(resourceMap("r1", "cache"))), true);

            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @Test
        void dry_run_with_null_erases_resources() {
            var existing = Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build();
            givenExistingApi(apiWithResources(List.of(existing)));
            stubValidateV4ReturnsArgument();

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("resources", null), true);

            assertThat(httpV4Def(output.api()).getResources()).isNull();
        }

        @Test
        void dry_run_returns_sanitised_resources() {
            var sanitisedResources = List.of(
                Resource.builder().name("r1").type("cache").configuration("{\"timeToLiveSeconds\":0}").enabled(true).build()
            );
            when(updateApiDomainService.validateV4(any(), any())).thenAnswer(inv -> {
                Api api = inv.getArgument(0);
                var def = (io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue();
                return api.toBuilder().apiDefinitionValue(def.toBuilder().resources(sanitisedResources).build()).build();
            });

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("resources", List.of(resourceMap("r1", "cache"))), true);

            var apiCaptor = ArgumentCaptor.forClass(Api.class);
            verify(updateApiDomainService).validateV4(apiCaptor.capture(), any());
            assertThat(httpV4Def(apiCaptor.getValue()).getResources()).containsExactly(
                Resource.builder().name("r1").type("cache").configuration("{}").enabled(true).build()
            );

            assertThat(httpV4Def(output.api()).getResources()).isEqualTo(sanitisedResources);
        }
    }

    @Nested
    class ListenersResolution {

        @Test
        void merge_patch_with_null_listeners_erases_them() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/existing"))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", null), false);

            assertThat(httpV4Def(output.api()).getListeners()).isEmpty();
        }

        @Test
        void merge_patch_with_empty_array_clears_listeners() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/existing"))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", List.of()), false);

            assertThat(httpV4Def(output.api()).getListeners()).isEmpty();
        }

        @Test
        void merge_patch_omitting_listeners_leaves_them_unchanged() {
            var existing = anHttpListener("/existing");
            givenExistingApi(apiWithListeners(List.of(existing)));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "renamed"), false);

            assertThat(httpV4Def(output.api()).getListeners()).containsExactly(existing);
        }

        @Test
        void merge_patch_sets_listeners_when_previously_absent() {
            givenExistingApi(apiWithListeners(null));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", List.of(listenerMap("/new"))), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(1);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().getFirst()).getPaths().getFirst().getPath()).isEqualTo(
                "/new"
            );
        }

        @Test
        void json_patch_add_listener_at_index() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/existing"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/listeners/0", listenerMap("/inserted")), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(2);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().getFirst()).getPaths().getFirst().getPath()).isEqualTo(
                "/inserted"
            );
        }

        @Test
        void json_patch_append_listener_using_dash() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/existing"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/listeners/-", listenerMap("/appended")), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(2);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().get(1)).getPaths().getFirst().getPath()).isEqualTo(
                "/appended"
            );
        }

        @Test
        void json_patch_remove_listener_at_index() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/first"), anHttpListener("/second"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/listeners/0"), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(1);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().getFirst()).getPaths().getFirst().getPath()).isEqualTo(
                "/second"
            );
        }

        @Test
        void merge_patch_with_new_listener_adds_it_to_existing_list() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/existing"))));
            var supplied = List.of(listenerMap("/existing"), listenerMap("/new"));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", supplied), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(2);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().get(1)).getPaths().getFirst().getPath()).isEqualTo("/new");
        }

        @Test
        void json_patch_replace_listeners_with_new_entry_replaces_list() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/old"))));
            var supplied = List.of(listenerMap("/replaced"));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/listeners", supplied), false);

            var listeners = httpV4Def(output.api()).getListeners();
            assertThat(listeners).hasSize(1);
            assertThat(((HttpListener) listeners.getFirst()).getPaths().getFirst().getPath()).isEqualTo("/replaced");
        }

        @Test
        void merge_patch_replaces_existing_listener_in_place() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/original"))));
            var modified = List.of(listenerMap("/modified"));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", modified), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(1);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().getFirst()).getPaths().getFirst().getPath()).isEqualTo(
                "/modified"
            );
        }

        @Test
        void merge_patch_updates_entrypoints_on_existing_listener() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/path"))));
            var withTwoEntrypoints = Map.of(
                "type",
                "http",
                "paths",
                List.of(Map.of("path", "/path")),
                "entrypoints",
                List.of(Map.of("type", "http-proxy", "configuration", "{}"), Map.of("type", "http-get", "configuration", "{}"))
            );

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", List.of(withTwoEntrypoints)), false);

            var listeners = httpV4Def(output.api()).getListeners();
            assertThat(listeners).hasSize(1);
            assertThat(((HttpListener) listeners.getFirst()).getEntrypoints()).hasSize(2);
        }

        @Test
        void merge_patch_removes_listener_by_sending_shorter_list() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/first"), anHttpListener("/second"))));
            var withoutSecond = List.of(listenerMap("/first"));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", withoutSecond), false);

            assertThat(httpV4Def(output.api()).getListeners()).hasSize(1);
            assertThat(((HttpListener) httpV4Def(output.api()).getListeners().getFirst()).getPaths().getFirst().getPath()).isEqualTo(
                "/first"
            );
        }

        @Test
        void json_patch_remove_listeners_clears_list() {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/first"), anHttpListener("/second"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/listeners"), false);

            assertThat(httpV4Def(output.api()).getListeners()).isEmpty();
        }

        static Stream<Arguments> reorderListenersVariants() {
            var reordered = List.of(listenerMap("/second"), listenerMap("/first"));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", reordered)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/listeners", reordered))
            );
        }

        @ParameterizedTest
        @MethodSource("reorderListenersVariants")
        void reordering_listeners_reflects_new_order(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithListeners(List.of(anHttpListener("/first"), anHttpListener("/second"))));

            var output = execute(type, body, false);

            var listeners = httpV4Def(output.api()).getListeners();
            assertThat(listeners).hasSize(2);
            assertThat(((HttpListener) listeners.getFirst()).getPaths().getFirst().getPath()).isEqualTo("/second");
            assertThat(((HttpListener) listeners.get(1)).getPaths().getFirst().getPath()).isEqualTo("/first");
        }

        @Test
        void validation_failure_on_listeners_surfaces_as_exception() {
            doThrow(new ValidationDomainException("duplicate listener", Map.of("listeners", "duplicate path")))
                .when(updateApiDomainService)
                .updateV4(any(), any());
            var duplicated = List.of(listenerMap("/dup"), listenerMap("/dup"));

            assertThatThrownBy(() ->
                execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", duplicated), false)
            ).isExactlyInstanceOf(ValidationDomainException.class);
        }

        @Test
        void dry_run_returns_sanitised_listeners() {
            List<Listener> sanitisedListeners = List.of(
                HttpListener.builder()
                    .paths(List.of(Path.builder().path("/sanitized").build()))
                    .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                    .build()
            );
            when(updateApiDomainService.validateV4(any(), any())).thenAnswer(inv -> {
                Api api = inv.getArgument(0);
                var def = api.getApiDefinitionHttpV4();
                return api.toBuilder().apiDefinitionValue(def.toBuilder().listeners(sanitisedListeners).build()).build();
            });

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", List.of(listenerMap("/requested"))), true);

            assertThat(httpV4Def(output.api()).getListeners()).isEqualTo(sanitisedListeners);
        }

        @Test
        void dry_run_does_not_persist_listeners_change() {
            stubValidateV4ReturnsArgument();
            var original = apiWithListeners(List.of(anHttpListener("/original")));
            givenExistingApi(original);

            execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("listeners", List.of(listenerMap("/changed"))), true);

            var stored = apiCrudService.storage().getFirst();
            assertThat(((HttpListener) httpV4Def(stored).getListeners().getFirst()).getPaths().getFirst().getPath()).isEqualTo("/original");
        }
    }

    @Nested
    class EndpointGroupsResolution {

        static EndpointGroup aGroup(String name) {
            return EndpointGroup.builder()
                .name(name)
                .type("http-proxy")
                .sharedConfiguration("{}")
                .endpoints(
                    List.of(
                        Endpoint.builder()
                            .name("default-endpoint")
                            .type("http-proxy")
                            .weight(1)
                            .inheritConfiguration(true)
                            .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                            .build()
                    )
                )
                .build();
        }

        static Map<String, Object> groupMap(String name) {
            return Map.of(
                "name",
                name,
                "type",
                "http-proxy",
                "sharedConfiguration",
                "{}",
                "endpoints",
                List.of(
                    Map.of(
                        "name",
                        "default-endpoint",
                        "type",
                        "http-proxy",
                        "weight",
                        1,
                        "inheritConfiguration",
                        true,
                        "configuration",
                        "{\"target\":\"https://api.gravitee.io/echo\"}"
                    )
                )
            );
        }

        @Test
        void merge_patch_appending_endpoint_group_grows_list_by_one() {
            var existing = aGroup("group-a");
            givenExistingApi(apiWithEndpointGroups(List.of(existing)));

            var newGroups = List.of(groupMap("group-a"), groupMap("group-b"));
            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", newGroups), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).hasSize(2);
            assertThat(httpV4Def(output.api()).getEndpointGroups().get(1).getName()).isEqualTo("group-b");
        }

        @Test
        void json_patch_add_at_minus_one_appends_endpoint_group() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/endpointGroups/-", groupMap("group-b")), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).hasSize(2);
            assertThat(httpV4Def(output.api()).getEndpointGroups().get(1).getName()).isEqualTo("group-b");
        }

        @Test
        void merge_patch_with_one_group_removed_persists_shorter_list() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"), aGroup("group-b"))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", List.of(groupMap("group-a"))), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).hasSize(1);
            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getName()).isEqualTo("group-a");
        }

        @Test
        void json_patch_remove_endpoint_group_at_index_persists_shorter_list() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"), aGroup("group-b"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/endpointGroups/1"), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).hasSize(1);
            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getName()).isEqualTo("group-a");
        }

        static Stream<Arguments> reorderEndpointGroupsVariants() {
            var reordered = List.of(groupMap("group-b"), groupMap("group-a"));
            return Stream.of(
                Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", reordered)),
                Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/endpointGroups", reordered))
            );
        }

        @ParameterizedTest
        @MethodSource("reorderEndpointGroupsVariants")
        void reordering_endpoint_groups_reflects_new_order(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"), aGroup("group-b"))));

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getEndpointGroups())
                .extracting(EndpointGroup::getName)
                .containsExactly("group-b", "group-a");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_endpoint_weight_persists_only_that_field(PatchApiUseCase.PatchType type) {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/endpoints/0/weight", 5)
                : mergePatch(
                    "endpointGroups",
                    List.of(
                        Map.of(
                            "name",
                            "group-a",
                            "type",
                            "http-proxy",
                            "sharedConfiguration",
                            "{}",
                            "endpoints",
                            List.of(
                                Map.of(
                                    "name",
                                    "default-endpoint",
                                    "type",
                                    "http-proxy",
                                    "weight",
                                    5,
                                    "inheritConfiguration",
                                    true,
                                    "configuration",
                                    "{\"target\":\"https://api.gravitee.io/echo\"}"
                                )
                            )
                        )
                    )
                );

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getEndpoints().getFirst().getWeight()).isEqualTo(5);
            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getEndpoints().getFirst().getName()).isEqualTo(
                "default-endpoint"
            );
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_endpoint_secondary_flag_persists_only_that_field(PatchApiUseCase.PatchType type) {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/endpoints/0/secondary", true)
                : mergePatch(
                    "endpointGroups",
                    List.of(
                        Map.of(
                            "name",
                            "group-a",
                            "type",
                            "http-proxy",
                            "sharedConfiguration",
                            "{}",
                            "endpoints",
                            List.of(
                                Map.of(
                                    "name",
                                    "default-endpoint",
                                    "type",
                                    "http-proxy",
                                    "weight",
                                    1,
                                    "inheritConfiguration",
                                    true,
                                    "secondary",
                                    true,
                                    "configuration",
                                    "{\"target\":\"https://api.gravitee.io/echo\"}"
                                )
                            )
                        )
                    )
                );

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getEndpoints().getFirst().isSecondary()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_health_check_config_persists_changes(PatchApiUseCase.PatchType type) {
            var hcService = new Service();
            hcService.setEnabled(false);
            var existingServices = EndpointGroupServices.builder().healthCheck(hcService).build();
            var groupWithHc = aGroup("group-a").toBuilder().services(existingServices).build();
            givenExistingApi(apiWithEndpointGroups(List.of(groupWithHc)));

            var enabledHc = Map.of("enabled", true, "type", "http-health-check");
            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/services/healthCheck", enabledHc)
                : mergePatch(
                    "endpointGroups",
                    List.of(
                        Map.of(
                            "name",
                            "group-a",
                            "type",
                            "http-proxy",
                            "sharedConfiguration",
                            "{}",
                            "services",
                            Map.of("healthCheck", enabledHc),
                            "endpoints",
                            List.of(
                                Map.of(
                                    "name",
                                    "default-endpoint",
                                    "type",
                                    "http-proxy",
                                    "weight",
                                    1,
                                    "inheritConfiguration",
                                    true,
                                    "configuration",
                                    "{\"target\":\"https://api.gravitee.io/echo\"}"
                                )
                            )
                        )
                    )
                );

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getServices().getHealthCheck().isEnabled()).isTrue();
        }

        @Test
        void merge_patch_with_empty_endpointGroups_array_propagates_empty_to_definition() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", List.of()), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).isEmpty();
        }

        @Test
        void domain_service_rejection_of_empty_endpointGroups_propagates_to_caller() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));
            doThrow(
                new ValidationDomainException("endpointGroups must not be empty", Map.of("location", "/endpointGroups"), "invalidValue")
            )
                .when(updateApiDomainService)
                .updateV4(any(), any());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", List.of()), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("endpointGroups");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_with_endpointGroups_invokes_validate_not_update(PatchApiUseCase.PatchType type) {
            stubValidateV4ReturnsArgument();
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var output = execute(type, setField(type, "endpointGroups", List.of(groupMap("group-b"))), true);

            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getName()).isEqualTo("group-b");
            verify(updateApiDomainService).validateV4(any(), any());
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @Test
        void merge_patch_with_endpointGroups_null_clears_endpointGroups_on_rebuilt_definition() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", null), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).isNull();
        }

        @Test
        void domain_service_rejection_of_null_endpointGroups_propagates_to_caller() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));
            doThrow(new ValidationDomainException("endpointGroups must not be null", Map.of("location", "/endpointGroups"), "invalidValue"))
                .when(updateApiDomainService)
                .updateV4(any(), any());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", null), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("endpointGroups");
        }

        @Test
        void json_patch_remove_endpointGroups_clears_endpointGroups_on_rebuilt_definition() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/endpointGroups"), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_endpoint_inheritConfiguration_persists_only_that_field(PatchApiUseCase.PatchType type) {
            var group = EndpointGroup.builder()
                .name("group-a")
                .type("http-proxy")
                .sharedConfiguration("{}")
                .endpoints(
                    List.of(
                        Endpoint.builder()
                            .name("default-endpoint")
                            .type("http-proxy")
                            .weight(1)
                            .inheritConfiguration(false)
                            .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                            .build()
                    )
                )
                .build();
            givenExistingApi(apiWithEndpointGroups(List.of(group)));

            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/endpoints/0/inheritConfiguration", true)
                : mergePatch(
                    "endpointGroups",
                    List.of(
                        Map.of(
                            "name",
                            "group-a",
                            "type",
                            "http-proxy",
                            "sharedConfiguration",
                            "{}",
                            "endpoints",
                            List.of(
                                Map.of(
                                    "name",
                                    "default-endpoint",
                                    "type",
                                    "http-proxy",
                                    "weight",
                                    1,
                                    "inheritConfiguration",
                                    true,
                                    "configuration",
                                    "{\"target\":\"https://api.gravitee.io/echo\"}"
                                )
                            )
                        )
                    )
                );

            var output = execute(type, body, false);

            var endpoint = httpV4Def(output.api()).getEndpointGroups().getFirst().getEndpoints().getFirst();
            assertThat(endpoint.isInheritConfiguration()).isTrue();
            assertThat(endpoint.getName()).isEqualTo("default-endpoint");
            assertThat(endpoint.getType()).isEqualTo("http-proxy");
            assertThat(endpoint.getWeight()).isEqualTo(1);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_group_name_persists_only_that_field(PatchApiUseCase.PatchType type) {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/name", "group-renamed")
                : mergePatch("endpointGroups", List.of(groupMap("group-renamed")));

            var output = execute(type, body, false);

            var groups = httpV4Def(output.api()).getEndpointGroups();
            assertThat(groups).hasSize(1);
            assertThat(groups.getFirst().getName()).isEqualTo("group-renamed");
            assertThat(groups.getFirst().getType()).isEqualTo("http-proxy");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_loadBalancer_type_persists_only_that_field(PatchApiUseCase.PatchType type) {
            var loadBalancer = new LoadBalancer();
            loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
            var group = aGroup("group-a").toBuilder().loadBalancer(loadBalancer).build();
            givenExistingApi(apiWithEndpointGroups(List.of(group)));

            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/loadBalancer/type", "RANDOM")
                : mergePatch(
                    "endpointGroups",
                    List.of(
                        Map.of(
                            "name",
                            "group-a",
                            "type",
                            "http-proxy",
                            "sharedConfiguration",
                            "{}",
                            "loadBalancer",
                            Map.of("type", "RANDOM"),
                            "endpoints",
                            List.of(
                                Map.of(
                                    "name",
                                    "default-endpoint",
                                    "type",
                                    "http-proxy",
                                    "weight",
                                    1,
                                    "inheritConfiguration",
                                    true,
                                    "configuration",
                                    "{\"target\":\"https://api.gravitee.io/echo\"}"
                                )
                            )
                        )
                    )
                );

            var output = execute(type, body, false);

            assertThat(httpV4Def(output.api()).getEndpointGroups().getFirst().getLoadBalancer().getType()).isEqualTo(
                LoadBalancerType.RANDOM
            );
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void modifying_endpoint_configuration_blob_persists_only_that_field(PatchApiUseCase.PatchType type) {
            var group = EndpointGroup.builder()
                .name("group-a")
                .type("http-proxy")
                .sharedConfiguration("{}")
                .endpoints(
                    List.of(
                        Endpoint.builder()
                            .name("default-endpoint")
                            .type("http-proxy")
                            .weight(1)
                            .inheritConfiguration(true)
                            .configuration("{\"target\":\"https://original.io\"}")
                            .build()
                    )
                )
                .build();
            givenExistingApi(apiWithEndpointGroups(List.of(group)));

            var newConfig = Map.of("target", "https://updated.io");
            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups/0/endpoints/0/configuration", newConfig)
                : mergePatch(
                    "endpointGroups",
                    List.of(
                        Map.of(
                            "name",
                            "group-a",
                            "type",
                            "http-proxy",
                            "sharedConfiguration",
                            "{}",
                            "endpoints",
                            List.of(
                                Map.of(
                                    "name",
                                    "default-endpoint",
                                    "type",
                                    "http-proxy",
                                    "weight",
                                    1,
                                    "inheritConfiguration",
                                    true,
                                    "configuration",
                                    newConfig
                                )
                            )
                        )
                    )
                );

            var output = execute(type, body, false);

            var endpoint = httpV4Def(output.api()).getEndpointGroups().getFirst().getEndpoints().getFirst();
            assertThat(endpoint.getConfiguration()).contains("\"target\":\"https://updated.io\"");
            assertThat(endpoint.getName()).isEqualTo("default-endpoint");
            assertThat(endpoint.getType()).isEqualTo("http-proxy");
            assertThat(endpoint.getWeight()).isEqualTo(1);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void patch_not_addressing_endpointGroups_leaves_existing_list_intact(PatchApiUseCase.PatchType type) {
            var existing = List.of(aGroup("group-a"), aGroup("group-b"));
            givenExistingApi(apiWithEndpointGroups(existing));

            var output = execute(type, setField(type, "name", "renamed"), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).isEqualTo(existing);
        }

        @Test
        void successful_endpointGroups_only_patch_delegates_to_audited_update_with_old_and_new_state() {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));
            var preGroups = httpV4Def(apiCrudService.storage().getFirst()).getEndpointGroups();

            var output = execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("endpointGroups", List.of(groupMap("group-b"))), false);

            assertThat(httpV4Def(output.api()).getEndpointGroups()).extracting(EndpointGroup::getName).containsExactly("group-b");
            var captor = ArgumentCaptor.forClass(Api.class);
            verify(updateApiDomainService).updateV4(captor.capture(), any());
            assertThat(httpV4Def(captor.getValue()).getEndpointGroups()).extracting(EndpointGroup::getName).containsExactly("group-b");
            assertThat(httpV4Def(captor.getValue()).getEndpointGroups()).isNotEqualTo(preGroups);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void malformed_endpointGroups_element_triggers_validation_exception_with_location_pointer(PatchApiUseCase.PatchType type) {
            givenExistingApi(apiWithEndpointGroups(List.of(aGroup("group-a"))));

            var malformedGroup = Map.of(
                "name",
                "group-a",
                "type",
                "http-proxy",
                "sharedConfiguration",
                "{}",
                "endpoints",
                List.of(
                    Map.of(
                        "name",
                        "default-endpoint",
                        "type",
                        "http-proxy",
                        "weight",
                        Map.of("not", "an-int"),
                        "inheritConfiguration",
                        true,
                        "configuration",
                        "{}"
                    )
                )
            );
            var body = type == PatchApiUseCase.PatchType.JSON_PATCH
                ? patch("replace", "/endpointGroups", List.of(malformedGroup))
                : mergePatch("endpointGroups", List.of(malformedGroup));

            assertThatThrownBy(() -> execute(type, body, false))
                .isInstanceOf(ValidationDomainException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ValidationDomainException.class))
                .satisfies(ex -> {
                    assertThat(ex.getTechnicalCode()).isEqualTo("invalidValue");
                    assertThat(ex.getParameters()).containsKey("location");
                    assertThat(ex.getParameters().get("location")).startsWith("/endpointGroups/0");
                });
        }
    }
}
