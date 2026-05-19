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
import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.service.CascadeResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.authorization.service.UpdateEntityCommand;
import io.gravitee.gamma.authorization.service.UpsertResult;
import io.gravitee.gamma.authorization.service.exception.EntityNotFoundException;
import io.gravitee.gamma.authz.rest.dto.CascadeResponse;
import io.gravitee.gamma.authz.rest.dto.EntityRequest;
import io.gravitee.gamma.authz.rest.dto.EntityResponse;
import io.gravitee.gamma.authz.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authz.rest.dto.UpdateEntityRequest;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EntitiesResourceTest extends AbstractAuthorizationResourceTest {

    private static final String ENV = "env-1";

    @Test
    void post_creates_new_entity_and_returns_201() {
        Entity created = entity("api.123", EntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim");
        when(entityService.upsert(any(AuthzCallerContext.class), any(CreateOrReplaceEntityCommand.class))).thenReturn(
            new UpsertResult(created, true)
        );

        try (
            Response response = target("/environments/" + ENV + "/entities")
                .request()
                .post(
                    jakarta.ws.rs.client.Entity.json(new EntityRequest("api.123", EntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim"))
                )
        ) {
            assertThat(response.getStatus()).isEqualTo(201);
            EntityResponse body = response.readEntity(EntityResponse.class);
            assertThat(body.entityId()).isEqualTo("api.123");
            assertThat(body.attributes()).containsEntry("k", "v");
        }
    }

    @Test
    void post_replaces_existing_entity_and_returns_200() {
        Entity replaced = entity("api.123", EntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim");
        when(entityService.upsert(any(), any())).thenReturn(new UpsertResult(replaced, false));

        try (
            Response response = target("/environments/" + ENV + "/entities")
                .request()
                .post(
                    jakarta.ws.rs.client.Entity.json(
                        new EntityRequest("api.123", EntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim")
                    )
                )
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            EntityResponse body = response.readEntity(EntityResponse.class);
            assertThat(body.attributes()).containsEntry("k", "v2");
            assertThat(body.parents()).containsExactly("api.parent");
        }
    }

    @Test
    void get_by_entityId_returns_entity() {
        Entity stored = entity("api.123", EntityKind.RESOURCE, Map.of(), List.of(), "apim");
        when(entityService.findByEntityId(ENV, "api.123")).thenReturn(Optional.of(stored));

        try (Response response = target("/environments/" + ENV + "/entities/api.123").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(EntityResponse.class).entityId()).isEqualTo("api.123");
        }
    }

    @Test
    void get_by_entityId_unknown_returns_404() {
        when(entityService.findByEntityId(ENV, "api.missing")).thenReturn(Optional.empty());

        try (Response response = target("/environments/" + ENV + "/entities/api.missing").request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
            Map<String, Object> body = response.readEntity(new GenericType<>() {});
            assertThat(body).containsEntry("error", "EntityNotFound");
        }
    }

    @Test
    void get_list_filters_by_kind() {
        List<Entity> page = List.of(entity("idp.am.alice", EntityKind.PRINCIPAL, Map.of(), List.of(), "gravitee_am_default"));
        when(entityService.findPage(eq(ENV), eq(new EntityFilter(EntityKind.PRINCIPAL, null, null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/entities").queryParam("kind", "PRINCIPAL").request().get()) {
            PagedResponseDto<EntityResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).extracting(EntityResponse::entityId).containsExactly("idp.am.alice");
        }
    }

    @Test
    void get_list_filters_by_source() {
        List<Entity> page = List.of(entity("api.123", EntityKind.RESOURCE, Map.of(), List.of(), "apim"));
        when(entityService.findPage(eq(ENV), eq(new EntityFilter(null, "apim", null)), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/entities").queryParam("source", "apim").request().get()) {
            PagedResponseDto<EntityResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).extracting(EntityResponse::entityId).containsExactly("api.123");
        }
    }

    @Test
    void get_list_filters_by_entityIdPrefix() {
        List<Entity> page = List.of(
            entity("api.123", EntityKind.RESOURCE, Map.of(), List.of(), "apim"),
            entity("api.123.tool-a", EntityKind.RESOURCE, Map.of(), List.of(), "apim")
        );
        when(entityService.findPage(eq(ENV), eq(new EntityFilter(null, null, "api.123")), any(Pageable.class))).thenReturn(
            new PagedResult<>(page, page.size(), 1, Pageable.DEFAULT_PER_PAGE)
        );

        try (Response response = target("/environments/" + ENV + "/entities").queryParam("entityIdPrefix", "api.123").request().get()) {
            PagedResponseDto<EntityResponse> body = response.readEntity(new GenericType<>() {});
            assertThat(body.data()).extracting(EntityResponse::entityId).containsExactlyInAnyOrder("api.123", "api.123.tool-a");
        }
    }

    @Test
    void put_updates_attributes_and_parents_only() {
        Entity updated = entity("api.123", EntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim");
        when(entityService.update(any(), eq("api.123"), any(UpdateEntityCommand.class))).thenReturn(updated);

        try (
            Response response = target("/environments/" + ENV + "/entities/api.123")
                .request()
                .put(jakarta.ws.rs.client.Entity.json(new UpdateEntityRequest(Map.of("k", "v2"), List.of("api.parent"))))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            EntityResponse body = response.readEntity(EntityResponse.class);
            assertThat(body.attributes()).containsEntry("k", "v2");
            assertThat(body.parents()).containsExactly("api.parent");
            assertThat(body.source()).isEqualTo("apim");
        }
    }

    @Test
    void put_unknown_entityId_returns_404() {
        when(entityService.update(any(), eq("api.missing"), any())).thenThrow(new EntityNotFoundException(ENV, "api.missing"));

        try (
            Response response = target("/environments/" + ENV + "/entities/api.missing")
                .request()
                .put(jakarta.ws.rs.client.Entity.json(new UpdateEntityRequest(Map.of(), List.of())))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void delete_returns_cascade_summary() {
        when(entityService.delete(any(), eq("api.bookings"))).thenReturn(
            new CascadeResult(List.of("api.bookings", "mcp.bookings.tool-1"), List.of("policy-1"))
        );

        try (Response response = target("/environments/" + ENV + "/entities/api.bookings").request().delete()) {
            assertThat(response.getStatus()).isEqualTo(200);
            CascadeResponse body = response.readEntity(CascadeResponse.class);
            assertThat(body.deletedEntityIds()).containsExactlyInAnyOrder("api.bookings", "mcp.bookings.tool-1");
            assertThat(body.deletedPolicyIds()).hasSize(1);
            assertThat(body.totalAffected()).isEqualTo(3);
        }
    }

    private static Entity entity(String entityId, EntityKind kind, Map<String, Object> attributes, List<String> parents, String source) {
        Instant now = Instant.parse("2026-05-14T10:00:00Z");
        return new Entity("id-" + entityId, entityId, kind, attributes, parents, source, ENV, now, now);
    }
}
