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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.SubscriptionFixtures;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ApiSubscriptionsResource_ExportTest extends AbstractApiSubscriptionsResourceTest {

    protected static final String CSV_HEADERS = "Plan;Application;Creation date;Process date;Start date;End date date;Status";
    protected static final String CSV_CONTENT =
        "Plan value;Application value;Creation date value;Process date value;Start date value;End date date value;Status value";
    protected static final String CSV = CSV_HEADERS + "\n" + CSV_CONTENT;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/_export";
    }

    @Test
    public void should_return_empty_export_if_no_subscriptions() {
        var subscriptionQuery = SubscriptionQuery.builder().apis(List.of(API)).statuses(Set.of(SubscriptionStatus.ACCEPTED)).build();

        when(
            subscriptionService.search(eq(GraviteeContext.getExecutionContext()), eq(subscriptionQuery), any(), eq(false), eq(false))
        ).thenReturn(new Page<>(List.of(), 0, 0, 0));

        when(subscriptionService.exportAsCsv(any(), any())).thenReturn(CSV_HEADERS);

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());
        assertTrue(response.getHeaderString("content-disposition").matches("attachment;filename=subscriptions-" + API + "-\\d+.csv"));

        var csv = response.readEntity(String.class);
        assertEquals(CSV_HEADERS, csv);

        verify(subscriptionService, never()).getMetadata(any(), any());
        verify(subscriptionService).exportAsCsv(List.of(), Map.of());
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
    public void should_export_subscriptions_as_csv() {
        var subscriptionQuery = SubscriptionQuery.builder().apis(List.of(API)).statuses(Set.of(SubscriptionStatus.ACCEPTED)).build();

        final List<SubscriptionEntity> subscriptionEntities = List.of(
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-1").build(),
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-2").build()
        );
        when(
            subscriptionService.search(eq(GraviteeContext.getExecutionContext()), eq(subscriptionQuery), any(), eq(false), eq(false))
        ).thenReturn(new Page<>(subscriptionEntities, 1, 10, 2));

        final Metadata metadata = new Metadata();
        metadata.put(API, "name", "my-api-name");
        metadata.put(PLAN, "name", "my-plan-name");
        metadata.put(APPLICATION, "name", "my-application-name");
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(metadata);
        when(subscriptionService.exportAsCsv(any(), any())).thenReturn(CSV);

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());
        assertTrue(response.getHeaderString("content-disposition").matches("attachment;filename=subscriptions-" + API + "-\\d+.csv"));

        var csv = response.readEntity(String.class);
        assertEquals(CSV, csv);

        verify(subscriptionService).getMetadata(
            eq(GraviteeContext.getExecutionContext()),
            argThat(metadataQuery -> {
                assertEquals(subscriptionEntities, metadataQuery.getSubscriptions());
                assertEquals(ORGANIZATION, metadataQuery.getOrganization());
                assertEquals(ENVIRONMENT, metadataQuery.getEnvironment());
                assertTrue(metadataQuery.ifApis().orElse(false));
                assertTrue(metadataQuery.ifApplications().orElse(false));
                assertTrue(metadataQuery.ifPlans().orElse(false));
                return true;
            })
        );
        verify(subscriptionService).exportAsCsv(subscriptionEntities, metadata.toMap());
    }

    @Test
    public void should_export_with_multi_criteria() {
        var subscriptionQuery = SubscriptionQuery.builder()
            .apis(List.of(API))
            .apiKey("apiKey")
            .plans(Set.of("plan-1", "plan-2"))
            .applications(Set.of("application-1", "application-2"))
            .statuses(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.CLOSED))
            .build();

        final List<SubscriptionEntity> subscriptionEntities = List.of(
            SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("subscription-1").build()
        );
        when(subscriptionService.search(any(), any(), any(), eq(false), eq(false))).thenReturn(new Page<>(subscriptionEntities, 1, 10, 1));

        final Metadata metadata = new Metadata();
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(metadata);
        when(subscriptionService.exportAsCsv(any(), any())).thenReturn(CSV);

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
        assertTrue(response.getHeaderString("content-disposition").matches("attachment;filename=subscriptions-" + API + "-\\d+.csv"));

        var csv = response.readEntity(String.class);
        assertEquals(CSV, csv);

        verify(subscriptionService).search(
            eq(GraviteeContext.getExecutionContext()),
            eq(subscriptionQuery),
            eq(new PageableImpl(2, 20)),
            eq(false),
            eq(false)
        );
        verify(subscriptionService).getMetadata(
            eq(GraviteeContext.getExecutionContext()),
            argThat(metadataQuery -> {
                assertEquals(subscriptionEntities, metadataQuery.getSubscriptions());
                assertEquals(ORGANIZATION, metadataQuery.getOrganization());
                assertEquals(ENVIRONMENT, metadataQuery.getEnvironment());
                assertTrue(metadataQuery.ifApis().orElse(false));
                assertTrue(metadataQuery.ifApplications().orElse(false));
                assertTrue(metadataQuery.ifPlans().orElse(false));
                return true;
            })
        );
        verify(subscriptionService).exportAsCsv(subscriptionEntities, metadata.toMap());
    }
}
