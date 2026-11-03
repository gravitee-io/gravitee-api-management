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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.PortalNotificationServicesInMemory;
import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.apim.core.portal_notification.use_case.GetUserPortalNotificationFiltersUseCase;
import io.gravitee.apim.core.portal_notification.use_case.SearchUserPortalNotificationsUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationFilters;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationReference;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserNotificationsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private PortalNotificationServicesInMemory portalNotifications;

    @Autowired
    private ApiCrudServiceInMemory apiCrudService;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudService;

    @Override
    protected String contextPath() {
        return "user";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        doCallRealMethod().when(portalNotificationMapper).convert(any(SearchUserPortalNotificationsUseCase.Output.class));
        doCallRealMethod().when(portalNotificationMapper).convert(any(GetUserPortalNotificationFiltersUseCase.Output.class));

        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").name("Payments").environmentId(ENV_ID).build()));
        applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id("app-1").name("Shop").environmentId(ENV_ID).build()));
        portalNotifications.initWith(
            List.of(
                notification("n-1", 3).apiId("api-1").applicationId("app-1").hook("SUBSCRIPTION_ACCEPTED").build(),
                notification("n-2", 2).apiId("deleted-api").readAt(new Date(1000)).build(),
                notification("legacy", 1).environmentId(null).build(),
                notification("other-env", 4).environmentId("other-env").build(),
                notification("other-user", 5).user("someone-else").build()
            )
        );
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNotifications.reset();
        apiCrudService.reset();
        applicationCrudService.reset();
    }

    @Test
    public void should_list_notifications_of_environment_with_read_state_and_references() {
        final Response response = target().path("notifications").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(PortalNotificationsResponse.class);
        assertThat(body.getData()).extracting("id").containsExactly("n-1", "n-2", "legacy");
        var first = body.getData().getFirst();
        assertThat(first.getRead()).isFalse();
        assertThat(first.getHook()).isEqualTo("SUBSCRIPTION_ACCEPTED");
        assertThat(first.getApi()).isEqualTo(new PortalNotificationReference().id("api-1").name("Payments"));
        assertThat(first.getApplication()).isEqualTo(new PortalNotificationReference().id("app-1").name("Shop"));
        var deletedApi = body.getData().get(1);
        assertThat(deletedApi.getRead()).isTrue();
        assertThat(deletedApi.getApi()).isEqualTo(new PortalNotificationReference().id("deleted-api").name(null));
        var legacy = body.getData().get(2);
        assertThat(legacy.getHook()).isNull();
        assertThat(legacy.getApi()).isNull();
        assertThat(legacy.getApplication()).isNull();
    }

    @Test
    public void should_page_with_total_from_search() {
        final Response response = target().path("notifications").queryParam("page", 2).queryParam("size", 2).request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(PortalNotificationsResponse.class);
        assertThat(body.getData()).extracting("id").containsExactly("legacy");
        assertThat(paginationTotal(body)).isEqualTo(3);
    }

    @Test
    public void should_filter_unread_notifications_of_an_api_and_an_application() {
        final Response response = target()
            .path("notifications")
            .queryParam("read", false)
            .queryParam("apiId", "api-1")
            .queryParam("applicationId", "app-1")
            .request()
            .get();

        var body = response.readEntity(PortalNotificationsResponse.class);
        assertThat(body.getData()).extracting("id").containsExactly("n-1");
    }

    @Test
    public void should_count_unread_notifications_with_a_page_of_one() {
        final Response response = target().path("notifications").queryParam("read", false).queryParam("size", 1).request().get();

        var body = response.readEntity(PortalNotificationsResponse.class);
        assertThat(paginationTotal(body)).isEqualTo(2);
    }

    @Test
    public void should_return_filters() {
        final Response response = target().path("notifications").path("_filters").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var filters = response.readEntity(PortalNotificationFilters.class);
        assertThat(filters.getApis()).containsExactly(
            new PortalNotificationReference().id("api-1").name("Payments"),
            new PortalNotificationReference().id("deleted-api").name(null)
        );
        assertThat(filters.getApplications()).containsExactly(new PortalNotificationReference().id("app-1").name("Shop"));
    }

    @Test
    public void should_mark_a_notification_as_read() {
        final Response response = target().path("notifications").path("n-1").path("_read").request().post(Entity.json(null));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        assertThat(find("n-1").isRead()).isTrue();
    }

    @Test
    public void should_not_find_a_notification_of_another_environment_to_mark_as_read() {
        final Response response = target().path("notifications").path("other-env").path("_read").request().post(Entity.json(null));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(find("other-env").isRead()).isFalse();
    }

    @Test
    public void should_not_find_a_notification_of_another_user_to_mark_as_read() {
        final Response response = target().path("notifications").path("other-user").path("_read").request().post(Entity.json(null));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(find("other-user").isRead()).isFalse();
    }

    @Test
    public void should_mark_all_notifications_of_environment_as_read() {
        final Response response = target().path("notifications").path("_read").request().post(Entity.json(null));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        assertThat(List.of(find("n-1"), find("n-2"), find("legacy"))).allMatch(PortalNotification::isRead);
        assertThat(find("other-env").isRead()).isFalse();
        assertThat(find("other-user").isRead()).isFalse();
    }

    @Test
    public void should_delete_all_notifications_of_environment_only() {
        final Response response = target().path("notifications").request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        assertThat(portalNotifications.storage())
            .extracting(PortalNotification::getId)
            .containsExactlyInAnyOrder("other-env", "other-user");
    }

    @Test
    public void should_delete_a_notification() {
        final Response response = target().path("notifications").path("n-1").request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        assertThat(portalNotifications.storage()).extracting(PortalNotification::getId).doesNotContain("n-1");
    }

    @Test
    public void should_not_delete_a_notification_of_another_environment() {
        final Response response = target().path("notifications").path("other-env").request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(portalNotifications.storage()).extracting(PortalNotification::getId).contains("other-env");
    }

    @Test
    public void should_not_delete_a_notification_of_another_user() {
        final Response response = target().path("notifications").path("other-user").request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(portalNotifications.storage()).extracting(PortalNotification::getId).contains("other-user");
    }

    private PortalNotification find(String id) {
        return portalNotifications
            .storage()
            .stream()
            .filter(n -> n.getId().equals(id))
            .findFirst()
            .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Number paginationTotal(PortalNotificationsResponse body) {
        return (Number) ((Map<String, Object>) body.getMetadata().get("pagination")).get("total");
    }

    private static PortalNotification.PortalNotificationBuilder notification(String id, int minutesAfterEpoch) {
        return PortalNotification.builder()
            .id(id)
            .title("title-" + id)
            .message("message-" + id)
            .user(USER_NAME)
            .environmentId(ENV_ID)
            .createdAt(new Date(minutesAfterEpoch * 60_000L));
    }
}
