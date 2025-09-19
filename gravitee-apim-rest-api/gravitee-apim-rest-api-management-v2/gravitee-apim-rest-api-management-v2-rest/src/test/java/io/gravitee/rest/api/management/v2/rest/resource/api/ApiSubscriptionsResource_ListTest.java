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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.ApplicationFixtures;
import fixtures.PlanFixtures;
import fixtures.SubscriptionFixtures;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionsResponse;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ApiSubscriptionsResource_ListTest extends AbstractApiSubscriptionsResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions";
    }

    @Test
    public void should_return_empty_page_if_no_subscriptions() {
        var subscriptionQuery = SubscriptionQuery.builder().apis(List.of(API)).statuses(Set.of(SubscriptionStatus.ACCEPTED)).build();

        when(
            subscriptionService.search(eq(GraviteeContext.getExecutionContext()), eq(subscriptionQuery), any(), eq(false), eq(false))
        ).thenReturn(new Page<>(List.of(), 0, 0, 0));

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var subscriptionsResponse = response.readEntity(SubscriptionsResponse.class);

        // Check data
        assertEquals(0, subscriptionsResponse.getData().size());

        // Check pagination
        Pagination pagination = subscriptionsResponse.getPagination();
        assertNull(pagination.getPage());
        assertNull(pagination.getPerPage());
        assertNull(pagination.getPageItemsCount());
        assertNull(pagination.getTotalCount());
        assertNull(pagination.getPageCount());

        // Check links
        Links links = subscriptionsResponse.getLinks();
        MAPIAssertions.assertThat(links).isEqualTo(Links.builder().self(rootTarget().getUri().toString()).build());
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
    public void should_return_list_of_subscriptions() {
        var subscriptionQuery = SubscriptionQuery.builder().apis(List.of(API)).statuses(Set.of(SubscriptionStatus.ACCEPTED)).build();

        when(
            subscriptionService.search(eq(GraviteeContext.getExecutionContext()), eq(subscriptionQuery), any(), eq(false), eq(false))
        ).thenReturn(
            new Page<>(
                List.of(
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-1").build(),
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-2").build()
                ),
                1,
                10,
                2
            )
        );

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var subscriptionsResponse = response.readEntity(SubscriptionsResponse.class);

        // Check data
        assertEquals(2, subscriptionsResponse.getData().size());

        assertEquals("subscription-1", subscriptionsResponse.getData().get(0).getId());
        assertEquals("subscription-2", subscriptionsResponse.getData().get(1).getId());

        // Check pagination
        Pagination pagination = subscriptionsResponse.getPagination();
        assertEquals(1L, pagination.getPage().longValue());
        assertEquals(10L, pagination.getPerPage().longValue());
        assertEquals(2L, pagination.getPageItemsCount().longValue());
        assertEquals(2L, pagination.getTotalCount().longValue());
        assertEquals(1L, pagination.getPageCount().longValue());

        // Check links
        Links links = subscriptionsResponse.getLinks();
        assert links != null;
        assertNotNull(links.getSelf());
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void should_return_search_multi_criteria() {
        var subscriptionQuery = SubscriptionQuery.builder()
            .apis(List.of(API))
            .apiKey("apiKey")
            .plans(Set.of("plan-1", "plan-2"))
            .applications(Set.of("application-1", "application-2"))
            .statuses(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.CLOSED))
            .build();

        when(subscriptionService.search(any(), any(), any(), eq(false), eq(false))).thenReturn(
            new Page<>(List.of(SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-1").build()), 1, 10, 1)
        );

        final Response response = rootTarget()
            .queryParam("applicationIds", "application-1, application-2")
            .queryParam("planIds", "plan-1, plan-2")
            .queryParam("statuses", "ACCEPTED, CLOSED")
            .queryParam("apiKey", "apiKey")
            .queryParam("page", 2)
            .queryParam("perPage", 20)
            .request()
            .get();

        assertEquals(OK_200, response.getStatus());

        var subscriptionsResponse = response.readEntity(SubscriptionsResponse.class);

        // Check data
        assertEquals(1, subscriptionsResponse.getData().size());

        verify(subscriptionService).search(
            eq(GraviteeContext.getExecutionContext()),
            eq(subscriptionQuery),
            eq(new PageableImpl(2, 20)),
            eq(false),
            eq(false)
        );
    }

    @Test
    public void should_return_list_of_subscriptions_with_expands() {
        var subscriptionQuery = SubscriptionQuery.builder().apis(List.of(API)).statuses(Set.of(SubscriptionStatus.ACCEPTED)).build();

        final List<SubscriptionEntity> subscriptionsEntities = List.of(
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-1").plan("plan-1").application("application-1").build(),
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-2").plan("plan-2").application("application-2").build(),
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-3").plan("plan-3").application("application-1").build(),
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-4").plan("plan-2").application("application-1").build()
        );

        when(
            subscriptionService.search(eq(GraviteeContext.getExecutionContext()), eq(subscriptionQuery), any(), eq(false), eq(false))
        ).thenReturn(new Page<>(subscriptionsEntities, 1, 10, 2));

        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of("plan-1", "plan-2", "plan-3"))).thenReturn(
            Set.of(
                PlanFixtures.aPlanEntityV4().toBuilder().id("plan-1").build(),
                PlanFixtures.aPlanEntityV4().toBuilder().id("plan-2").build(),
                PlanFixtures.aPlanEntityV4().toBuilder().id("plan-3").build()
            )
        );
        when(
            applicationService.findByIds(
                eq(GraviteeContext.getExecutionContext()),
                argThat(argument -> List.of("application-1", "application-2").containsAll(argument))
            )
        ).thenReturn(
            Set.of(
                ApplicationFixtures.anApplicationListItem().toBuilder().id("application-1").build(),
                ApplicationFixtures.anApplicationListItem().toBuilder().id("application-2").build()
            )
        );

        final Response response = rootTarget().queryParam("expands", "plan,application").request().get();

        assertEquals(OK_200, response.getStatus());

        var subscriptionsResponse = response.readEntity(SubscriptionsResponse.class);

        // Check data
        assertEquals(4, subscriptionsResponse.getData().size());

        for (int i = 0; i < subscriptionsResponse.getData().size(); i++) {
            final SubscriptionEntity subscriptionEntity = subscriptionsEntities.get(i);
            final Subscription subscription = subscriptionsResponse.getData().get(i);

            assertEquals(subscriptionEntity.getId(), subscription.getId());
            assertEquals(subscriptionEntity.getPlan(), subscription.getPlan().getId());
            assertNotNull(subscription.getPlan().getName());
            assertNotNull(subscription.getPlan().getDescription());
            assertEquals(subscriptionEntity.getApplication(), subscription.getApplication().getId());
            assertNotNull(subscription.getApplication().getName());
            assertNotNull(subscription.getApplication().getDescription());
        }
    }
}
