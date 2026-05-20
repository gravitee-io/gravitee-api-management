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
package io.gravitee.gamma.authorization.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.rest.dto.AuthzPolicyRequest;
import io.gravitee.gamma.authorization.rest.dto.AuthzPolicyResponse;
import io.gravitee.gamma.authorization.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authorization.rest.dto.UpdateAuthzPolicyRequest;
import io.gravitee.gamma.authorization.service.AuthzPolicyFilter;
import io.gravitee.gamma.authorization.service.CreateAuthzPolicyCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzPolicyCommand;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityIdValidationCode;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidEntityIdException;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidStatusTransitionException;
import io.gravitee.gamma.authorization.service.exception.AuthzPolicyNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthzPoliciesResourceTest extends AbstractAuthorizationResourceTest {

    private static final String ENV = "env-1";

    @Test
    void post_creates_policy_and_returns_201() {
        AuthzPolicy created = policy("id-1", "global-1", AuthzPolicyKind.GLOBAL, null, "permit", AuthzPolicyStatus.DRAFT);
        when(policyService.create(any(AuthzCallerContext.class), any(CreateAuthzPolicyCommand.class))).thenReturn(created);

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new AuthzPolicyRequest("global-1", AuthzPolicyKind.GLOBAL, null, "permit")))
        ) {
            assertThat(response.getStatus()).isEqualTo(201);
            AuthzPolicyResponse body = response.readEntity(AuthzPolicyResponse.class);
            assertThat(body.id()).isEqualTo("id-1");
            assertThat(body.name()).isEqualTo("global-1");
            assertThat(body.kind()).isEqualTo(AuthzPolicyKind.GLOBAL);
            assertThat(body.status()).isEqualTo(AuthzPolicyStatus.DRAFT);
            assertThat(body.environmentId()).isEqualTo(ENV);
        }
    }

    @Test
    void post_resource_without_entityId_returns_400_with_REQUIRED_FOR_RESOURCE_code() {
        when(policyService.create(any(), any())).thenThrow(
            new AuthzInvalidEntityIdException(AuthzEntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE, "entityId required")
        );

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new AuthzPolicyRequest("r", AuthzPolicyKind.RESOURCE, null, "")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "ENTITY_ID_REQUIRED_FOR_RESOURCE").containsKey("message");
        }
    }

    @Test
    void post_resource_with_malformed_entityId_returns_400_with_MALFORMED_code() {
        when(policyService.create(any(), any())).thenThrow(
            new AuthzInvalidEntityIdException(AuthzEntityIdValidationCode.ENTITY_ID_MALFORMED, "entityId malformed")
        );

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new AuthzPolicyRequest("r", AuthzPolicyKind.RESOURCE, "api.MyApi", "")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "ENTITY_ID_MALFORMED");
        }
    }

    @Test
    void post_global_with_non_null_entityId_returns_400_with_FORBIDDEN_ON_GLOBAL_code() {
        when(policyService.create(any(), any())).thenThrow(
            new AuthzInvalidEntityIdException(AuthzEntityIdValidationCode.ENTITY_ID_FORBIDDEN_ON_GLOBAL, "forbidden on global")
        );

        try (
            Response response = target("/environments/" + ENV + "/policies")
                .request()
                .post(Entity.json(new AuthzPolicyRequest("g", AuthzPolicyKind.GLOBAL, "api.x", "")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "ENTITY_ID_FORBIDDEN_ON_GLOBAL");
        }
    }

    @Test
    void get_list_returns_all_policies_when_no_filter() {
        List<AuthzPolicy> page = List.of(
            policy("a", "g1", AuthzPolicyKind.GLOBAL, null, "", AuthzPolicyStatus.DRAFT),
            policy("b", "r1", AuthzPolicyKind.RESOURCE, "api-1", "", AuthzPolicyStatus.DRAFT)
        );
        when(policyService.findPage(eq(ENV), eq(new AuthzPolicyFilter(null, null, null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/policies").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            PagedResponseDto<AuthzPolicyResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).hasSize(2);
        }
    }

    @Test
    void get_list_filters_by_kind() {
        List<AuthzPolicy> page = List.of(policy("a", "g1", AuthzPolicyKind.GLOBAL, null, "", AuthzPolicyStatus.DRAFT));
        when(
            policyService.findPage(eq(ENV), eq(new AuthzPolicyFilter(AuthzPolicyKind.GLOBAL, null, null)), any(Pageable.class))
        ).thenReturn(new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE));

        try (Response response = target("/environments/" + ENV + "/policies").queryParam("kind", "GLOBAL").request().get()) {
            PagedResponseDto<AuthzPolicyResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).hasSize(1).first().extracting(AuthzPolicyResponse::kind).isEqualTo(AuthzPolicyKind.GLOBAL);
        }
    }

    @Test
    void get_list_filters_by_entity_id() {
        List<AuthzPolicy> page = List.of(policy("a", "r1", AuthzPolicyKind.RESOURCE, "api-1", "", AuthzPolicyStatus.DRAFT));
        when(policyService.findPage(eq(ENV), eq(new AuthzPolicyFilter(null, "api-1", null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/policies").queryParam("entityId", "api-1").request().get()) {
            PagedResponseDto<AuthzPolicyResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).hasSize(1).first().extracting(AuthzPolicyResponse::entityId).isEqualTo("api-1");
        }
    }

    @Test
    void get_by_id_returns_single_policy() {
        AuthzPolicy stored = policy("id-1", "g1", AuthzPolicyKind.GLOBAL, null, "", AuthzPolicyStatus.DRAFT);
        when(policyService.findById(ENV, "id-1")).thenReturn(Optional.of(stored));

        try (Response response = target("/environments/" + ENV + "/policies/id-1").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzPolicyResponse body = response.readEntity(AuthzPolicyResponse.class);
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
        AuthzPolicy updated = policy("id-1", "renamed", AuthzPolicyKind.GLOBAL, null, "new", AuthzPolicyStatus.DRAFT);
        when(policyService.update(any(), eq("id-1"), any(UpdateAuthzPolicyCommand.class))).thenReturn(updated);

        try (
            Response response = target("/environments/" + ENV + "/policies/id-1")
                .request()
                .put(Entity.json(new UpdateAuthzPolicyRequest("renamed", "new")))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzPolicyResponse body = response.readEntity(AuthzPolicyResponse.class);
            assertThat(body.name()).isEqualTo("renamed");
            assertThat(body.policyText()).isEqualTo("new");
            assertThat(body.status()).isEqualTo(AuthzPolicyStatus.DRAFT);
        }
    }

    @Test
    void put_unknown_returns_404() {
        when(policyService.update(any(), eq("missing"), any())).thenThrow(new AuthzPolicyNotFoundException(ENV, "missing"));

        try (
            Response response = target("/environments/" + ENV + "/policies/missing")
                .request()
                .put(Entity.json(new UpdateAuthzPolicyRequest("x", null)))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void post_deploy_transitions_draft_to_deployed_returns_200() {
        AuthzPolicy deployed = policy("id-1", "g1", AuthzPolicyKind.GLOBAL, null, "", AuthzPolicyStatus.DEPLOYED);
        when(policyService.deploy(any(), eq("id-1"))).thenReturn(deployed);

        try (Response response = target("/environments/" + ENV + "/policies/id-1/deploy").request().post(Entity.json(""))) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzPolicyResponse body = response.readEntity(AuthzPolicyResponse.class);
            assertThat(body.id()).isEqualTo("id-1");
            assertThat(body.status()).isEqualTo(AuthzPolicyStatus.DEPLOYED);
        }
    }

    @Test
    void post_deploy_unknown_returns_404() {
        when(policyService.deploy(any(), eq("missing"))).thenThrow(new AuthzPolicyNotFoundException(ENV, "missing"));

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
        AuthzPolicy disabled = policy("id-1", "g1", AuthzPolicyKind.GLOBAL, null, "", AuthzPolicyStatus.DISABLED);
        when(policyService.disable(any(), eq("id-1"))).thenReturn(disabled);

        try (Response response = target("/environments/" + ENV + "/policies/id-1/disable").request().post(Entity.json(""))) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzPolicyResponse body = response.readEntity(AuthzPolicyResponse.class);
            assertThat(body.status()).isEqualTo(AuthzPolicyStatus.DISABLED);
        }
    }

    @Test
    void post_disable_invalid_transition_returns_409() {
        when(policyService.disable(any(), eq("id-1"))).thenThrow(
            new AuthzInvalidStatusTransitionException(AuthzPolicyStatus.DRAFT, AuthzPolicyStatus.DISABLED)
        );

        try (Response response = target("/environments/" + ENV + "/policies/id-1/disable").request().post(Entity.json(""))) {
            assertThat(response.getStatus()).isEqualTo(409);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsKey("error").containsKey("message");
        }
    }

    @Test
    void post_disable_unknown_returns_404() {
        when(policyService.disable(any(), eq("missing"))).thenThrow(new AuthzPolicyNotFoundException(ENV, "missing"));

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

    private static AuthzPolicy policy(
        String id,
        String name,
        AuthzPolicyKind kind,
        String entityId,
        String text,
        AuthzPolicyStatus status
    ) {
        Instant now = Instant.parse("2026-05-14T10:00:00Z");
        return new AuthzPolicy(id, name, kind, entityId, text, status, ENV, now, now);
    }
}
