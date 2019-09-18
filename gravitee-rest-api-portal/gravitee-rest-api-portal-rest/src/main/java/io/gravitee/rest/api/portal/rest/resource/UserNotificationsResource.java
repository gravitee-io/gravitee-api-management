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
package io.gravitee.rest.api.portal.rest.resource;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.notification.PortalNotificationEntity;
import io.gravitee.rest.api.portal.rest.mapper.PortalNotificationMapper;
import io.gravitee.rest.api.portal.rest.model.PortalNotification;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.PortalNotificationService;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserNotificationsResource extends AbstractResource  {

    @Autowired
    private PortalNotificationService portalNotificationService;

    @Autowired
    private PortalNotificationMapper portalNotificationMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUserNotifications(@BeanParam PaginationParam paginationParam)  {
        List<PortalNotification> notifications = portalNotificationService.findByUser(getAuthenticatedUser())
                .stream()
                .sorted(Comparator.comparing(PortalNotificationEntity::getCreatedAt))
                .map(portalNotificationMapper::convert)
                .collect(Collectors.toList());

        return createListResponse(notifications, paginationParam);
    }

    @DELETE
    public Response deleteAll() {
        portalNotificationService.deleteAll(getAuthenticatedUser());
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @Path("{notificationId}")
    @DELETE
    public Response delete(@PathParam("notificationId") String notificationId) {
        portalNotificationService.delete(notificationId);
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }
}
