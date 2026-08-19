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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.plugin.gamma.api.automation.AutomationContext;
import io.gravitee.apim.plugin.gamma.api.automation.AutomationIssue;
import io.gravitee.apim.plugin.gamma.api.automation.GammaAutomationPort;
import io.gravitee.apim.plugin.gamma.api.automation.ResourceKind;
import io.gravitee.apim.plugin.gamma.api.automation.UpsertResult;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.apim.rest.api.automation.spring.GammaAutomationPorts;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GammaModuleAutomationResourceTest extends AbstractResourceTest {

    private static final String MODULE = "aim";
    private static final String KIND_PATH = "catalog/mcp-servers";
    private static final String LICENSE_FEATURE = "gamma-aim-module";
    private static final ResourceKind KIND = new ResourceKind(KIND_PATH, RolePermission.ENVIRONMENT_AI_CATALOG);
    private static final ExecutionContext CONTEXT = new ExecutionContext(ORGANIZATION, ENVIRONMENT);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private GammaAutomationPorts gammaAutomationPorts;

    @Inject
    private GammaAutomationPort port;

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT;
    }

    @BeforeEach
    void setUpModule() {
        when(gammaAutomationPorts.module(MODULE)).thenReturn(Optional.of(port));
        when(port.module()).thenReturn(MODULE);
        when(port.licenseFeature()).thenReturn(Optional.of(LICENSE_FEATURE));
        when(port.kind(KIND_PATH)).thenReturn(Optional.of(KIND));
        license = mock(License.class);
        when(license.isFeatureEnabled(LICENSE_FEATURE)).thenReturn(true);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @AfterEach
    void tearDownModule() {
        reset(gammaAutomationPorts, port, licenseManager);
    }

    private static String derivedId(String kindPath, String hrid) {
        return HRIDToUUID.gamma().context(CONTEXT).module(MODULE).kind(kindPath).hrid(hrid).id();
    }

    private static ObjectNode view(String hrid) {
        return MAPPER.createObjectNode().put("hrid", hrid).put("entityId", "mcp-server.github").put("protocolVersion", "2024-11-05");
    }

    private Response put(String path, String fixture, boolean dryRun) {
        var target = rootTarget(path);
        if (dryRun) {
            target = target.queryParam("dryRun", true);
        }
        return target.request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON(fixture)));
    }

    private JsonNode body(Response response) {
        try {
            return MAPPER.readTree(response.readEntity(String.class));
        } catch (Exception e) {
            throw new AssertionError("response body is not JSON", e);
        }
    }

    @Nested
    class Routing {

        @Test
        void should_return_404_module_unavailable_when_no_module_answers_on_the_segment() {
            when(gammaAutomationPorts.module("nope")).thenReturn(Optional.empty());

            try (var response = rootTarget("nope/catalog/mcp-servers/github-mcp").request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
                var error = body(response);
                assertThat(error.get("technicalCode").asText()).isEqualTo("gamma.module.unavailable");
                assertThat(error.get("message").asText()).contains("nope");
            }
        }

        @Test
        void should_return_404_when_module_serves_nothing_at_the_path() {
            when(port.kind("catalog/unknown")).thenReturn(Optional.empty());

            try (var response = rootTarget(MODULE + "/catalog/unknown/x").request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
                assertThat(body(response).get("technicalCode").asText()).isEqualTo("gamma.resource.kind.notFound");
            }
        }

        @Test
        void should_leave_literal_sibling_paths_to_their_own_resources() {
            try (var response = rootTarget("dictionaries/some-dictionary").request().get()) {
                assertThat(response.getStatus()).isNotEqualTo(200);
                verify(gammaAutomationPorts, never()).module(anyString());
            }
        }
    }

    @Nested
    class DryRun {

        @Test
        void should_call_validate_with_the_derived_id_and_never_upsert() {
            when(port.validate(any(), eq(KIND), eq(derivedId(KIND_PATH, "github-mcp")), any())).thenReturn(
                UpsertResult.of(view("github-mcp"))
            );

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", true)) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(port).validate(any(), eq(KIND), eq(derivedId(KIND_PATH, "github-mcp")), any());
                verify(port, never()).upsert(any(), any(), any(), any());
            }
        }

        @Test
        void should_stamp_envelope_on_the_module_view() {
            when(port.validate(any(), any(), any(), any())).thenReturn(UpsertResult.of(view("github-mcp")));

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", true)) {
                var state = body(response);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.get("id").asText()).isEqualTo(derivedId(KIND_PATH, "github-mcp"));
                    soft.assertThat(state.get("environmentId").asText()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.get("organizationId").asText()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.get("hrid").asText()).isEqualTo("github-mcp");
                    soft.assertThat(state.get("protocolVersion").asText()).isEqualTo("2024-11-05");
                    soft.assertThat(state.has("errors")).isFalse();
                });
            }
        }

        @Test
        void should_return_200_with_errors_even_when_severe() {
            when(port.validate(any(), any(), any(), any())).thenReturn(
                new UpsertResult<>(
                    view("github-mcp"),
                    List.of(AutomationIssue.severe("entityId is taken"), AutomationIssue.warning("slow"))
                )
            );

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", true)) {
                assertThat(response.getStatus()).isEqualTo(200);
                var errors = body(response).get("errors");
                assertThat(errors.get("severe")).extracting(JsonNode::asText).containsExactly("entityId is taken");
                assertThat(errors.get("warning")).extracting(JsonNode::asText).containsExactly("slow");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec-without-hrid.json", true)) {
                assertThat(response.getStatus()).isEqualTo(400);
                verify(port, never()).validate(any(), any(), any(), any());
            }
        }

        @Test
        void should_return_400_when_hrid_is_invalid() {
            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec-invalid-hrid.json", true)) {
                assertThat(response.getStatus()).isEqualTo(400);
                verify(port, never()).validate(any(), any(), any(), any());
            }
        }

        @Test
        void should_hand_the_module_a_context_that_resolves_hrids_of_its_other_kinds() {
            when(port.validate(any(), any(), any(), any())).thenReturn(UpsertResult.of(view("github-mcp")));
            var context = ArgumentCaptor.forClass(AutomationContext.class);

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", true)) {
                assertThat(response.getStatus()).isEqualTo(200);
            }

            verify(port).validate(context.capture(), any(), any(), any());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(context.getValue().organizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(context.getValue().environmentId()).isEqualTo(ENVIRONMENT);
                soft
                    .assertThat(context.getValue().hrids().idOf("catalog/sources", "smithery"))
                    .isEqualTo(derivedId("catalog/sources", "smithery"));
            });
        }
    }

    @Nested
    class Run {

        @Test
        void should_return_200_with_stamped_state_on_success() {
            when(port.upsert(any(), eq(KIND), eq(derivedId(KIND_PATH, "github-mcp")), any())).thenReturn(
                UpsertResult.of(view("github-mcp"))
            );

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = body(response);
                assertThat(state.get("id").asText()).isEqualTo(derivedId(KIND_PATH, "github-mcp"));
                assertThat(state.get("hrid").asText()).isEqualTo("github-mcp");
                verify(port, never()).validate(any(), any(), any(), any());
            }
        }

        @Test
        void should_return_400_with_state_and_errors_when_severe() {
            when(port.upsert(any(), any(), any(), any())).thenReturn(
                new UpsertResult<>(view("github-mcp"), List.of(AutomationIssue.severe("entityId is taken")))
            );

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(400);
                var state = body(response);
                assertThat(state.get("id").asText()).isEqualTo(derivedId(KIND_PATH, "github-mcp"));
                assertThat(state.get("errors").get("severe")).extracting(JsonNode::asText).containsExactly("entityId is taken");
            }
        }

        @Test
        void should_return_200_with_warnings() {
            when(port.upsert(any(), any(), any(), any())).thenReturn(
                new UpsertResult<>(view("github-mcp"), List.of(AutomationIssue.warning("discovery failed")))
            );

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(body(response).get("errors").get("warning")).extracting(JsonNode::asText).containsExactly("discovery failed");
            }
        }
    }

    @Nested
    class Guards {

        @Test
        void should_return_403_when_license_lacks_the_module_feature() {
            when(license.isFeatureEnabled(LICENSE_FEATURE)).thenReturn(false);

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(403);
                assertThat(body(response).get("technicalCode").asText()).isEqualTo("feature.missing");
                verify(port, never()).upsert(any(), any(), any(), any());
            }
        }

        @Test
        void should_skip_license_check_when_module_declares_no_feature() {
            when(port.licenseFeature()).thenReturn(Optional.empty());
            when(port.upsert(any(), any(), any(), any())).thenReturn(UpsertResult.of(view("github-mcp")));

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(license);
            }
        }

        @Test
        void should_return_403_and_never_call_the_module_when_permission_is_denied() {
            when(
                permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_AI_CATALOG), any(), eq(CREATE), eq(UPDATE))
            ).thenReturn(false);

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(403);
                verify(port, never()).upsert(any(), any(), any(), any());
                verify(port, never()).validate(any(), any(), any(), any());
            }
        }

        @Test
        void should_check_read_acl_on_get_and_delete_acl_on_delete() {
            clearInvocations(permissionService);
            when(port.findById(any(), any(), any())).thenReturn(Optional.of(view("github-mcp")));
            when(port.deleteById(any(), any(), any())).thenReturn(true);

            try (var response = rootTarget(MODULE + "/" + KIND_PATH + "/github-mcp").request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
            }
            try (var response = rootTarget(MODULE + "/" + KIND_PATH + "/github-mcp").request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }

            verify(permissionService).hasPermission(any(), eq(RolePermission.ENVIRONMENT_AI_CATALOG), eq(ENVIRONMENT), eq(READ));
            verify(permissionService).hasPermission(any(), eq(RolePermission.ENVIRONMENT_AI_CATALOG), eq(ENVIRONMENT), eq(DELETE));
        }

        @Test
        void should_fail_loudly_when_module_declares_a_permission_that_is_not_environment_scoped() {
            when(port.kind(KIND_PATH)).thenReturn(Optional.of(new ResourceKind(KIND_PATH, RolePermission.API_DEFINITION)));

            try (var response = put(MODULE + "/" + KIND_PATH, "module-spec.json", false)) {
                assertThat(response.getStatus()).isEqualTo(500);
                verify(port, never()).upsert(any(), any(), any(), any());
            }
        }
    }

    @Nested
    class Get {

        @Test
        void should_return_stamped_state() {
            // The module's view carries no hrid: the resource stamps it from the path, so modules never store it.
            var viewWithoutHrid = view("github-mcp");
            viewWithoutHrid.remove("hrid");
            when(port.findById(any(), eq(KIND), eq(derivedId(KIND_PATH, "github-mcp")))).thenReturn(Optional.of(viewWithoutHrid));

            try (var response = rootTarget(MODULE + "/" + KIND_PATH + "/github-mcp").request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = body(response);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.get("id").asText()).isEqualTo(derivedId(KIND_PATH, "github-mcp"));
                    soft.assertThat(state.get("environmentId").asText()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.get("organizationId").asText()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.get("hrid").asText()).isEqualTo("github-mcp");
                    soft.assertThat(state.has("errors")).isFalse();
                });
            }
        }

        @Test
        void should_return_404_hrid_not_found_when_absent() {
            when(port.findById(any(), any(), any())).thenReturn(Optional.empty());

            try (var response = rootTarget(MODULE + "/" + KIND_PATH + "/unknown").request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
                assertThat(body(response).get("technicalCode").asText()).isEqualTo("hrid.notFound");
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_return_204_when_deleted() {
            when(port.deleteById(any(), eq(KIND), eq(derivedId(KIND_PATH, "github-mcp")))).thenReturn(true);

            try (var response = rootTarget(MODULE + "/" + KIND_PATH + "/github-mcp").request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }
        }

        @Test
        void should_return_404_when_absent() {
            when(port.deleteById(any(), any(), any())).thenReturn(false);

            try (var response = rootTarget(MODULE + "/" + KIND_PATH + "/unknown").request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
                assertThat(body(response).get("technicalCode").asText()).isEqualTo("hrid.notFound");
            }
        }
    }
}
