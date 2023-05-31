/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.*;
import static jakarta.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.ApiFixtures;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String UNKNOWN_API = "unknown";
    private static final String ENVIRONMENT = "my-env";

    private ApiEntity apiEntity;

    @Autowired
    private WorkflowService workflowService;

    private final ObjectMapper mapper = new GraviteeMapper(false);

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Before
    public void init() throws TechnicalException {
        reset(apiServiceV4);
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        Api api = new Api();
        api.setId(API);
        api.setEnvironmentId(ENVIRONMENT);
        doReturn(Optional.of(api)).when(apiRepository).findById(API);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setId(API);
        apiEntity.setName(API);
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("my.fake.host", "/test")));
        httpListener.setPathMappings(Set.of("/test"));

        SubscriptionListener subscriptionListener = new SubscriptionListener();
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("Entrypoint type");
        entrypoint.setQos(Qos.AT_LEAST_ONCE);
        entrypoint.setDlq(new Dlq("my-endpoint"));
        entrypoint.setConfiguration("nice configuration");
        subscriptionListener.setEntrypoints(List.of(entrypoint));
        subscriptionListener.setType(ListenerType.SUBSCRIPTION);

        TcpListener tcpListener = new TcpListener();
        tcpListener.setType(ListenerType.TCP);
        tcpListener.setEntrypoints(List.of(entrypoint));

        apiEntity.setListeners(List.of(httpListener, subscriptionListener, tcpListener));
        apiEntity.setProperties(List.of(new Property()));
        apiEntity.setServices(new ApiServices());
        apiEntity.setResources(List.of(new Resource()));
        apiEntity.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setAnalytics(new Analytics());

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setType("http-get");
        Endpoint endpoint = new Endpoint();
        endpoint.setType("http-get");
        endpoint.setConfiguration(
            "{\n" +
            "                        \"bootstrapServers\": \"kafka:9092\",\n" +
            "                        \"topics\": [\n" +
            "                            \"demo\"\n" +
            "                        ],\n" +
            "                        \"producer\": {\n" +
            "                            \"enabled\": false\n" +
            "                        },\n" +
            "                        \"consumer\": {\n" +
            "                            \"encodeMessageId\": true,\n" +
            "                            \"enabled\": true,\n" +
            "                            \"autoOffsetReset\": \"earliest\"\n" +
            "                        }\n" +
            "                    }"
        );
        endpointGroup.setEndpoints(List.of(endpoint));
        apiEntity.setEndpointGroups(List.of(endpointGroup));

        doReturn(apiEntity).when(apiSearchServiceV4).findGenericById(GraviteeContext.getExecutionContext(), API);
        doThrow(ApiNotFoundException.class).when(apiSearchServiceV4).findGenericById(GraviteeContext.getExecutionContext(), UNKNOWN_API);
    }

    @Test
    public void shouldDeployApi() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label");

        apiEntity.setState(Lifecycle.State.STARTED);
        apiEntity.setUpdatedAt(new Date());
        doReturn(apiEntity).when(apiStateServiceV4).deploy(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));

        assertEquals(ACCEPTED_202, response.getStatus());

        assertEquals(apiEntity.getUpdatedAt().toString(), response.getLastModified().toString());
        assertEquals(new EntityTag(Long.toString(apiEntity.getUpdatedAt().getTime())), response.getEntityTag());

        verify(apiStateServiceV4, times(1)).deploy(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
    }

    @Test
    public void shouldNotDeployApi() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label_too_long_because_more_than_32_chars");

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());

        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotDeployApiWithInvalidRole() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("a nice label");

        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any());

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }

    @Test
    public void should_return_404_if_not_found() {
        UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Api [" + API + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_GATEWAY_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_update_v4_api() {
        ApiEntity apiEntity = ApiFixtures.aModelApiV4().toBuilder().id(API).build();
        UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);
        when(apiServiceV4.update(eq(GraviteeContext.getExecutionContext()), eq(API), any(UpdateApiEntity.class), eq(false), eq(USER_NAME)))
            .thenReturn(apiEntity);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        final ApiV4 apiV4 = response.readEntity(ApiV4.class);
        assertEquals(API, apiV4.getId());

        verify(apiServiceV4)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                argThat(updateApiEntity -> {
                    assertEquals(updateApiV4.getName(), updateApiEntity.getName());
                    return true;
                }),
                eq(false),
                eq(USER_NAME)
            );
    }

    @Test
    public void should_update_v2_api() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = ApiFixtures.aModelApiV2().toBuilder().id(API).build();
        UpdateApiV2 updateApiV2 = ApiFixtures.anUpdateApiV2();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);
        when(
            apiService.update(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                any(io.gravitee.rest.api.model.api.UpdateApiEntity.class),
                eq(false)
            )
        )
            .thenReturn(apiEntity);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV2));
        assertEquals(OK_200, response.getStatus());

        final ApiV2 apiV2 = response.readEntity(ApiV2.class);
        assertEquals(API, apiV2.getId());

        verify(apiService)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                argThat(updateApiEntity -> {
                    assertEquals(updateApiV2.getName(), updateApiEntity.getName());
                    return true;
                }),
                eq(false)
            );
    }

    @Test
    public void shouldDeleteApi() {
        doNothing().when(apiServiceV4).delete(GraviteeContext.getExecutionContext(), API, false);
        final Response response = rootTarget(API).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldNotDeleteApiBecauseNotfound() throws TechnicalException {
        doThrow(ApiNotFoundException.class).when(apiServiceV4).delete(GraviteeContext.getExecutionContext(), UNKNOWN_API, false);

        final Response response = rootTarget(UNKNOWN_API).request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotDeleteApiWithInsufficientRights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any());
        final Response response = rootTarget(API).request().delete();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }
}
