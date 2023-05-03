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

import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
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
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.management.v2.rest.model.ErrorEntity;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_StopTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String UNKNOWN_API = "unknown";
    private static final String ENVIRONMENT = "my-env";

    private ApiEntity apiEntity;

    @Autowired
    private ParameterService parameterService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_stop";
    }

    @Before
    public void init() throws TechnicalException {
        reset(apiServiceV4, parameterService, apiSearchServiceV4, apiStateServiceV4);
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
        apiEntity.setState(Lifecycle.State.STARTED);

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

        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.API_REVIEW_ENABLED),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(false);
    }

    @Test
    public void should_not_stop_api_with_insufficient_rights() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any());
        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_stop_api_with_incorrect_if_match() {
        final Response response = rootTarget().request().header(HttpHeaders.IF_MATCH, "\"000\"").post(Entity.json(""));
        assertEquals(HttpStatusCode.PRECONDITION_FAILED_412, response.getStatus());

        var body = response.readEntity(String.class);
        assert (body.isEmpty());

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_not_stop_api_if_not_found() {
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API)))
            .thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        var body = response.readEntity(ErrorEntity.class);
        assertNotNull(body);

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_not_stop_api_if_archived() {
        var archivedEntity = apiEntity;
        archivedEntity.setLifecycleState(ApiLifecycleState.ARCHIVED);

        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(archivedEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(ErrorEntity.class);
        assertNotNull(body);
        assertEquals("Deleted API cannot be stopped", body.getMessage());

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_not_stop_api_if_already_stopped() {
        var stoppedEntity = apiEntity;
        stoppedEntity.setState(Lifecycle.State.STOPPED);

        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(stoppedEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(ErrorEntity.class);
        assertNotNull(body);
        assertEquals("API is already stopped", body.getMessage());

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_stop_api() {
        var updatedEntity = apiEntity;
        updatedEntity.setUpdatedAt(new Date());

        when(apiStateServiceV4.stop(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME))).thenReturn(updatedEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        assertEquals("\"" + updatedEntity.getUpdatedAt().getTime() + "\"", response.getHeaders().get("Etag").get(0));

        verify(apiStateServiceV4, times(1)).stop(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME));
    }
}
