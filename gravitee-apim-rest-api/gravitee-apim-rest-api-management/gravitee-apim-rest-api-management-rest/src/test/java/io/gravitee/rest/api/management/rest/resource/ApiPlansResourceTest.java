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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PLAN = "my-plan";
    private static final String SECURITY_DEFINITION = "{\"signature\":\"HMAC_HS256\",\"resolverParameter\":\"plan-key\"}";

    @Override
    protected String contextPath() {
        return "apis";
    }

    @BeforeEach
    public void init() {
        Mockito.reset(planService, apiService, apiSearchServiceV4, groupService, accessControlService);
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(true);
        GraviteeContext.cleanContext();
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnBadRequestWhenGettingPlansForV4Api() {
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(
            v4Api(DefinitionVersion.V4)
        );

        final Response response = envTarget().path(API).path("plans").request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiService, never()).findById(any(), eq(API));
        verify(planService, never()).findByApi(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenGettingSinglePlanForV4Api() {
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(
            v4Api(DefinitionVersion.V4)
        );

        final Response response = planTarget().request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiService, never()).findById(any(), eq(API));
        verify(planService, never()).findById(any(), any());
    }

    @Test
    void should_return_403_when_getting_plans_of_public_api_exposed_in_no_portal_without_permission() {
        mockReadableV2Api();
        withoutPlanOrLogPermission();
        when(accessControlService.canConsumeApiFromPortal(any(ExecutionContext.class), any(ApiEntity.class))).thenReturn(false);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        verify(planService, never()).findByApi(any(), any());
    }

    @Test
    void should_return_403_when_getting_a_plan_of_public_api_exposed_in_no_portal_without_permission() {
        mockReadableV2Api();
        withoutPlanOrLogPermission();
        when(accessControlService.canConsumeApiFromPortal(any(ExecutionContext.class), any(ApiEntity.class))).thenReturn(false);

        final Response response = planTarget().request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        verify(planService, never()).findById(any(), any());
    }

    @Test
    void should_return_only_plan_summary_to_portal_visibility_caller_without_permissions() {
        mockPortalVisibleV2ApiWithPlan();
        withoutPermission(RolePermission.API_PLAN);
        withoutPermission(RolePermission.API_GATEWAY_DEFINITION);

        assertPlanSummaryOnly(getPlan(), PlanStatus.PUBLISHED);
    }

    @Test
    void should_return_only_plan_summary_to_caller_without_gateway_definition_read() {
        mockPortalVisibleV2ApiWithPlan();
        withoutPermission(RolePermission.API_GATEWAY_DEFINITION);

        assertPlanSummaryOnly(getPlan(), PlanStatus.PUBLISHED);
    }

    @Test
    void should_return_only_plan_summary_to_member_with_plan_read_without_gateway_definition_read() {
        mockPortalVisibleV2ApiWithPlan();
        notExposedInPortal();
        withoutAnyPermission();
        withReadPermission(RolePermission.API_PLAN);

        assertPlanSummaryOnly(getPlan(), PlanStatus.PUBLISHED);
    }

    @Test
    void should_return_only_plan_summary_to_caller_without_plan_read() {
        mockPortalVisibleV2ApiWithPlan();
        withoutPermission(RolePermission.API_PLAN);

        assertPlanSummaryOnly(getPlan(), PlanStatus.PUBLISHED);
    }

    @Test
    void should_return_full_plan_to_caller_with_plan_and_gateway_definition_read() {
        mockPortalVisibleV2ApiWithPlan();
        notExposedInPortal();
        withOnlyPlanAndGatewayDefinitionRead();

        assertFullPlan(getPlan(), PlanStatus.PUBLISHED);
    }

    @Test
    void should_return_only_plan_summary_after_update_to_caller_without_gateway_definition_read() {
        mockV2ApiWithPlanWrites();
        withoutPermission(RolePermission.API_GATEWAY_DEFINITION);

        final Response response = planTarget().request().put(Entity.json(planUpdate()));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertPlanSummaryOnly(response.readEntity(JsonNode.class), PlanStatus.PUBLISHED);
    }

    @ParameterizedTest
    @CsvSource({ "_close,CLOSED", "_publish,PUBLISHED", "_deprecate,DEPRECATED", "_depreciate,DEPRECATED" })
    void should_return_only_plan_summary_after_status_change_to_caller_without_gateway_definition_read(
        String action,
        PlanStatus expectedStatus
    ) {
        mockV2ApiWithPlanWrites();
        withoutPermission(RolePermission.API_GATEWAY_DEFINITION);

        final Response response = planTarget().path(action).request().post(Entity.json(""));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertPlanSummaryOnly(response.readEntity(JsonNode.class), expectedStatus);
    }

    @Test
    void should_return_full_plan_after_update_to_caller_with_plan_and_gateway_definition_read() {
        mockV2ApiWithPlanWrites();
        withOnlyPlanAndGatewayDefinitionRead();
        withPlanUpdatePermission();

        final Response response = planTarget().request().put(Entity.json(planUpdate()));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertFullPlan(response.readEntity(JsonNode.class), PlanStatus.PUBLISHED);
    }

    @ParameterizedTest
    @CsvSource({ "_close,CLOSED", "_publish,PUBLISHED", "_deprecate,DEPRECATED", "_depreciate,DEPRECATED" })
    void should_return_full_plan_after_status_change_to_caller_with_plan_and_gateway_definition_read(
        String action,
        PlanStatus expectedStatus
    ) {
        mockV2ApiWithPlanWrites();
        withOnlyPlanAndGatewayDefinitionRead();
        withPlanUpdatePermission();

        final Response response = planTarget().path(action).request().post(Entity.json(""));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertFullPlan(response.readEntity(JsonNode.class), expectedStatus);
    }

    @Test
    void should_list_plan_summaries_to_portal_visibility_caller_without_permissions() {
        mockPortalVisibleV2ApiWithPlan();
        withoutPermission(RolePermission.API_PLAN);
        withoutPermission(RolePermission.API_GATEWAY_DEFINITION);
        withoutPermission(RolePermission.API_LOG);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertSinglePlanSummary(response.readEntity(JsonNode.class));
    }

    @ParameterizedTest
    @EnumSource(value = RolePermission.class, names = { "API_PLAN", "API_LOG" })
    void should_list_plan_summaries_to_member_with_read_permission_without_gateway_definition_read(RolePermission permission) {
        mockPortalVisibleV2ApiWithPlan();
        notExposedInPortal();
        withoutAnyPermission();
        withReadPermission(permission);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertSinglePlanSummary(response.readEntity(JsonNode.class));
    }

    @Test
    void should_list_full_plans_to_caller_with_plan_and_gateway_definition_read() {
        mockPortalVisibleV2ApiWithPlan();
        notExposedInPortal();
        withOnlyPlanAndGatewayDefinitionRead();

        final Response response = envTarget().path(API).path("plans").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        JsonNode plans = response.readEntity(JsonNode.class);
        assertThat(plans).hasSize(1);
        assertFullPlan(plans.get(0), PlanStatus.PUBLISHED);
    }

    @Test
    public void shouldGetApiPlansForV2Api() {
        mockReadableV2Api();

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("published-plan");
        publishedPlan.setReferenceId(API);
        publishedPlan.setReferenceType(GenericPlanEntity.ReferenceType.API);
        publishedPlan.setStatus(PlanStatus.PUBLISHED);
        publishedPlan.setOrder(1);
        publishedPlan.setSecurity(PlanSecurityType.KEY_LESS);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(Set.of(publishedPlan));
        when(groupService.isUserAuthorizedToAccessApiData(any(ApiEntity.class), any(), any())).thenReturn(true);

        final Response response = envTarget().path(API).path("plans").request().get();

        assertEquals(OK_200, response.getStatus());
        List<PlanEntity> plans = response.readEntity(new GenericType<>() {});
        assertNotNull(plans);
        assertEquals(1, plans.size());
    }

    @Test
    public void shouldCreateApiPlanForV4Api() {
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(
            v4Api(DefinitionVersion.V4)
        );

        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName(PLAN);
        newPlanEntity.setDescription("my-plan-description");
        newPlanEntity.setValidation(PlanValidationType.AUTO);
        newPlanEntity.setSecurity(PlanSecurityType.KEY_LESS);
        newPlanEntity.setType(PlanType.API);
        newPlanEntity.setStatus(PlanStatus.STAGING);

        PlanEntity createdPlanEntity = new PlanEntity();
        createdPlanEntity.setId("new-plan-id");
        when(planService.create(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(createdPlanEntity);

        final Response response = envTarget().path(API).path("plans").request().post(Entity.json(newPlanEntity));

        assertEquals(CREATED_201, response.getStatus());
        verify(planService).create(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldCreateApiPlan() {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName(PLAN);
        newPlanEntity.setDescription("my-plan-description");
        newPlanEntity.setValidation(PlanValidationType.AUTO);
        newPlanEntity.setSecurity(PlanSecurityType.KEY_LESS);
        newPlanEntity.setType(PlanType.API);
        newPlanEntity.setStatus(PlanStatus.STAGING);

        PlanEntity createdPlanEntity = new PlanEntity();
        createdPlanEntity.setId("new-plan-id");
        when(planService.create(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(createdPlanEntity);

        final Response response = envTarget().path(API).path("plans").request().post(Entity.json(newPlanEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(API).path("plans").path("new-plan-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void shouldCloseApiPlan() {
        ApiEntity api = getApi(DefinitionVersion.V1);

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setReferenceId(API);
        existingPlan.setReferenceType(GenericPlanEntity.ReferenceType.API);

        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("closed-plan-id");
        when(apiService.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(api);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(existingPlan);
        when(planService.close(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(closedPlan);

        final Response response = planTarget().path("_close").request().post(Entity.json(""));

        assertEquals(OK_200, response.getStatus());
        verify(planService, times(1)).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
        verify(apiService, never()).update(eq(GraviteeContext.getExecutionContext()), eq(API), any());
    }

    @Test
    public void shouldDeleteApiPlan() {
        ApiEntity api = getApi(DefinitionVersion.V1);

        PlanEntity existingPlan = new PlanEntity();
        existingPlan.setName(PLAN);
        existingPlan.setReferenceId(API);
        existingPlan.setReferenceType(GenericPlanEntity.ReferenceType.API);

        when(apiService.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(api);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(existingPlan);

        final Response response = planTarget().request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(planService, times(1)).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN));
    }

    @Test
    public void shouldReturnBadRequestWhenGettingAnApiProductPlan() {
        mockApiProductPlan();

        final Response response = planTarget().request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingAnApiProductPlan() {
        mockApiProductPlan();

        UpdatePlanEntity updatePlanEntity = new UpdatePlanEntity();
        updatePlanEntity.setId(PLAN);
        updatePlanEntity.setName("updated-plan");
        updatePlanEntity.setDescription("updated-description");
        updatePlanEntity.setValidation(PlanValidationType.AUTO);

        final Response response = planTarget().request().put(Entity.json(updatePlanEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(planService, never()).update(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenDeletingAnApiProductPlan() {
        mockApiProductPlan();

        final Response response = planTarget().request().delete();

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(planService, never()).delete(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenClosingAnApiProductPlan() {
        mockApiProductPlan();

        final Response response = planTarget().path("_close").request().post(Entity.json(""));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(planService, never()).close(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenPublishingAnApiProductPlan() {
        mockApiProductPlan();

        final Response response = planTarget().path("_publish").request().post(Entity.json(""));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(planService, never()).publish(any(), any());
    }

    @Test
    public void shouldReturnBadRequestWhenDeprecatingAnApiProductPlan() {
        mockApiProductPlan();

        final Response response = planTarget().path("_deprecate").request().post(Entity.json(""));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(planService, never()).deprecate(any(), any());
    }

    private void mockApiProductPlan() {
        mockReadableV2Api();
        PlanEntity apiProductPlan = new PlanEntity();
        apiProductPlan.setId(PLAN);
        apiProductPlan.setReferenceId("an-api-product");
        apiProductPlan.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(apiProductPlan);
    }

    private void mockPortalVisibleV2ApiWithPlan() {
        mockReadableV2Api();
        when(accessControlService.canConsumeApiFromPortal(any(ExecutionContext.class), any(ApiEntity.class))).thenReturn(true);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(
            planWithFlowsAndSecurityDefinition(PlanStatus.PUBLISHED)
        );
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(
            Set.of(planWithFlowsAndSecurityDefinition(PlanStatus.PUBLISHED))
        );
        when(groupService.isUserAuthorizedToAccessApiData(any(ApiEntity.class), any(), any())).thenReturn(true);
    }

    private JsonNode getPlan() {
        final Response response = planTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        return response.readEntity(JsonNode.class);
    }

    private void mockV2ApiWithPlanWrites() {
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(
            planWithFlowsAndSecurityDefinition(PlanStatus.STAGING)
        );
        when(planService.update(eq(GraviteeContext.getExecutionContext()), any(UpdatePlanEntity.class))).thenReturn(
            planWithFlowsAndSecurityDefinition(PlanStatus.PUBLISHED)
        );
        when(planService.close(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(
            planWithFlowsAndSecurityDefinition(PlanStatus.CLOSED)
        );
        when(planService.publish(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(
            planWithFlowsAndSecurityDefinition(PlanStatus.PUBLISHED)
        );
        when(planService.deprecate(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(
            planWithFlowsAndSecurityDefinition(PlanStatus.DEPRECATED)
        );
    }

    private static PlanEntity planWithFlowsAndSecurityDefinition(PlanStatus status) {
        Flow flow = new Flow();
        flow.setName("plan-flow");
        PlanEntity plan = new PlanEntity();
        plan.setId(PLAN);
        plan.setName("JWT plan");
        plan.setReferenceId(API);
        plan.setReferenceType(GenericPlanEntity.ReferenceType.API);
        plan.setApi(API);
        plan.setStatus(status);
        plan.setType(PlanType.API);
        plan.setValidation(PlanValidationType.MANUAL);
        plan.setSecurity(PlanSecurityType.JWT);
        plan.setSecurityDefinition(SECURITY_DEFINITION);
        plan.setFlows(List.of(flow));
        plan.setPaths(Map.of("/", List.of(new Rule())));
        plan.setDescription("JWT plan description");
        plan.setCharacteristics(List.of("secured"));
        plan.setOrder(2);
        plan.setCommentRequired(true);
        plan.setCommentMessage("Why do you need it?");
        plan.setGeneralConditions("conditions-page");
        plan.setTags(Set.of("internal"));
        plan.setSelectionRule("#context.attributes['jwt'] != null");
        plan.setExcludedGroups(List.of("excluded-group"));
        plan.setCrossId("plan-cross-id");
        plan.setHrid("plan-hrid");
        plan.setEnvironmentId("DEFAULT");
        plan.setCreatedAt(new Date(1_000L));
        plan.setUpdatedAt(new Date(2_000L));
        plan.setPublishedAt(new Date(3_000L));
        plan.setClosedAt(new Date(4_000L));
        return plan;
    }

    private static UpdatePlanEntity planUpdate() {
        UpdatePlanEntity updatePlanEntity = new UpdatePlanEntity();
        updatePlanEntity.setId(PLAN);
        updatePlanEntity.setName("JWT plan");
        updatePlanEntity.setDescription("JWT plan description");
        updatePlanEntity.setValidation(PlanValidationType.AUTO);
        return updatePlanEntity;
    }

    private static void assertFullPlan(JsonNode plan, PlanStatus expectedStatus) {
        assertThat(plan.get("status").asText()).isEqualTo(expectedStatus.name());
        assertThat(plan.get("security").asText()).isEqualTo(PlanSecurityType.JWT.name());
        assertThat(plan.get("securityDefinition").asText()).isEqualTo(SECURITY_DEFINITION);
        assertThat(plan.get("flows")).hasSize(1);
        assertThat(plan.get("flows").get(0).get("name").asText()).isEqualTo("plan-flow");
        assertThat(plan.get("paths").has("/")).isTrue();
        assertThat(plan.get("tags")).hasSize(1);
        assertThat(plan.get("selection_rule").asText()).isEqualTo("#context.attributes['jwt'] != null");
        assertThat(plan.get("excluded_groups")).hasSize(1);
        assertThat(plan.get("crossId").asText()).isEqualTo("plan-cross-id");
    }

    private static void assertSinglePlanSummary(JsonNode plans) {
        assertThat(plans).hasSize(1);
        assertPlanSummaryOnly(plans.get(0), PlanStatus.PUBLISHED);
    }

    private static void assertPlanSummaryOnly(JsonNode plan, PlanStatus expectedStatus) {
        assertThat(plan.get("id").asText()).isEqualTo(PLAN);
        assertThat(plan.get("name").asText()).isEqualTo("JWT plan");
        assertThat(plan.get("security").asText()).isEqualTo(PlanSecurityType.JWT.name());
        assertThat(plan.get("status").asText()).isEqualTo(expectedStatus.name());
        assertThat(plan.get("type").asText()).isEqualTo(PlanType.API.name());
        assertThat(plan.get("validation").asText()).isEqualTo(PlanValidationType.MANUAL.name());
        assertThat(plan.get("api").asText()).isEqualTo(API);
        assertThat(plan.get("description").asText()).isEqualTo("JWT plan description");
        assertThat(plan.get("characteristics")).hasSize(1);
        assertThat(plan.get("order").asInt()).isEqualTo(2);
        assertThat(plan.get("comment_required").asBoolean()).isTrue();
        assertThat(plan.get("comment_message").asText()).isEqualTo("Why do you need it?");
        assertThat(plan.get("general_conditions").asText()).isEqualTo("conditions-page");
        assertThat(plan.path("flows")).isEmpty();
        assertThat(plan.path("paths")).isEmpty();
        assertAbsent(
            plan,
            "securityDefinition",
            "tags",
            "selection_rule",
            "excluded_groups",
            "crossId",
            "hrid",
            "environmentId",
            "created_at",
            "updated_at",
            "published_at",
            "closed_at"
        );
    }

    private static void assertAbsent(JsonNode plan, String... fields) {
        for (String field : fields) {
            assertThat(plan.path(field).isMissingNode() || plan.path(field).isNull()).as(field).isTrue();
        }
    }

    private void withoutPermission(RolePermission permission) {
        when(
            permissionService.hasPermission(any(ExecutionContext.class), eq(permission), eq(API), any(RolePermissionAction[].class))
        ).thenReturn(false);
    }

    private void withoutAnyPermission() {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(false);
    }

    private void withOnlyPlanAndGatewayDefinitionRead() {
        withoutAnyPermission();
        withReadPermission(RolePermission.API_PLAN);
        withReadPermission(RolePermission.API_GATEWAY_DEFINITION);
    }

    private void withPlanUpdatePermission() {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                eq(RolePermission.API_PLAN),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);
    }

    private void notExposedInPortal() {
        when(accessControlService.canConsumeApiFromPortal(any(ExecutionContext.class), any(ApiEntity.class))).thenReturn(false);
    }

    private void withReadPermission(RolePermission permission) {
        when(
            permissionService.hasPermission(any(ExecutionContext.class), eq(permission), eq(API), eq(RolePermissionAction.READ))
        ).thenReturn(true);
    }

    private void withoutPlanOrLogPermission() {
        withoutPermission(RolePermission.API_PLAN);
        withoutPermission(RolePermission.API_LOG);
    }

    private static io.gravitee.rest.api.model.v4.api.ApiEntity v4Api(DefinitionVersion definitionVersion) {
        io.gravitee.rest.api.model.v4.api.ApiEntity api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId(API);
        api.setDefinitionVersion(definitionVersion);
        return api;
    }

    private void mockReadableV2Api() {
        ApiEntity v2Api = readableV2Api();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(v2Api);
        when(apiService.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(v2Api);
    }

    private WebTarget planTarget() {
        return envTarget().path(API).path("plans").path(PLAN);
    }

    private static ApiEntity readableV2Api() {
        ApiEntity api = new ApiEntity();
        api.setId(API);
        api.setVisibility(Visibility.PUBLIC);
        api.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        return api;
    }

    private ApiEntity getApi(DefinitionVersion version) {
        ApiEntity api = new ApiEntity();
        api.setId(API);
        api.setGraviteeDefinitionVersion(version.getLabel());

        if (DefinitionVersion.V2.equals(version)) {
            PlanEntity plan1 = new PlanEntity();
            plan1.setId(PLAN);

            PlanEntity plan2 = new PlanEntity();
            plan2.setId("plan-2");
            api.setPlans(Set.of(plan1, plan2));
        }

        return api;
    }
}
