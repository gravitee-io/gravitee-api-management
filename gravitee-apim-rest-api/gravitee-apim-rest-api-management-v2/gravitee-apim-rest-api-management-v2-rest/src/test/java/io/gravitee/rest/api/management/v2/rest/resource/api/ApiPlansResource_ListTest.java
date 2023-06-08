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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.PlanFixtures;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.PlansResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiPlansResource_ListTest extends ApiPlansResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/plans";
    }

    @Autowired
    private PlanSearchService planSearchService;

    @Test
    public void should_return_empty_page_if_no_plans() {
        var planQuery = PlanQuery.builder().apiId(API).status(List.of(PlanStatus.PUBLISHED)).build();

        when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
            .thenReturn(new ArrayList<>());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var plansResponse = response.readEntity(PlansResponse.class);

        // Check data
        assertEquals(0, plansResponse.getData().size());

        // Check pagination
        Pagination pagination = plansResponse.getPagination();
        assertNull(pagination.getPage());
        assertNull(pagination.getPerPage());
        assertNull(pagination.getPageItemsCount());
        assertNull(pagination.getTotalCount());
        assertNull(pagination.getPageCount());

        // Check links
        Links links = plansResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_PLAN),
                eq(API),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_return_list_if_no_params_specified_v4() {
        var planQuery = PlanQuery.builder().apiId(API).securityType(new ArrayList<>()).status(List.of(PlanStatus.PUBLISHED)).build();

        when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
            .thenReturn(
                List.of(
                    PlanFixtures.aPlanEntityV4().toBuilder().id("plan-1").order(3).build(),
                    PlanFixtures.aPlanEntityV4().toBuilder().id("plan-3").order(1).build()
                )
            );

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var plansResponse = response.readEntity(PlansResponse.class);

        // Check data
        assertEquals(2, plansResponse.getData().size());

        assertEquals("plan-3", plansResponse.getData().get(0).getPlanV4().getId());
        assertEquals("plan-1", plansResponse.getData().get(1).getPlanV4().getId());

        // Check pagination
        Pagination pagination = plansResponse.getPagination();
        assertEquals(1L, pagination.getPage().longValue());
        assertEquals(10L, pagination.getPerPage().longValue());
        assertEquals(2L, pagination.getPageItemsCount().longValue());
        assertEquals(2L, pagination.getTotalCount().longValue());
        assertEquals(1L, pagination.getPageCount().longValue());

        // Check links
        Links links = plansResponse.getLinks();
        assert (Objects.nonNull(links.getSelf()));
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void should_return_list_with_params_specified_v2() {
        var planQuery = PlanQuery
            .builder()
            .apiId(API)
            .securityType(List.of(PlanSecurityType.JWT))
            .status(List.of(PlanStatus.DEPRECATED))
            .build();

        var rule = new Rule();
        rule.setMethods(Set.of(HttpMethod.GET));
        rule.setDescription("description of a rule");
        rule.setEnabled(true);

        var rules = List.of(rule);

        when(planSearchService.search(eq(GraviteeContext.getExecutionContext()), eq(planQuery), eq(USER_NAME), eq(true)))
            .thenReturn(
                List.of(
                    PlanFixtures
                        .aPlanEntityV2()
                        .toBuilder()
                        .id("plan-1")
                        .order(1)
                        .status(io.gravitee.rest.api.model.PlanStatus.DEPRECATED)
                        .paths(Map.of("path", rules))
                        .build(),
                    PlanFixtures
                        .aPlanEntityV2()
                        .toBuilder()
                        .id("plan-3")
                        .order(2)
                        .status(io.gravitee.rest.api.model.PlanStatus.DEPRECATED)
                        .build()
                )
            );

        final Response response = rootTarget()
            .queryParam("securities", "JWT")
            .queryParam("statuses", "DEPRECATED")
            .queryParam("perPage", 1)
            .request()
            .get();

        assertEquals(OK_200, response.getStatus());

        var plansResponse = response.readEntity(PlansResponse.class);

        // Check data
        assertEquals(1, plansResponse.getData().size());
        assertEquals("plan-1", plansResponse.getData().get(0).getPlanV2().getId());

        // Check pagination
        Pagination pagination = plansResponse.getPagination();
        assertEquals(1L, pagination.getPage().longValue());
        assertEquals(1L, pagination.getPerPage().longValue());
        assertEquals(1L, pagination.getPageItemsCount().longValue());
        assertEquals(2L, pagination.getTotalCount().longValue());
        assertEquals(2L, pagination.getPageCount().longValue());

        // Check links
        Links links = plansResponse.getLinks();
        assert (Objects.nonNull(links.getSelf()));
        assert (Objects.nonNull(links.getFirst()));
        assertNull(links.getPrevious());
        assert (Objects.nonNull(links.getNext()));
        assert (Objects.nonNull(links.getLast()));
    }
}
