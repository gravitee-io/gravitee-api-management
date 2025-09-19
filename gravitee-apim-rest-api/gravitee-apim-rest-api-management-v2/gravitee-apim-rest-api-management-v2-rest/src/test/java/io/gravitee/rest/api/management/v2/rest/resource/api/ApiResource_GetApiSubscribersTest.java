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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApplicationFixtures;
import fixtures.SubscriptionFixtures;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.SubscribersResponse;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_GetApiSubscribersTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscribers";
    }

    @Autowired
    ApplicationService applicationService;

    @Autowired
    SubscriptionService subscriptionService;

    @BeforeEach
    public void init() throws TechnicalException {
        super.init();
        Mockito.reset(applicationService, subscriptionService);
    }

    @Test
    public void should_return_empty_page_if_no_subscriber() {
        when(subscriptionService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(Collections.emptyList());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var subscribersResponse = response.readEntity(SubscribersResponse.class);

        // Check data
        assertEquals(0, subscribersResponse.getData().size());

        // Check pagination
        Pagination pagination = subscribersResponse.getPagination();
        assertNull(pagination.getPage());
        assertNull(pagination.getPerPage());
        assertNull(pagination.getPageItemsCount());
        assertNull(pagination.getTotalCount());
        assertNull(pagination.getPageCount());

        // Check links
        Links links = subscribersResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        final Response response = rootTarget().request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_return_list_of_subscribers() {
        var subscriptionEntity1 = SubscriptionFixtures.aSubscriptionEntity().toBuilder().application("my-application-1").build();
        var subscriptionEntity2 = SubscriptionFixtures.aSubscriptionEntity().toBuilder().application("my-application-2").build();
        when(subscriptionService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(
            List.of(subscriptionEntity1, subscriptionEntity2)
        );

        var applicationQuery = ApplicationQuery.builder().ids(Set.of("my-application-1", "my-application-2")).build();
        when(
            applicationService.search(
                eq(GraviteeContext.getExecutionContext()),
                eq(applicationQuery),
                any(Sortable.class),
                any(Pageable.class)
            )
        ).thenReturn(
            new Page<>(
                List.of(
                    ApplicationFixtures.anApplicationListItem().toBuilder().id("my-application-1").name("My first application").build(),
                    ApplicationFixtures.anApplicationListItem().toBuilder().id("my-application-2").name("My second application").build()
                ),
                1,
                10,
                2
            )
        );

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var subscribersResponse = response.readEntity(SubscribersResponse.class);

        // Check data
        assertEquals(2, subscribersResponse.getData().size());

        assertEquals("my-application-1", subscribersResponse.getData().get(0).getId());
        assertEquals("my-application-2", subscribersResponse.getData().get(1).getId());

        // Check pagination
        Pagination pagination = subscribersResponse.getPagination();
        assertEquals(1L, pagination.getPage().longValue());
        assertEquals(10L, pagination.getPerPage().longValue());
        assertEquals(2L, pagination.getPageItemsCount().longValue());
        assertEquals(2L, pagination.getTotalCount().longValue());
        assertEquals(1L, pagination.getPageCount().longValue());

        // Check links
        Links links = subscribersResponse.getLinks();
        assert links != null;
        assertNotNull(links.getSelf());
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void should_return_search_multi_criteria() {
        var subscriptionEntity1 = SubscriptionFixtures.aSubscriptionEntity().toBuilder().application("my-application-1").build();
        var subscriptionEntity2 = SubscriptionFixtures.aSubscriptionEntity().toBuilder().application("my-application-2").build();
        when(subscriptionService.findByApi(GraviteeContext.getExecutionContext(), API)).thenReturn(
            List.of(subscriptionEntity1, subscriptionEntity2)
        );

        var applicationQuery = ApplicationQuery.builder().ids(Set.of("my-application-1", "my-application-2")).name("first").build();
        when(
            applicationService.search(
                eq(GraviteeContext.getExecutionContext()),
                eq(applicationQuery),
                any(Sortable.class),
                any(Pageable.class)
            )
        ).thenReturn(
            new Page<>(
                List.of(
                    ApplicationFixtures.anApplicationListItem().toBuilder().id("my-application-1").name("My first application").build()
                ),
                2,
                20,
                1
            )
        );

        final Response response = rootTarget().queryParam("name", "first").queryParam("page", 2).queryParam("perPage", 20).request().get();

        assertEquals(OK_200, response.getStatus());

        var subscribersResponse = response.readEntity(SubscribersResponse.class);

        // Check data
        assertEquals(1, subscribersResponse.getData().size());

        verify(applicationService).search(
            eq(GraviteeContext.getExecutionContext()),
            eq(applicationQuery),
            eq(new SortableImpl("name", true)),
            eq(new PageableImpl(2, 20))
        );
    }
}
