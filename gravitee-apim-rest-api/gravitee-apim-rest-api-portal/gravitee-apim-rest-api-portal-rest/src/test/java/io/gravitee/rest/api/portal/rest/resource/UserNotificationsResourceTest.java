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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.notification.PortalNotificationEntity;
import io.gravitee.rest.api.portal.rest.model.PortalNotification;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationsResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserNotificationsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "user";
    }

    @BeforeEach
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();

        PortalNotificationEntity portalNotificationEntity1 = new PortalNotificationEntity();
        portalNotificationEntity1.setCreatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
        portalNotificationEntity1.setId("1");

        PortalNotificationEntity portalNotificationEntity2 = new PortalNotificationEntity();
        portalNotificationEntity2.setCreatedAt(Date.from(Instant.parse("2019-02-10T12:00:00.00Z")));
        portalNotificationEntity2.setId("2");

        PortalNotificationEntity portalNotificationEntity3 = new PortalNotificationEntity();
        portalNotificationEntity3.setCreatedAt(Date.from(Instant.parse("1970-01-01T00:00:00.00Z")));
        portalNotificationEntity3.setId("3");

        doReturn(Arrays.asList(portalNotificationEntity1, portalNotificationEntity2, portalNotificationEntity3))
            .when(portalNotificationService)
            .findByUser(any());
        Mockito.doCallRealMethod().when(portalNotificationMapper).convert(any());
    }

    @Test
    public void shouldGetCurrentUserNotifications() {
        final Response response = target().path("notifications").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        PortalNotificationsResponse notificationsResponse = response.readEntity(PortalNotificationsResponse.class);
        assertNotNull(notificationsResponse);
        List<PortalNotification> data = notificationsResponse.getData();
        assertEquals(3, data.size());
        assertEquals("1", data.get(0).getId());
        assertEquals("2", data.get(1).getId());
        assertEquals("3", data.get(2).getId());
    }

    @Test
    public void shouldDeleteAllCurrentUserNotifications() {
        final Response response = target().path("notifications").request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        Mockito.verify(portalNotificationService).deleteAll(USER_NAME);
    }

    @Test
    public void shouldDeleteACurrentUserNotification() {
        resetAllMocks();

        PortalNotificationEntity portalNotificationEntity = new PortalNotificationEntity();
        portalNotificationEntity.setCreatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
        portalNotificationEntity.setId("ID");

        doReturn(List.of(portalNotificationEntity)).when(portalNotificationService).findByUser(USER_NAME);

        final Response response = target().path("notifications").path("ID").request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        Mockito.verify(portalNotificationService).findByUser(USER_NAME);
        Mockito.verify(portalNotificationService).delete("ID");
    }

    @Test
    public void shouldNotDeleteACurrentUserNotificationBecauseNotificationDoesNotExist() {
        resetAllMocks();

        doReturn(List.of()).when(portalNotificationService).findByUser(USER_NAME);

        final Response response = target().path("notifications").path("ID").request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        Mockito.verify(portalNotificationService).findByUser(USER_NAME);
        Mockito.verify(portalNotificationService, never()).delete("ID");
    }

    @Test
    public void shouldNotDeleteACurrentUserNotificationBecauseNotificationDoesNotBelongToUser() {
        resetAllMocks();

        PortalNotificationEntity portalNotificationEntity = new PortalNotificationEntity();
        portalNotificationEntity.setCreatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
        portalNotificationEntity.setId("Another_ID");

        doReturn(List.of(portalNotificationEntity)).when(portalNotificationService).findByUser(USER_NAME);

        final Response response = target().path("notifications").path("ID").request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        Mockito.verify(portalNotificationService).findByUser(USER_NAME);
        Mockito.verify(portalNotificationService, never()).delete("ID");
    }
}
