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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.portal.rest.resource.ApplicationsResource.METADATA_SUBSCRIPTIONS_KEY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResourceTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";

    @Override
    protected String contextPath() {
        return "applications";
    }

    @Before
    public void init() {
        resetAllMocks();

        ApplicationListItem applicationA = new ApplicationListItem();
        applicationA.setId("A");
        applicationA.setName("A");

        ApplicationListItem applicationB = new ApplicationListItem();
        applicationB.setId("B");
        applicationB.setName("B");

        doReturn(new HashSet<>(Arrays.asList("A", "B")))
            .when(applicationService)
            .findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), any());
        doReturn(new HashSet<>(Arrays.asList(applicationA, applicationB)))
            .when(applicationService)
            .findByIds(eq(GraviteeContext.getExecutionContext()), eq(Arrays.asList("A", "B")));
        doReturn(new HashSet<>(Arrays.asList(applicationB, applicationA)))
            .when(applicationService)
            .findByIds(eq(GraviteeContext.getExecutionContext()), eq(Arrays.asList("B", "A")));

        doReturn(new HashSet<>(Arrays.asList(applicationB)))
            .when(applicationService)
            .findByIds(eq(GraviteeContext.getExecutionContext()), eq(List.of("B")));

        doReturn(new Application().id("A").name("A"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationA), any());

        doReturn(new Application().id("B").name("B"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationB), any());

        ApplicationEntity createdEntity = mock(ApplicationEntity.class);
        doReturn("NEW").when(createdEntity).getId();
        doReturn(createdEntity).when(applicationService).create(eq(GraviteeContext.getExecutionContext()), any(), any());
        doReturn(new Application().id("NEW"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(createdEntity), any());
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldGetApplications() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Sortable sort = new SortableImpl("name", true);
        Mockito.verify(applicationService).findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(sort));

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        Mockito.verify(applicationMapper, Mockito.times(2)).computeApplicationLinks(ac.capture(), eq(null));

        String expectedBasePath = target().getUri().toString();
        List<String> bastPathList = ac.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath + "/A"));
        assertTrue(bastPathList.contains(expectedBasePath + "/B"));

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());
        assertEquals("A", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetApplicationsOrderByName() {
        ApplicationListItem applicationA = new ApplicationListItem();
        applicationA.setId("A");
        applicationA.setName("A");

        ApplicationListItem applicationB = new ApplicationListItem();
        applicationB.setId("B");
        applicationB.setName("b");

        ApplicationListItem applicationC = new ApplicationListItem();
        applicationC.setId("C");
        applicationC.setName("C");

        ApplicationListItem applicationD = new ApplicationListItem();
        applicationD.setId("D");
        applicationD.setName("d");

        Set<ApplicationListItem> mockApplications = new HashSet<>(Arrays.asList(applicationA, applicationB, applicationC, applicationD));
        Sortable sort = new SortableImpl("name", true);
        doReturn(new HashSet<>(Arrays.asList("A", "B", "C", "D")))
            .when(applicationService)
            .findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(sort));
        doReturn(mockApplications)
            .when(applicationService)
            .findByIds(eq(GraviteeContext.getExecutionContext()), eq(Arrays.asList("A", "B", "C", "D")));

        doReturn(new Application().id("A").name("A"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationA), any());
        doReturn(new Application().id("B").name("b"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationB), any());
        doReturn(new Application().id("C").name("C"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationC), any());
        doReturn(new Application().id("D").name("d"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationD), any());

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(4, applicationsResponse.getData().size());
        assertEquals("A", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());
        assertEquals("C", applicationsResponse.getData().get(2).getId());
        assertEquals("D", applicationsResponse.getData().get(3).getId());

        Mockito.verify(filteringService, times(0)).getApplicationsOrderByNumberOfSubscriptions(anySet(), eq(Order.DESC));
    }

    @Test
    public void shouldGetApplicationsOrderByNbSubscriptionsDesc() {
        ApplicationListItem applicationA = new ApplicationListItem();
        applicationA.setId("A");
        ApplicationListItem applicationB = new ApplicationListItem();
        applicationB.setId("B");
        Collection<String> mockFilteredApp = Arrays.asList(applicationB.getId(), applicationA.getId());
        doReturn(mockFilteredApp).when(filteringService).getApplicationsOrderByNumberOfSubscriptions(anyCollection(), eq(Order.DESC));

        final Response response = target().queryParam("order", "-nbSubscriptions").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(filteringService).getApplicationsOrderByNumberOfSubscriptions(anyCollection(), eq(Order.DESC));

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());
        assertEquals("B", applicationsResponse.getData().get(0).getId());
        assertEquals("A", applicationsResponse.getData().get(1).getId());
    }

    @Test
    public void shouldGetApplicationsOrderByNbSubscriptionsAsc() {
        ApplicationListItem applicationA = new ApplicationListItem();
        applicationA.setId("A");
        ApplicationListItem applicationB = new ApplicationListItem();
        applicationB.setId("B");
        Collection<String> mockFilteredApp = Arrays.asList(applicationA.getId(), applicationB.getId());
        doReturn(mockFilteredApp).when(filteringService).getApplicationsOrderByNumberOfSubscriptions(anyCollection(), eq(Order.ASC));

        final Response response = target().queryParam("order", "nbSubscriptions").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(filteringService).getApplicationsOrderByNumberOfSubscriptions(anyCollection(), eq(Order.ASC));

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());
        assertEquals("A", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());
    }

    @Test
    public void shouldGetApplicationsWithPaginatedLink() {
        final Response response = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(1, applicationsResponse.getData().size());
        assertEquals("B", applicationsResponse.getData().get(0).getId());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetApplicationsWithSubscriptions() {
        doReturn(true)
            .when(permissionService)
            .hasPermission(any(), eq(RolePermission.APPLICATION_SUBSCRIPTION), any(), eq(RolePermissionAction.READ));
        List<SubscriptionEntity> subscriptions = new ArrayList<>();
        SubscriptionEntity sub1 = createSubscriptionEntity("sub-1", "A");
        SubscriptionEntity sub2 = createSubscriptionEntity("sub-2", "A");
        SubscriptionEntity sub3 = createSubscriptionEntity("sub-3", "B");

        subscriptions.addAll(Arrays.asList(sub1, sub2, sub3));
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(Arrays.asList("A", "B"));
        query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED));
        doReturn(subscriptions).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        doReturn(new Subscription().application("A")).when(subscriptionMapper).convert(sub1);
        doReturn(new Subscription().application("A")).when(subscriptionMapper).convert(sub2);
        doReturn(new Subscription().application("B")).when(subscriptionMapper).convert(sub3);

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);

        Map<String, Object> metadataSubscriptions = applicationsResponse.getMetadata().get(METADATA_SUBSCRIPTIONS_KEY);

        assertEquals(2, metadataSubscriptions.size());
        assertEquals(2, ((List) metadataSubscriptions.get("A")).size());
        assertEquals(1, ((List) metadataSubscriptions.get("B")).size());
    }

    @Test
    public void shouldGetAllApplicationsWithoutSubscriptions() {
        doReturn(true)
            .when(permissionService)
            .hasPermission(any(), eq(RolePermission.APPLICATION_SUBSCRIPTION), any(), eq(RolePermissionAction.READ));
        List<SubscriptionEntity> subscriptions = new ArrayList<>();
        SubscriptionEntity sub1 = createSubscriptionEntity("sub-1", "A");
        SubscriptionEntity sub2 = createSubscriptionEntity("sub-2", "A");
        SubscriptionEntity sub3 = createSubscriptionEntity("sub-3", "B");

        subscriptions.addAll(Arrays.asList(sub1, sub2, sub3));
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(Arrays.asList("A", "B"));
        query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED));
        doReturn(subscriptions).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        doReturn(new Subscription().application("A")).when(subscriptionMapper).convert(sub1);
        doReturn(new Subscription().application("A")).when(subscriptionMapper).convert(sub2);
        doReturn(new Subscription().application("B")).when(subscriptionMapper).convert(sub3);

        ApplicationListItem appA = mock(ApplicationListItem.class);
        ApplicationListItem appB = mock(ApplicationListItem.class);
        Collection<ApplicationListItem> applications = new HashSet<>(Arrays.asList(appA, appB));
        doReturn(applications).when(applicationService).findByUser(any(), any(), any());

        final Response response = target().queryParam("size", -1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);
        assertNotNull(applicationsResponse.getMetadata());
        assertTrue(applicationsResponse.getMetadata().isEmpty());
    }

    @Test
    public void shouldGetAllApplicationsForSubscription() {
        doReturn(true)
            .when(permissionService)
            .hasPermission(any(), eq(RolePermission.APPLICATION_SUBSCRIPTION), any(), eq(RolePermissionAction.READ));
        List<SubscriptionEntity> subscriptions = new ArrayList<>();
        SubscriptionEntity sub1 = createSubscriptionEntity("sub-1", "A");
        SubscriptionEntity sub2 = createSubscriptionEntity("sub-2", "A");
        SubscriptionEntity sub3 = createSubscriptionEntity("sub-3", "B");

        subscriptions.addAll(Arrays.asList(sub1, sub2, sub3));
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(Arrays.asList("A", "B"));
        query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED));
        doReturn(subscriptions).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        doReturn(new Subscription().application("A")).when(subscriptionMapper).convert(sub1);
        doReturn(new Subscription().application("A")).when(subscriptionMapper).convert(sub2);
        doReturn(new Subscription().application("B")).when(subscriptionMapper).convert(sub3);

        ApplicationListItem appA = mock(ApplicationListItem.class);
        ApplicationListItem appB = mock(ApplicationListItem.class);
        Collection<ApplicationListItem> applications = Arrays.asList(appA, appB);
        doReturn(applications)
            .when(applicationService)
            .findByUserAndPermission(any(), any(), any(), eq(RolePermission.APPLICATION_SUBSCRIPTION), eq(RolePermissionAction.CREATE));

        final Response response = target().queryParam("size", -1).queryParam("forSubscription", true).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);
        assertNotNull(applicationsResponse.getMetadata());
        assertTrue(applicationsResponse.getMetadata().isEmpty());
    }

    @Test
    public void shouldGetNoApplication() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertEquals("errors.pagination.invalid", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Pagination is not valid", error.getMessage());
    }

    @Test
    public void shouldGetNoApplicationAndNoLink() {
        Sortable sort = new SortableImpl("name", true);
        doReturn(new HashSet<>()).when(applicationService).findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(sort));

        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(0, applicationsResponse.getData().size());

        Links links = applicationsResponse.getLinks();
        assertNull(links);

        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        applicationsResponse = anotherResponse.readEntity(ApplicationsResponse.class);
        assertEquals(0, applicationsResponse.getData().size());

        links = applicationsResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldCreateApplicationWithoutSettings() {
        ApplicationInput input = new ApplicationInput().description(APPLICATION).groups(Arrays.asList(APPLICATION)).name(APPLICATION);

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(target().path("NEW").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(eq(GraviteeContext.getExecutionContext()), captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        assertNull(settings.getOAuthClient());
        assertEquals(ApiKeyMode.UNSPECIFIED, value.getApiKeyMode());

        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldCreateApplicationWithEmptySettings() {
        ApplicationInput input = new ApplicationInput()
            .description(APPLICATION)
            .groups(Arrays.asList(APPLICATION))
            .name(APPLICATION)
            .settings(new io.gravitee.rest.api.portal.rest.model.ApplicationSettings())
            .apiKeyMode(ApiKeyModeEnum.SHARED);

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(eq(GraviteeContext.getExecutionContext()), captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        assertNull(settings.getOAuthClient());
        assertEquals(ApiKeyMode.SHARED, value.getApiKeyMode());

        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldCreateApplicationWithSimpleSettings() {
        ApplicationInput input = new ApplicationInput().description(APPLICATION).groups(Arrays.asList(APPLICATION)).name(APPLICATION);

        SimpleApplicationSettings sas = new SimpleApplicationSettings().clientId(APPLICATION).type(APPLICATION);
        input.setSettings(new io.gravitee.rest.api.portal.rest.model.ApplicationSettings().app(sas));

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(eq(GraviteeContext.getExecutionContext()), captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        final io.gravitee.rest.api.model.application.SimpleApplicationSettings app = settings.getApp();
        assertNotNull(app);
        assertEquals(APPLICATION, app.getClientId());
        assertEquals(APPLICATION, app.getType());
        assertNull(settings.getOAuthClient());

        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldCreateApplicationWithOauthSettings() {
        ApplicationInput input = new ApplicationInput().description(APPLICATION).groups(Arrays.asList(APPLICATION)).name(APPLICATION);

        OAuthClientSettings oacs = new OAuthClientSettings()
            .applicationType(APPLICATION)
            .clientId(APPLICATION)
            .clientSecret(APPLICATION)
            .clientUri(APPLICATION)
            .logoUri(APPLICATION)
            .grantTypes(Arrays.asList(APPLICATION))
            .redirectUris(Arrays.asList(APPLICATION))
            .responseTypes(Arrays.asList(APPLICATION))
            .renewClientSecretSupported(Boolean.TRUE);
        input.setSettings(new io.gravitee.rest.api.portal.rest.model.ApplicationSettings().oauth(oacs));

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(eq(GraviteeContext.getExecutionContext()), captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        final io.gravitee.rest.api.model.application.OAuthClientSettings oauthClientSettings = settings.getOAuthClient();
        assertNotNull(oauthClientSettings);
        assertEquals(APPLICATION, oauthClientSettings.getApplicationType());
        final List<String> grantTypes = oauthClientSettings.getGrantTypes();
        assertNotNull(grantTypes);
        assertFalse(grantTypes.isEmpty());
        assertEquals(APPLICATION, grantTypes.get(0));

        final List<String> redirectUris = oauthClientSettings.getRedirectUris();
        assertNotNull(redirectUris);
        assertFalse(redirectUris.isEmpty());
        assertEquals(APPLICATION, redirectUris.get(0));

        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldHaveBadRequestWhileCreatingApplication() {
        final Response response = target().request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    private SubscriptionEntity createSubscriptionEntity(String id, String application) {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(id);
        subscriptionEntity.setApplication(application);
        return subscriptionEntity;
    }
}
