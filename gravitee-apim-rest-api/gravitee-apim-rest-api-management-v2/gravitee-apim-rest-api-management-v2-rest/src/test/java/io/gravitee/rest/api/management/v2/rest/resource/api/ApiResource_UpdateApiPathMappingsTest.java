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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.rest.api.model.MembershipMemberType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import fixtures.ListenerFixtures;
import fixtures.ListenerModelFixtures;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.management.v2.rest.model.Cors;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.PathV4;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_UpdateApiPathMappingsTest extends AbstractNonAdminApiResourceTest {

    private static final String API_ROLE_ID = "api-role-id";

    @Autowired
    private ApiAuthorizationService apiAuthorizationService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void givenUserIsDirectApiMember() {
        var membership = MembershipEntity.builder()
            .memberId(USER_NAME)
            .memberType(USER)
            .referenceId(API)
            .referenceType(MembershipReferenceType.API)
            .roleId(API_ROLE_ID)
            .build();
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(
            Set.of(membership)
        );
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(Set.of());
        var role = RoleEntity.builder().id(API_ROLE_ID).build();
        when(roleService.findById(API_ROLE_ID)).thenReturn(role);
        when(apiAuthorizationService.canManageApi(role)).thenReturn(true);
    }

    @Test
    public void should_apply_path_mappings_when_user_lacks_gateway_definition_update() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/test"));
        stubLacksGatewayDefinitionUpdate(apiEntity);

        var updateApiV4 = updateApiWithPathMappings(List.of("/charges/:chargeId"));
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                assertEquals(Set.of("/charges/:chargeId"), firstHttpListener(updateApiEntity).getPathMappings());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_keep_stored_paths_cors_and_entrypoints_when_user_lacks_gateway_definition_update() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/test"));
        stubLacksGatewayDefinitionUpdate(apiEntity);
        var storedHttpListener = (HttpListener) apiEntity.getListeners().getFirst();

        var requestListener = ListenerFixtures.aHttpListener()
            .paths(List.of(new PathV4().host("changed.host").path("/changed").overrideAccess(false)))
            .cors(new Cors().enabled(false))
            .pathMappings(List.of("/charges/:chargeId"));
        var updateApiV4 = ApiFixtures.anUpdateApiV4().listeners(List.of(new Listener(requestListener)));

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                var httpListener = firstHttpListener(updateApiEntity);
                assertEquals(Set.of("/charges/:chargeId"), httpListener.getPathMappings());
                assertEquals(storedHttpListener.getPaths(), httpListener.getPaths());
                assertEquals(storedHttpListener.getCors(), httpListener.getCors());
                assertEquals(storedHttpListener.getEntrypoints(), httpListener.getEntrypoints());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_keep_stored_path_mappings_when_request_has_null_path_mappings() throws Exception {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/kept"));
        stubLacksGatewayDefinitionUpdate(apiEntity);

        // Generated HttpListener defaults pathMappings to []. Explicit JSON null must survive
        // deserialization so the guard can treat null as "keep stored" (V2 parity).
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var updateApiV4 = updateApiWithPathMappings(List.of("/placeholder"));
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(updateApiV4);
        ((com.fasterxml.jackson.databind.node.ObjectNode) tree.get("listeners").get(0)).putNull("pathMappings");
        final Response response = rootTarget(API)
            .request()
            .put(Entity.entity(mapper.writeValueAsString(tree), jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                assertEquals(Set.of("/kept"), firstHttpListener(updateApiEntity).getPathMappings());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_apply_all_listener_changes_when_user_has_gateway_definition_update() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/test"));
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(apiEntity);
        when(
            apiServiceV4.update(eq(GraviteeContext.getExecutionContext()), eq(API), any(UpdateApiEntity.class), eq(false), eq(USER_NAME))
        ).thenReturn(apiEntity);
        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(apiEntity))).thenReturn(true);

        var requestListener = ListenerFixtures.aHttpListener()
            .paths(List.of(new PathV4().host("changed.host").path("/changed").overrideAccess(false)))
            .pathMappings(List.of("/charges/:chargeId"));
        var updateApiV4 = ApiFixtures.anUpdateApiV4().listeners(List.of(new Listener(requestListener)));

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                var httpListener = firstHttpListener(updateApiEntity);
                assertEquals(Set.of("/charges/:chargeId"), httpListener.getPathMappings());
                assertEquals("/changed", httpListener.getPaths().getFirst().getPath());
                assertEquals("changed.host", httpListener.getPaths().getFirst().getHost());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_delete_path_mapping_when_user_lacks_gateway_definition_update() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(new LinkedHashSet<>(List.of("/charges/:chargeId", "/a")));
        stubLacksGatewayDefinitionUpdate(apiEntity);

        var updateApiV4 = updateApiWithPathMappings(List.of("/a"));
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                assertEquals(Set.of("/a"), firstHttpListener(updateApiEntity).getPathMappings());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_edit_path_mapping_when_user_lacks_gateway_definition_update() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/old"));
        stubLacksGatewayDefinitionUpdate(apiEntity);

        var updateApiV4 = updateApiWithPathMappings(List.of("/new"));
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                assertEquals(Set.of("/new"), firstHttpListener(updateApiEntity).getPathMappings());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_accept_path_mapping_outside_form_rule_that_compiles() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/"));
        stubLacksGatewayDefinitionUpdate(apiEntity);

        var updateApiV4 = updateApiWithPathMappings(List.of("/", "/users/@me"));
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        verify(apiServiceV4).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            argThat(updateApiEntity -> {
                assertEquals(Set.of("/", "/users/@me"), firstHttpListener(updateApiEntity).getPathMappings());
                return true;
            }),
            eq(false),
            eq(USER_NAME)
        );
    }

    @Test
    public void should_return_400_when_path_mapping_cannot_be_compiled_for_user_lacking_gateway_definition_update() {
        ApiEntity apiEntity = storedHttpApiWithPathMappings(Set.of("/test"));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_GATEWAY_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(apiEntity);

        var updateApiV4 = updateApiWithPathMappings(List.of("/a/**"));
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals("api.listeners.http.pathMappings.invalid", error.getTechnicalCode());
    }

    private ApiEntity storedHttpApiWithPathMappings(Set<String> pathMappings) {
        var httpListener = ListenerModelFixtures.aModelHttpListener().toBuilder().pathMappings(pathMappings).build();
        return ApiFixtures.aModelHttpApiV4().toBuilder().id(API).listeners(List.of(httpListener)).build();
    }

    private void stubLacksGatewayDefinitionUpdate(ApiEntity apiEntity) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_GATEWAY_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(apiEntity);
        when(
            apiServiceV4.update(eq(GraviteeContext.getExecutionContext()), eq(API), any(UpdateApiEntity.class), eq(false), eq(USER_NAME))
        ).thenReturn(apiEntity);
        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(apiEntity))).thenReturn(true);
    }

    private UpdateApiV4 updateApiWithPathMappings(List<String> pathMappings) {
        return ApiFixtures.anUpdateApiV4().listeners(List.of(new Listener(ListenerFixtures.aHttpListener().pathMappings(pathMappings))));
    }

    private HttpListener firstHttpListener(UpdateApiEntity updateApiEntity) {
        return (HttpListener) updateApiEntity.getListeners().getFirst();
    }
}
