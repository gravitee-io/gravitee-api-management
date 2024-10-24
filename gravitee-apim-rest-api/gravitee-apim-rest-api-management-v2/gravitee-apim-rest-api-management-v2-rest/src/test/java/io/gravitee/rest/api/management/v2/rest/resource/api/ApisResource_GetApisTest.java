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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiV1;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApisResource_GetApisTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environment);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environment);
    }

    @Test
    public void should_return_404_when_environment_does_not_exist() {
        doThrow(new EnvironmentNotFoundException(ENVIRONMENT)).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void should_return_403_when_user_not_permitted_to_read_api() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq(ENVIRONMENT),
                any()
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_list_one_V4_api() {
        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("api-1");
        returnedApi.setName("api-name");
        returnedApi.setApiVersion("v1");
        returnedApi.setDescription("api-description");
        returnedApi.setDefinitionVersion(DefinitionVersion.V4);
        returnedApi.setDisableMembershipNotifications(true);
        returnedApi.setLabels(List.of("label1", "label2"));
        returnedApi.setGroups(Set.of("group1", "group2"));
        returnedApi.setTags(Set.of("tag1", "tag2"));
        returnedApi.setState(Lifecycle.State.STARTED);
        returnedApi.setWorkflowState(WorkflowState.REVIEW_OK);
        returnedApi.setVisibility(Visibility.PUBLIC);
        returnedApi.setCreatedAt(Date.from(Instant.parse("2020-01-01T10:10:10.00Z")));
        returnedApi.setUpdatedAt(Date.from(Instant.parse("2020-01-01T10:10:10.00Z")));
        returnedApi.setDeployedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));

        var apiList = new ArrayList<GenericApiEntity>();
        apiList.add(returnedApi);

        when(
            apiServiceV4.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                isNull(),
                eq(new SortableImpl("name", true)),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(apiList, 1, apiList.size(), apiList.size()));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check Response content
        final ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apisResponse.getData());
        assertNotNull(apisResponse.getPagination());
        assertNotNull(apisResponse.getLinks());

        // Check apis
        List<Api> apis = apisResponse.getData();
        Assertions.assertEquals(1, apis.size());
        ApiV4 api = apis.get(0).getApiV4();
        Assertions.assertEquals("api-1", api.getId());
        Assertions.assertEquals("api-name", api.getName());
        Assertions.assertEquals("v1", api.getApiVersion());
        Assertions.assertEquals("api-description", api.getDescription());
        Assertions.assertEquals("V4", api.getDefinitionVersion().getValue());
        Assertions.assertEquals(true, api.getDisableMembershipNotifications());
        Assertions.assertEquals(List.of("label1", "label2"), api.getLabels());
        Assertions.assertTrue(api.getGroups().containsAll(List.of("group1", "group2")));
        Assertions.assertTrue(api.getTags().containsAll(List.of("tag1", "tag2")));
        Assertions.assertEquals(GenericApi.StateEnum.STARTED, api.getState());
        Assertions.assertEquals(io.gravitee.rest.api.management.v2.rest.model.Visibility.PUBLIC, api.getVisibility());
        Assertions.assertEquals("2020-02-02T20:22:02Z", api.getDeployedAt().toString());
        Assertions.assertEquals("2020-01-01T10:10:10Z", api.getCreatedAt().toString());
        Assertions.assertEquals("2020-01-01T10:10:10Z", api.getUpdatedAt().toString());

        // Check pagination
        Pagination pagination = apisResponse.getPagination();
        Assertions.assertEquals(1, pagination.getPage());
        Assertions.assertEquals(10, pagination.getPerPage());
        Assertions.assertEquals(1, pagination.getPageItemsCount());
        Assertions.assertEquals(1, pagination.getTotalCount());
        Assertions.assertEquals(1, pagination.getPageCount());

        // Check links
        Links links = apisResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/apis/"));
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void should_list_one_V2_api() {
        io.gravitee.rest.api.model.api.ApiEntity returnedApi = new io.gravitee.rest.api.model.api.ApiEntity();
        returnedApi.setId("api-1");
        returnedApi.setName("api-name");
        returnedApi.setVersion("v1");
        returnedApi.setDescription("api-description");
        returnedApi.setGraviteeDefinitionVersion("2.0.0");
        returnedApi.setDisableMembershipNotifications(true);
        returnedApi.setLabels(List.of("label1", "label2"));
        returnedApi.setGroups(Set.of("group1", "group2"));
        returnedApi.setTags(Set.of("tag1", "tag2"));
        returnedApi.setState(Lifecycle.State.STARTED);
        returnedApi.setWorkflowState(WorkflowState.REVIEW_OK);
        returnedApi.setVisibility(Visibility.PUBLIC);
        returnedApi.setCreatedAt(Date.from(Instant.parse("2020-01-01T10:10:10.00Z")));
        returnedApi.setUpdatedAt(Date.from(Instant.parse("2020-01-01T10:10:10.00Z")));
        returnedApi.setDeployedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));

        var apiList = new ArrayList<GenericApiEntity>();
        apiList.add(returnedApi);

        when(
            apiServiceV4.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                isNull(),
                eq(new SortableImpl("name", true)),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(apiList, 1, apiList.size(), apiList.size()));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check Response content
        final ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apisResponse.getData());
        assertNotNull(apisResponse.getPagination());
        assertNotNull(apisResponse.getLinks());

        // Check apis
        List<Api> apis = apisResponse.getData();
        Assertions.assertEquals(1, apis.size());
        ApiV2 api = apis.get(0).getApiV2();
        Assertions.assertEquals("api-1", api.getId());
        Assertions.assertEquals("api-name", api.getName());
        Assertions.assertEquals("v1", api.getApiVersion());
        Assertions.assertEquals("api-description", api.getDescription());
        Assertions.assertEquals("V2", api.getDefinitionVersion().getValue());
        Assertions.assertEquals(true, api.getDisableMembershipNotifications());
        Assertions.assertEquals(List.of("label1", "label2"), api.getLabels());
        Assertions.assertTrue(api.getGroups().containsAll(List.of("group1", "group2")));
        Assertions.assertTrue(api.getTags().containsAll(List.of("tag1", "tag2")));
        Assertions.assertEquals(GenericApi.StateEnum.STARTED, api.getState());
        Assertions.assertEquals(io.gravitee.rest.api.management.v2.rest.model.Visibility.PUBLIC, api.getVisibility());
        Assertions.assertEquals("2020-02-02T20:22:02Z", api.getDeployedAt().toString());
        Assertions.assertEquals("2020-01-01T10:10:10Z", api.getCreatedAt().toString());
        Assertions.assertEquals("2020-01-01T10:10:10Z", api.getUpdatedAt().toString());

        // Check pagination
        Pagination pagination = apisResponse.getPagination();
        Assertions.assertEquals(1, pagination.getPage());
        Assertions.assertEquals(10, pagination.getPerPage());
        Assertions.assertEquals(1, pagination.getPageItemsCount());
        Assertions.assertEquals(1, pagination.getTotalCount());
        Assertions.assertEquals(1, pagination.getPageCount());

        // Check links
        Links links = apisResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/apis/"));
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void should_list_one_V1_api() {
        io.gravitee.rest.api.model.api.ApiEntity returnedApi = new io.gravitee.rest.api.model.api.ApiEntity();
        returnedApi.setId("api-v1");
        returnedApi.setName("api-v1-name");
        returnedApi.setVersion("v1");
        returnedApi.setDescription("api-description");
        returnedApi.setGraviteeDefinitionVersion("1.0.0");
        returnedApi.setDisableMembershipNotifications(true);
        returnedApi.setLabels(List.of("label1", "label2"));
        returnedApi.setGroups(Set.of("group1", "group2"));
        returnedApi.setTags(Set.of("tag1", "tag2"));
        returnedApi.setState(Lifecycle.State.STARTED);
        returnedApi.setWorkflowState(WorkflowState.REVIEW_OK);
        returnedApi.setVisibility(Visibility.PUBLIC);
        returnedApi.setCreatedAt(Date.from(Instant.parse("2020-01-01T10:10:10.00Z")));
        returnedApi.setUpdatedAt(Date.from(Instant.parse("2020-01-01T10:10:10.00Z")));
        returnedApi.setDeployedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));

        var apiList = new ArrayList<GenericApiEntity>();
        apiList.add(returnedApi);

        when(
            apiServiceV4.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                isNull(),
                eq(new SortableImpl("name", true)),
                eq(new PageableImpl(1, 10))
            )
        )
            .thenReturn(new Page<>(apiList, 1, apiList.size(), apiList.size()));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check Response content
        final ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apisResponse.getData());
        assertNotNull(apisResponse.getPagination());
        assertNotNull(apisResponse.getLinks());

        // Check apis
        List<Api> apis = apisResponse.getData();
        Assertions.assertEquals(1, apis.size());
        ApiV1 api = apis.get(0).getApiV1();
        Assertions.assertEquals("api-v1", api.getId());
        Assertions.assertEquals("api-v1-name", api.getName());
        Assertions.assertEquals("v1", api.getApiVersion());
        Assertions.assertEquals("api-description", api.getDescription());
        Assertions.assertEquals("V1", api.getDefinitionVersion().getValue());
        Assertions.assertEquals(true, api.getDisableMembershipNotifications());
        Assertions.assertEquals(List.of("label1", "label2"), api.getLabels());
        Assertions.assertTrue(api.getGroups().containsAll(List.of("group1", "group2")));
        Assertions.assertTrue(api.getTags().containsAll(List.of("tag1", "tag2")));
        Assertions.assertEquals(GenericApi.StateEnum.STARTED, api.getState());
        Assertions.assertEquals(io.gravitee.rest.api.management.v2.rest.model.Visibility.PUBLIC, api.getVisibility());
        Assertions.assertEquals("2020-02-02T20:22:02Z", api.getDeployedAt().toString());
        Assertions.assertEquals("2020-01-01T10:10:10Z", api.getCreatedAt().toString());
        Assertions.assertEquals("2020-01-01T10:10:10Z", api.getUpdatedAt().toString());

        // Check pagination
        Pagination pagination = apisResponse.getPagination();
        Assertions.assertEquals(1, pagination.getPage());
        Assertions.assertEquals(10, pagination.getPerPage());
        Assertions.assertEquals(1, pagination.getPageItemsCount());
        Assertions.assertEquals(1, pagination.getTotalCount());
        Assertions.assertEquals(1, pagination.getPageCount());

        // Check links
        Links links = apisResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/apis/"));
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void should_list_multiple_api() {
        ApiEntity returnedApi1 = new ApiEntity();
        returnedApi1.setId("api-1");
        returnedApi1.setState(Lifecycle.State.STOPPED);
        returnedApi1.setDefinitionVersion(DefinitionVersion.V4);

        io.gravitee.rest.api.model.api.ApiEntity returnedApi2 = new io.gravitee.rest.api.model.api.ApiEntity();
        returnedApi2.setId("api-2");
        returnedApi2.setState(Lifecycle.State.STOPPED);
        returnedApi2.setGraviteeDefinitionVersion("2.0.0");

        var apiList = new ArrayList<GenericApiEntity>();
        apiList.add(returnedApi1);
        apiList.add(returnedApi2);

        when(
            apiServiceV4.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                isNull(),
                eq(new SortableImpl("name", true)),
                eq(new PageableImpl(1, 2))
            )
        )
            .thenReturn(new Page<>(apiList, 1, 2, 42));

        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 1)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 2)
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check Response content
        final ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apisResponse.getData());
        assertNotNull(apisResponse.getPagination());
        assertNotNull(apisResponse.getLinks());

        // Check apis
        List<Api> apis = apisResponse.getData();
        Assertions.assertEquals(2, apis.size());
        ApiV4 api = apis.get(0).getApiV4();
        Assertions.assertEquals("api-1", api.getId());
        // Check no expands fields are set
        Assertions.assertNull(api.getDeploymentState());

        // Check pagination
        Pagination pagination = apisResponse.getPagination();
        Assertions.assertEquals(1, pagination.getPage());
        Assertions.assertEquals(2, pagination.getPerPage());
        Assertions.assertEquals(2, pagination.getPageItemsCount());
        Assertions.assertEquals(42, pagination.getTotalCount());
        Assertions.assertEquals(21, pagination.getPageCount());

        // Check links
        Links links = apisResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/apis/?page=1&perPage=2"));
        assertTrue(links.getFirst().endsWith("/apis/?page=1&perPage=2"));
        assertNull(links.getPrevious());
        assertTrue(links.getNext().endsWith("/apis/?page=2&perPage=2"));
        assertTrue(links.getLast().endsWith("/apis/?page=21&perPage=2"));
    }

    @Test
    public void should_list_api_with_all_expands_params() {
        ApiEntity returnedApi1 = new ApiEntity();
        returnedApi1.setId("api-1");
        returnedApi1.setState(Lifecycle.State.STOPPED);
        returnedApi1.setDefinitionVersion(DefinitionVersion.V4);

        io.gravitee.rest.api.model.api.ApiEntity returnedApi2 = new io.gravitee.rest.api.model.api.ApiEntity();
        returnedApi2.setId("api-2");
        returnedApi2.setState(Lifecycle.State.STOPPED);
        returnedApi2.setGraviteeDefinitionVersion("2.0.0");

        var apiList = new ArrayList<GenericApiEntity>();
        apiList.add(returnedApi1);
        apiList.add(returnedApi2);

        when(
            apiServiceV4.findAll(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                eq(Set.of("deploymentState", "primaryOwner")),
                eq(new SortableImpl("name", true)),
                eq(new PageableImpl(1, 2))
            )
        )
            .thenReturn(new Page<>(apiList, 1, 2, 42));
        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(returnedApi1))).thenReturn(true);

        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(returnedApi2))).thenReturn(false);

        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 1)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 2)
            .queryParam("expands", "deploymentState,primaryOwner")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check Response content
        final ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apisResponse.getData());
        assertNotNull(apisResponse.getPagination());
        assertNotNull(apisResponse.getLinks());

        // Check apis
        List<Api> apis = apisResponse.getData();
        Assertions.assertEquals(2, apis.size());
        ApiV4 api1 = apis.get(0).getApiV4();
        Assertions.assertEquals("api-1", api1.getId());
        Assertions.assertEquals(GenericApi.DeploymentStateEnum.DEPLOYED, api1.getDeploymentState());

        ApiV2 api2 = apis.get(1).getApiV2();
        Assertions.assertEquals("api-2", api2.getId());
        Assertions.assertEquals(GenericApi.DeploymentStateEnum.NEED_REDEPLOY, api2.getDeploymentState());
    }
}
