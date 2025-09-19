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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.rest.model.wrapper.ApplicationListItemPagedResult;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscribersResourceTest extends AbstractResourceTest {

    private static final String API_ID = "my-api";

    @Override
    protected String contextPath() {
        return "apis/" + API_ID + "";
    }

    @Before
    public void init() throws IOException {
        reset(apiKeyService, subscriptionService, applicationService);
        GraviteeContext.cleanContext();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API_ID);
        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(user);
        mockApi.setPrimaryOwner(primaryOwner);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetApiSubscribers() {
        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API_ID);
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API_ID);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API_ID);
        doReturn(Arrays.asList(subB1, subC1, subA1)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        ApplicationListItem appA = new ApplicationListItem();
        appA.setId("A");
        ApplicationListItem appB = new ApplicationListItem();
        appB.setId("B");
        ApplicationListItem appC = new ApplicationListItem();
        appC.setId("C");
        Page<ApplicationListItem> applications = new Page(Arrays.asList(appA, appB, appC), 1, 10, 42);

        doReturn(applications)
            .when(applicationService)
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(q -> q.getIds().containsAll(Arrays.asList("A", "B", "C"))),
                eq(new SortableImpl("name", true)),
                argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 20)
            );

        final Response response = envTarget(API_ID).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        final Collection<ApplicationListItem> applicationsResponse = response.readEntity(new GenericType<>() {});
        assertNotNull(applicationsResponse);
        assertEquals(3, applicationsResponse.size());
    }

    @Test
    public void shouldGetNoSubscribers() {
        doReturn(Collections.emptyList()).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = envTarget(API_ID).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        final Collection<ApplicationListItem> applicationsResponse = response.readEntity(new GenericType<>() {});
        assertNotNull(applicationsResponse);
        assertEquals(0, applicationsResponse.size());
    }

    @Test
    public void shouldNotFilterOnSubscriptionStatus() {
        envTarget(API_ID).path("subscribers").request().get();
        verify(subscriptionService).search(
            eq(GraviteeContext.getExecutionContext()),
            argThat(query -> CollectionUtils.isEmpty(query.getStatuses()))
        );
    }

    @Test
    public void shouldGetApiSubscribersWithPagination() {
        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API_ID);
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API_ID);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API_ID);
        doReturn(Arrays.asList(subB1, subC1, subA1)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        ApplicationListItem appA = new ApplicationListItem();
        appA.setId("A");
        ApplicationListItem appB = new ApplicationListItem();
        appB.setId("B");
        ApplicationListItem appC = new ApplicationListItem();
        appC.setId("C");
        Page<ApplicationListItem> applications = new Page(Arrays.asList(appA, appB, appC), 1, 10, 42);

        doReturn(applications)
            .when(applicationService)
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(q -> q.getIds().containsAll(Arrays.asList("A", "B", "C"))),
                eq(new SortableImpl("name", true)),
                argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 20)
            );

        final Response response = envTarget(API_ID).path("subscribers/_paged").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApplicationListItemPagedResult applicationsResponse = response.readEntity(ApplicationListItemPagedResult.class);
        assertNotNull(applicationsResponse);
        assertEquals(3, applicationsResponse.getData().size());
    }

    @Test
    public void shouldGetNoSubscribersWithPagination() {
        doReturn(Collections.emptyList()).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = envTarget(API_ID).path("subscribers/_paged").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApplicationListItemPagedResult applicationsResponse = response.readEntity(ApplicationListItemPagedResult.class);
        assertNotNull(applicationsResponse);
        assertNull(applicationsResponse.getData());
    }

    @Test
    public void shouldNotFilterOnSubscriptionStatusWithPagination() {
        envTarget(API_ID).path("subscribers/_paged").request().get();
        verify(subscriptionService).search(
            eq(GraviteeContext.getExecutionContext()),
            argThat(query -> CollectionUtils.isEmpty(query.getStatuses()))
        );
    }
}
