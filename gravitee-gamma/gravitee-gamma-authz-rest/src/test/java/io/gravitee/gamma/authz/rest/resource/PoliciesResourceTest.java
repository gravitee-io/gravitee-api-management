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
package io.gravitee.gamma.authz.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.domain.PolicyStatus;
import io.gravitee.gamma.authorization.service.CreatePolicyCommand;
import io.gravitee.gamma.authorization.service.PolicyFilter;
import io.gravitee.gamma.authorization.service.UpdatePolicyCommand;
import io.gravitee.gamma.authorization.service.exception.EntityIdValidationCode;
import io.gravitee.gamma.authorization.service.exception.InvalidEntityIdException;
import io.gravitee.gamma.authorization.service.exception.InvalidStatusTransitionException;
import io.gravitee.gamma.authorization.service.exception.PolicyNotFoundException;
import io.gravitee.gamma.authz.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authz.rest.dto.PolicyRequest;
import io.gravitee.gamma.authz.rest.dto.PolicyResponse;
import io.gravitee.gamma.authz.rest.dto.UpdatePolicyRequest;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PoliciesResourceTest extends AbstractAuthorizationResourceTest {

    private static final String ENV = "env-1";

    @Test
    void post_creates_policy_and_returns_201() {
        Policy created = policy("id-1", "global-1", PolicyKind.GLOBAL, null, "permit", PolicyStatus.DRAFT);
        when(policyService.create(any(AuthzCallerContext.class), any(CreatePolicyCommand.class))).thenReturn(created);

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new PolicyRequest("global-1", PolicyKind.GLOBAL, null, "permit")))
        ) {
            assertThat(response.getStatus()).isEqualTo(201);
            PolicyResponse body = response.readEntity(PolicyResponse.class);
            assertThat(body.id()).isEqualTo("id-1");
            assertThat(body.name()).isEqualTo("global-1");
            assertThat(body.kind()).isEqualTo(PolicyKind.GLOBAL);
            assertThat(body.status()).isEqualTo(PolicyStatus.DRAFT);
            assertThat(body.environmentId()).isEqualTo(ENV);
        }
    }

    @Test
    void post_resource_without_entityId_returns_400_with_REQUIRED_FOR_RESOURCE_code() {
        when(policyService.create(any(), any())).thenThrow(
            new InvalidEntityIdException(EntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE, "entityId required")
        );

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new PolicyRequest("r", PolicyKind.RESOURCE, null, "")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "ENTITY_ID_REQUIRED_FOR_RESOURCE").containsKey("message");
        }
    }

    @Test
    void post_resource_with_malformed_entityId_returns_400_with_MALFORMED_code() {
        when(policyService.create(any(), any())).thenThrow(
            new InvalidEntityIdException(EntityIdValidationCode.ENTITY_ID_MALFORMED, "entityId malformed")
        );

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new PolicyRequest("r", PolicyKind.RESOURCE, "api.MyApi", "")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "ENTITY_ID_MALFORMED");
        }
    }

    @Test
    void post_global_with_non_null_entityId_returns_400_with_FORBIDDEN_ON_GLOBAL_code() {
        when(policyService.create(any(), any())).thenThrow(
            new InvalidEntityIdException(EntityIdValidationCode.ENTITY_ID_FORBIDDEN_ON_GLOBAL, "forbidden on global")
        );

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new PolicyRequest("g", PolicyKind.GLOBAL, "api.x", "")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "ENTITY_ID_FORBIDDEN_ON_GLOBAL");
        }
    }

    @Test
    void get_list_returns_all_policies_when_no_filter() {
        List<Policy> page = List.of(
            policy("a", "g1", PolicyKind.GLOBAL, null, "", PolicyStatus.DRAFT),
            policy("b", "r1", PolicyKind.RESOURCE, "api-1", "", PolicyStatus.DRAFT)
        );
        when(policyService.findPage(eq(ENV), eq(new PolicyFilter(null, null, null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/policies").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            PagedResponseDto<PolicyResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).hasSize(2);
        }
    }

    @Test
    void get_list_filters_by_kind() {
        List<Policy> page = List.of(policy("a", "g1", PolicyKind.GLOBAL, null, "", PolicyStatus.DRAFT));
        when(policyService.findPage(eq(ENV), eq(new PolicyFilter(PolicyKind.GLOBAL, null, null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/policies").queryParam("kind", "GLOBAL").request().get()) {
            PagedResponseDto<PolicyResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).hasSize(1).first().extracting(PolicyResponse::kind).isEqualTo(PolicyKind.GLOBAL);
        }
    }

    @Test
    void get_list_filters_by_entity_id() {
        List<Policy> page = List.of(policy("a", "r1", PolicyKind.RESOURCE, "api-1", "", PolicyStatus.DRAFT));
        when(policyService.findPage(eq(ENV), eq(new PolicyFilter(null, "api-1", null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/policies").queryParam("entityId", "api-1").request().get()) {
            PagedResponseDto<PolicyResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).hasSize(1).first().extracting(PolicyResponse::entityId).isEqualTo("api-1");
        }
    }

    @Test
    void get_by_id_returns_single_policy() {
        Policy stored = policy("id-1", "g1", PolicyKind.GLOBAL, null, "", PolicyStatus.DRAFT);
        when(policyService.findById(ENV, "id-1")).thenReturn(Optional.of(stored));

        try (Response response = target("/environments/" + ENV + "/policies/id-1").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            PolicyResponse body = response.readEntity(PolicyResponse.class);
            assertThat(body.id()).isEqualTo("id-1");
        }
    }

    @Test
    void get_by_id_unknown_returns_404() {
        when(policyService.findById(ENV, "missing")).thenReturn(Optional.empty());

        try (Response response = target("/environments/" + ENV + "/policies/missing").request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void put_updates_name_and_text_only() {
        Policy updated = policy("id-1", "renamed", PolicyKind.GLOBAL, null, "new", PolicyStatus.DRAFT);
        when(policyService.update(any(), eq("id-1"), any(UpdatePolicyCommand.class))).thenReturn(updated);

        try (
            Response response = target("/environments/" + ENV + "/policies/id-1")
                .request()
                .put(Entity.json(new UpdatePolicyRequest("renamed", "new")))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            PolicyResponse body = response.readEntity(PolicyResponse.class);
            assertThat(body.name()).isEqualTo("renamed");
            assertThat(body.policyText()).isEqualTo("new");
            assertThat(body.status()).isEqualTo(PolicyStatus.DRAFT);
        }
    }

    @Test
    void put_unknown_returns_404() {
        when(policyService.update(any(), eq("missing"), any())).thenThrow(new PolicyNotFoundException(ENV, "missing"));

        try (
            Response response = target("/environments/" + ENV + "/policies/missing")
                .request()
                .put(Entity.json(new UpdatePolicyRequest("x", null)))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void post_deploy_transitions_draft_to_deployed_returns_200() {
        Policy deployed = policy("id-1", "g1", PolicyKind.GLOBAL, null, "", PolicyStatus.DEPLOYED);
        when(policyService.deploy(any(), eq("id-1"))).thenReturn(deployed);

        try (Response response = target("/environments/" + ENV + "/policies/id-1/deploy").request().post(Entity.json(""))) {
            assertThat(response.getStatus()).isEqualTo(200);
            PolicyResponse body = response.readEntity(PolicyResponse.class);
            assertThat(body.id()).isEqualTo("id-1");
            assertThat(body.status()).isEqualTo(PolicyStatus.DEPLOYED);
        }
    }

    @Test
    void post_deploy_unknown_returns_404() {
        when(policyService.deploy(any(), eq("missing"))).thenThrow(new PolicyNotFoundException(ENV, "missing"));

        try (
            Response response = target("/environments/" + ENV + "/policies/missing/deploy")
                .request()
                .post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void post_disable_transitions_deployed_to_disabled_returns_200() {
        Policy disabled = policy("id-1", "g1", PolicyKind.GLOBAL, null, "", PolicyStatus.DISABLED);
        when(policyService.disable(any(), eq("id-1"))).thenReturn(disabled);

        try (Response response = target("/environments/" + ENV + "/policies/id-1/disable").request().post(Entity.json(""))) {
            assertThat(response.getStatus()).isEqualTo(200);
            PolicyResponse body = response.readEntity(PolicyResponse.class);
            assertThat(body.status()).isEqualTo(PolicyStatus.DISABLED);
        }
    }

    @Test
    void post_disable_invalid_transition_returns_409() {
        when(policyService.disable(any(), eq("id-1"))).thenThrow(
            new InvalidStatusTransitionException(PolicyStatus.DRAFT, PolicyStatus.DISABLED)
        );

        try (Response response = target("/environments/" + ENV + "/policies/id-1/disable").request().post(Entity.json(""))) {
            assertThat(response.getStatus()).isEqualTo(409);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsKey("error").containsKey("message");
        }
    }

    @Test
    void post_disable_unknown_returns_404() {
        when(policyService.disable(any(), eq("missing"))).thenThrow(new PolicyNotFoundException(ENV, "missing"));

        try (
            Response response = target("/environments/" + ENV + "/policies/missing/disable")
                .request()
                .post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void delete_returns_204_when_deleted() {
        when(policyService.delete(any(), eq("id-1"))).thenReturn(true);

        try (Response response = target("/environments/" + ENV + "/policies/id-1").request().delete()) {
            assertThat(response.getStatus()).isEqualTo(204);
        }
    }

    @Test
    void delete_returns_204_when_not_found() {
        when(policyService.delete(any(), eq("missing"))).thenReturn(false);

        try (Response response = target("/environments/" + ENV + "/policies/missing").request().delete()) {
            assertThat(response.getStatus()).isEqualTo(204);
        }
    }

    private static Policy policy(String id, String name, PolicyKind kind, String entityId, String text, PolicyStatus status) {
        Instant now = Instant.parse("2026-05-14T10:00:00Z");
        return new Policy(id, name, kind, entityId, text, status, ENV, now, now);
    }
}
