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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.PRECONDITION_FAILED_412;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.Yaml;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiPlansResource_PatchApiPlanTest extends AbstractResourceTest {

    static final String API = "my-api";
    static final String PLAN = "my-plan";
    static final String ENVIRONMENT = "my-env";

    private static final String MERGE_PATCH_TYPE = "application/merge-patch+json";
    private static final String JSON_PATCH_TYPE = "application/json-patch+json";

    @Autowired
    ApiCrudServiceInMemory apiCrudService;

    @Autowired
    PlanCrudServiceInMemory planCrudServiceInMemory;

    @Autowired
    UpdatePlanDomainService updatePlanDomainService;

    @Autowired
    PlanSearchService planSearchService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
    }

    @BeforeEach
    void init() throws Exception {
        var api = Api.builder().id(API).environmentId(ENVIRONMENT).build();
        when(apiRepository.findById(API)).thenReturn(Optional.of(api));

        var environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        var updatedAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1000), ZoneId.systemDefault());

        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API).updatedAt(updatedAt).build()));

        planCrudServiceInMemory.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id(PLAN)
                    .name("Original Name")
                    .referenceId(API)
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .updatedAt(updatedAt)
                    .build()
            )
        );

        reset(updatePlanDomainService);
        when(updatePlanDomainService.update(any(), any(), any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        stubPlanSearch(API, Date.from(updatedAt.toInstant()));
    }

    private void stubPlanSearch(String referenceId, Date updatedAt) {
        var planEntity = BasePlanEntity.builder()
            .id(PLAN)
            .referenceId(referenceId)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .updatedAt(updatedAt)
            .build();
        when(planSearchService.findById(any(), eq(PLAN))).thenReturn(planEntity);
    }

    private void seedPlanWithNullUpdatedAt() {
        planCrudServiceInMemory.reset();
        planCrudServiceInMemory.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id(PLAN)
                    .name("Original Name")
                    .referenceId(API)
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .updatedAt(null)
                    .build()
            )
        );
        stubPlanSearch(API, null);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        apiCrudService.reset();
        planCrudServiceInMemory.reset();
        GraviteeContext.cleanContext();
        reset(apiRepository);
    }

    @Test
    void should_return_200_and_updated_plan_for_json_patch_on_allow_listed_field() {
        var patchDoc = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Patched Name\"}]";

        Response response = rootTarget().path(PLAN).request().method("PATCH", Entity.entity(patchDoc, JSON_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PlanV4.class).getName()).isEqualTo("Patched Name");
    }

    @Test
    void should_return_200_and_updated_plan_for_merge_patch_on_allow_listed_field() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Patched Name\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PlanV4.class).getName()).isEqualTo("Patched Name");
        verify(updatePlanDomainService).update(any(), any(), any(), any(), any());
    }

    @Test
    void should_return_200_with_patched_flows_in_response_body() {
        var patchDoc = "{\"flows\":[{\"name\":\"Patched Flow\",\"enabled\":true}]}";

        Response response = rootTarget().path(PLAN).request().method("PATCH", Entity.entity(patchDoc, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        var flows = response.readEntity(PlanV4.class).getFlows();
        assertThat(flows).hasSize(1);
        assertThat(flows.get(0).getName()).isEqualTo("Patched Flow");
    }

    @Test
    void should_treat_application_json_content_type_as_merge_patch_and_return_200() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Patched Name\"}", "application/json"));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PlanV4.class).getName()).isEqualTo("Patched Name");
    }

    @Test
    void should_return_200_with_would_be_state_when_dry_run() {
        Response response = rootTarget()
            .path(PLAN)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Dry Run Name\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PlanV4.class).getName()).isEqualTo("Dry Run Name");
    }

    @Test
    void should_not_invoke_write_path_when_dry_run() {
        Response response = rootTarget()
            .path(PLAN)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Dry Run Name\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PlanV4.class).getName()).isEqualTo("Dry Run Name");
        verify(updatePlanDomainService).validate(any(), any(), any(), any());
        verify(updatePlanDomainService, never()).update(any(), any(), any(), any(), any());
    }

    @Test
    void should_return_400_when_patch_touches_status_field() {
        var patchDoc = "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"CLOSED\"}]";

        Response response = rootTarget().path(PLAN).request().method("PATCH", Entity.entity(patchDoc, JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_proceed_without_412_when_if_match_header_is_absent() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"No Precondition\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_412_when_if_match_header_does_not_match_plan_etag() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"wrong-etag\"")
            .method("PATCH", Entity.entity("{\"name\":\"Conflict\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_return_200_when_if_match_header_matches_plan_etag() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"1000\"")
            .method("PATCH", Entity.entity("{\"name\":\"Matched Precondition\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_400_when_plan_patch_is_attempted_on_v2_api() {
        apiCrudService.reset();
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2().toBuilder().id(API).build()));

        Response response = rootTarget().path(PLAN).request().method("PATCH", Entity.entity("{\"name\":\"Scoped Out\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_api_is_v4_message_type() {
        apiCrudService.reset();
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().id(API).build()));

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Message API Patch\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_404_when_plan_belongs_to_different_api() {
        planCrudServiceInMemory.reset();
        planCrudServiceInMemory.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id(PLAN)
                    .referenceId("ANOTHER-API")
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .build()
            )
        );
        stubPlanSearch("ANOTHER-API", null);

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Cross API Patch\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_404_not_412_when_plan_belongs_to_different_api_and_if_match_is_present() {
        planCrudServiceInMemory.reset();
        planCrudServiceInMemory.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id(PLAN)
                    .referenceId("ANOTHER-API")
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .updatedAt(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1000), ZoneId.systemDefault()))
                    .build()
            )
        );
        stubPlanSearch("ANOTHER-API", Date.from(Instant.ofEpochMilli(1000)));

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"wrong-etag\"")
            .method("PATCH", Entity.entity("{\"name\":\"Cross API Patch\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_200_when_if_match_is_wildcard() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "*")
            .method("PATCH", Entity.entity("{\"name\":\"Wildcard Match\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_200_when_if_match_is_weak_validator_matching_plan_etag() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "W/\"1000\"")
            .method("PATCH", Entity.entity("{\"name\":\"Weak Validator Match\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_200_when_if_match_has_gzip_suffix_matching_plan_etag() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"1000-gzip\"")
            .method("PATCH", Entity.entity("{\"name\":\"Gzip Suffix Match\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_412_for_malformed_if_match_header() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "not-an-etag-no-quotes")
            .method("PATCH", Entity.entity("{\"name\":\"Malformed ETag\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_return_412_when_if_match_header_is_empty_tag_set() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "-gzip")
            .method("PATCH", Entity.entity("{\"name\":\"Empty Tag Set\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_return_200_when_plan_has_null_updatedAt() {
        seedPlanWithNullUpdatedAt();

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Null UpdatedAt\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.getHeaderString(HttpHeaders.ETAG)).isNull();
        assertThat(response.getHeaderString(HttpHeaders.LAST_MODIFIED)).isNull();
    }

    @Test
    void should_return_412_when_if_match_header_is_present_and_plan_has_null_updatedAt() {
        seedPlanWithNullUpdatedAt();

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"1000\"")
            .method("PATCH", Entity.entity("{\"name\":\"Null UpdatedAt With Precondition\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_return_412_when_if_match_wildcard_is_present_and_plan_has_null_updatedAt() {
        seedPlanWithNullUpdatedAt();

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .header(HttpHeaders.IF_MATCH, "*")
            .method("PATCH", Entity.entity("{\"name\":\"Null UpdatedAt With Wildcard Precondition\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_include_etag_and_last_modified_headers_in_successful_patch_response() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Check Headers\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.getHeaderString(HttpHeaders.ETAG)).isEqualTo("\"1000\"");
        assertThat(response.getHeaderString(HttpHeaders.LAST_MODIFIED)).isNotNull();
    }

    @Test
    void should_treat_content_type_with_parameters_as_merge_patch() {
        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"Charset Patch\"}", "application/json; charset=utf-8"));

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PlanV4.class).getName()).isEqualTo("Charset Patch");
    }

    @Test
    void should_return_404_when_plan_has_api_product_reference_type_and_different_reference_id() {
        planCrudServiceInMemory.reset();
        planCrudServiceInMemory.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id(PLAN)
                    .referenceId("ANOTHER-API")
                    .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                    .build()
            )
        );
        var planEntity = BasePlanEntity.builder()
            .id(PLAN)
            .referenceId("ANOTHER-API")
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
        when(planSearchService.findById(any(), eq(PLAN))).thenReturn(planEntity);

        Response response = rootTarget()
            .path(PLAN)
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"API Product Patch\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_document_patch_operation_in_openapi_spec() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("openapi/openapi-apis.yaml");
        assertThat(stream).as("openapi-apis.yaml must be on the classpath").isNotNull();

        String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        Yaml parser = new Yaml();
        Map<String, Object> spec = parser.load(yaml);
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        Map<String, Object> planIdPath = (Map<String, Object>) paths.get("/environments/{envId}/apis/{apiId}/plans/{planId}");

        assertThat(planIdPath).as("/environments/{envId}/apis/{apiId}/plans/{planId} must define a 'patch' operation").containsKey("patch");
    }
}
