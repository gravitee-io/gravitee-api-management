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
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.rest.dto.AuthzCascadeResponse;
import io.gravitee.gamma.authorization.rest.dto.AuthzEntityRequest;
import io.gravitee.gamma.authorization.rest.dto.AuthzEntityResponse;
import io.gravitee.gamma.authorization.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authorization.rest.dto.UpdateAuthzEntityRequest;
import io.gravitee.gamma.authorization.service.AuthzCascadeResult;
import io.gravitee.gamma.authorization.service.AuthzEntityFilter;
import io.gravitee.gamma.authorization.service.AuthzUpsertResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityNotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthzEntitiesResourceTest extends AbstractAuthorizationResourceTest {

    private static final String ENV = "test-env";

    @Test
    void post_creates_new_entity_and_returns_201() {
        AuthzEntity created = entity("api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim");
        when(entityService.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceAuthzEntityCommand.class))).thenReturn(
            new AuthzUpsertResult(created, true)
        );

        try (
            Response response = target("/entities")
                .request()
                .post(
                    jakarta.ws.rs.client.Entity.json(
                        new AuthzEntityRequest("api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim")
                    )
                )
        ) {
            assertThat(response.getStatus()).isEqualTo(201);
            AuthzEntityResponse body = response.readEntity(AuthzEntityResponse.class);
            assertThat(body.entityId()).isEqualTo("api.123");
            assertThat(body.attributes()).containsEntry("k", "v");
        }
    }

    @Test
    void post_replaces_existing_entity_and_returns_200() {
        AuthzEntity replaced = entity("api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim");
        when(entityService.upsert(any(), any())).thenReturn(new AuthzUpsertResult(replaced, false));

        try (
            Response response = target("/entities")
                .request()
                .post(
                    jakarta.ws.rs.client.Entity.json(
                        new AuthzEntityRequest("api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim")
                    )
                )
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzEntityResponse body = response.readEntity(AuthzEntityResponse.class);
            assertThat(body.attributes()).containsEntry("k", "v2");
            assertThat(body.parents()).containsExactly("api.parent");
        }
    }

    @Test
    void post_with_blank_entityId_is_rejected_by_bean_validation() {
        try (
            Response response = target("/entities")
                .request()
                .post(jakarta.ws.rs.client.Entity.json(new AuthzEntityRequest("", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim")))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    void post_with_uppercase_entityId_is_rejected_by_bean_validation() {
        try (
            Response response = target("/entities")
                .request()
                .post(
                    jakarta.ws.rs.client.Entity.json(
                        new AuthzEntityRequest("api.MyApi", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim")
                    )
                )
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    void get_by_entityId_returns_entity() {
        AuthzEntity stored = entity("api.123", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim");
        when(entityService.findByEntityId(ENV, "api.123")).thenReturn(Optional.of(stored));

        try (Response response = target("/entities/api.123").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(AuthzEntityResponse.class).entityId()).isEqualTo("api.123");
        }
    }

    @Test
    void get_by_entityId_unknown_returns_404() {
        when(entityService.findByEntityId(ENV, "api.missing")).thenReturn(Optional.empty());

        try (Response response = target("/entities/api.missing").request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "EntityNotFound");
        }
    }

    @Test
    void get_list_filters_by_kind() {
        List<AuthzEntity> page = List.of(entity("idp.am.alice", AuthzEntityKind.PRINCIPAL, Map.of(), List.of(), "gravitee_am_default"));
        when(
            entityService.findPage(eq(ENV), eq(new AuthzEntityFilter(AuthzEntityKind.PRINCIPAL, null, null)), any(Pageable.class))
        ).thenReturn(new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE));

        try (Response response = target("/entities").queryParam("kind", "PRINCIPAL").request().get()) {
            PagedResponseDto<AuthzEntityResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).extracting(AuthzEntityResponse::entityId).containsExactly("idp.am.alice");
        }
    }

    @Test
    void get_list_filters_by_source() {
        List<AuthzEntity> page = List.of(entity("api.123", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim"));
        when(entityService.findPage(eq(ENV), eq(new AuthzEntityFilter(null, "apim", null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/entities").queryParam("source", "apim").request().get()) {
            PagedResponseDto<AuthzEntityResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).extracting(AuthzEntityResponse::entityId).containsExactly("api.123");
        }
    }

    @Test
    void get_list_filters_by_entityIdPrefix() {
        List<AuthzEntity> page = List.of(
            entity("api.123", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim"),
            entity("api.123.tool-a", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim")
        );
        when(entityService.findPage(eq(ENV), eq(new AuthzEntityFilter(null, null, "api.123")), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/entities").queryParam("entityIdPrefix", "api.123").request().get()) {
            PagedResponseDto<AuthzEntityResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).extracting(AuthzEntityResponse::entityId).containsExactlyInAnyOrder("api.123", "api.123.tool-a");
        }
    }

    @Test
    void put_updates_attributes_and_parents_only() {
        AuthzEntity updated = entity("api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim");
        when(entityService.update(any(), eq("api.123"), any(UpdateAuthzEntityCommand.class))).thenReturn(updated);

        try (
            Response response = target("/entities/api.123")
                .request()
                .put(jakarta.ws.rs.client.Entity.json(new UpdateAuthzEntityRequest(Map.of("k", "v2"), List.of("api.parent"))))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzEntityResponse body = response.readEntity(AuthzEntityResponse.class);
            assertThat(body.attributes()).containsEntry("k", "v2");
            assertThat(body.parents()).containsExactly("api.parent");
            assertThat(body.source()).isEqualTo("apim");
        }
    }

    @Test
    void put_unknown_entityId_returns_404() {
        when(entityService.update(any(), eq("api.missing"), any())).thenThrow(new AuthzEntityNotFoundException(ENV, "api.missing"));

        try (
            Response response = target("/entities/api.missing")
                .request()
                .put(jakarta.ws.rs.client.Entity.json(new UpdateAuthzEntityRequest(Map.of(), List.of())))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void delete_returns_cascade_summary() {
        when(entityService.delete(any(), eq("api.bookings"))).thenReturn(
            new AuthzCascadeResult(List.of("api.bookings", "mcp.bookings.tool-1"), List.of("policy-1"))
        );

        try (Response response = target("/entities/api.bookings").request().delete()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzCascadeResponse body = response.readEntity(AuthzCascadeResponse.class);
            assertThat(body.deletedEntityIds()).containsExactlyInAnyOrder("api.bookings", "mcp.bookings.tool-1");
            assertThat(body.deletedPolicyIds()).hasSize(1);
            assertThat(body.totalAffected()).isEqualTo(3);
        }
    }

    private static AuthzEntity entity(
        String entityId,
        AuthzEntityKind kind,
        Map<String, Object> attributes,
        List<String> parents,
        String source
    ) {
        Instant now = Instant.parse("2026-05-14T10:00:00Z");
        return new AuthzEntity("id-" + entityId, entityId, kind, attributes, parents, source, ENV, now, now);
    }
}
