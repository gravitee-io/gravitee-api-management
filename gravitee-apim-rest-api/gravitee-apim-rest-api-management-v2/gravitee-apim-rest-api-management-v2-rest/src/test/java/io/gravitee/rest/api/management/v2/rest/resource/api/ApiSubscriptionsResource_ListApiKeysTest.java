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

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.SubscriptionFixtures;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionApiKeysResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ApiSubscriptionsResource_ListApiKeysTest extends AbstractApiSubscriptionsResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/" + SUBSCRIPTION + "/api-keys";
    }

    @Test
    public void should_return_404_if_not_found() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION));

        final Response response = rootTarget().request().get();
        assertThat(response)
            .hasStatus(HttpStatusCode.NOT_FOUND_404)
            .asError()
            .hasHttpStatus(NOT_FOUND_404)
            .hasMessage("Subscription [" + SUBSCRIPTION + "] cannot be found.");
    }

    @Test
    public void should_return_empty_page_if_no_api_keys() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION).referenceId(API).referenceType("API").build()
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION)).thenReturn(List.of());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var subscriptionApiKeysResponse = response.readEntity(SubscriptionApiKeysResponse.class);

        // Check data
        assertEquals(0, subscriptionApiKeysResponse.getData().size());

        // Check pagination
        Pagination pagination = subscriptionApiKeysResponse.getPagination();
        assertNull(pagination.getPage());
        assertNull(pagination.getPerPage());
        assertNull(pagination.getPageItemsCount());
        assertNull(pagination.getTotalCount());
        assertNull(pagination.getPageCount());

        // Check links
        Links links = subscriptionApiKeysResponse.getLinks();
        MAPIAssertions.assertThat(links).isEqualTo(new Links().self(rootTarget().getUri().toString()));
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
        MAPIAssertions.assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    public void should_return_list_of_api_keys() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION).referenceId(API).referenceType("API").build()
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION)).thenReturn(
            List.of(
                SubscriptionFixtures.anApiKeyEntity().toBuilder().id("api-key-1").key("custom1").build(),
                SubscriptionFixtures.anApiKeyEntity().toBuilder().id("api-key-2").key("custom2").build()
            )
        );

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var subscriptionApiKeysResponse = response.readEntity(SubscriptionApiKeysResponse.class);

        // Check data
        assertEquals(2, subscriptionApiKeysResponse.getData().size());

        assertEquals("api-key-1", subscriptionApiKeysResponse.getData().get(0).getId());
        assertEquals("api-key-2", subscriptionApiKeysResponse.getData().get(1).getId());

        // Check pagination
        Pagination pagination = subscriptionApiKeysResponse.getPagination();
        assertEquals(1L, pagination.getPage().longValue());
        assertEquals(10L, pagination.getPerPage().longValue());
        assertEquals(2L, pagination.getPageItemsCount().longValue());
        assertEquals(2L, pagination.getTotalCount().longValue());
        assertEquals(1L, pagination.getPageCount().longValue());

        // Check links
        Links links = subscriptionApiKeysResponse.getLinks();
        assert links != null;
        assertNotNull(links.getSelf());
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }
}
