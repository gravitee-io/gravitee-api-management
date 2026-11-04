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

import io.gravitee.apim.core.portal_notification.use_case.DeleteUserPortalNotificationsUseCase;
import io.gravitee.apim.core.portal_notification.use_case.GetUserPortalNotificationFiltersUseCase;
import io.gravitee.apim.core.portal_notification.use_case.MarkUserPortalNotificationsAsReadUseCase;
import io.gravitee.apim.core.portal_notification.use_case.SearchUserPortalNotificationsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.portal.rest.mapper.PortalNotificationMapper;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every operation is limited to the caller's notifications of the current environment and to those not tied to an
 * environment; a notification outside that scope is not found.
 */
public class UserNotificationsResource extends AbstractResource {

    @Autowired
    private SearchUserPortalNotificationsUseCase searchUserPortalNotificationsUseCase;

    @Autowired
    private GetUserPortalNotificationFiltersUseCase getUserPortalNotificationFiltersUseCase;

    @Autowired
    private MarkUserPortalNotificationsAsReadUseCase markUserPortalNotificationsAsReadUseCase;

    @Autowired
    private DeleteUserPortalNotificationsUseCase deleteUserPortalNotificationsUseCase;

    @Autowired
    private PortalNotificationMapper portalNotificationMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUserNotifications(
        @BeanParam PaginationParam paginationParam,
        @QueryParam("read") Boolean read,
        @QueryParam("apiId") String apiId,
        @QueryParam("applicationId") String applicationId
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = searchUserPortalNotificationsUseCase.execute(
            new SearchUserPortalNotificationsUseCase.Input(
                executionContext,
                getAuthenticatedUser(),
                read,
                apiId,
                applicationId,
                toPageable(paginationParam)
            )
        );

        Map<String, Map<String, Object>> metadata = new HashMap<>(
            Map.of("paginateMetaData", new HashMap<>(Map.of("totalElements", output.notifications().getTotalElements())))
        );
        return createListResponse(executionContext, portalNotificationMapper.convert(output), paginationParam, metadata);
    }

    @GET
    @Path("_filters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUserNotificationFilters() {
        var output = getUserPortalNotificationFiltersUseCase.execute(
            new GetUserPortalNotificationFiltersUseCase.Input(GraviteeContext.getExecutionContext(), getAuthenticatedUser())
        );
        return Response.ok(portalNotificationMapper.convert(output)).build();
    }

    @POST
    @Path("_read")
    public Response markAllAsRead() {
        markUserPortalNotificationsAsReadUseCase.execute(
            new MarkUserPortalNotificationsAsReadUseCase.Input(GraviteeContext.getExecutionContext(), getAuthenticatedUser(), null)
        );
        return Response.noContent().build();
    }

    @POST
    @Path("{notificationId}/_read")
    public Response markAsRead(@PathParam("notificationId") String notificationId) {
        markUserPortalNotificationsAsReadUseCase.execute(
            new MarkUserPortalNotificationsAsReadUseCase.Input(
                GraviteeContext.getExecutionContext(),
                getAuthenticatedUser(),
                notificationId
            )
        );
        return Response.noContent().build();
    }

    @DELETE
    public Response deleteAll() {
        deleteUserPortalNotificationsUseCase.execute(
            new DeleteUserPortalNotificationsUseCase.Input(GraviteeContext.getExecutionContext(), getAuthenticatedUser(), null)
        );
        return Response.noContent().build();
    }

    @Path("{notificationId}")
    @DELETE
    public Response delete(@PathParam("notificationId") String notificationId) {
        deleteUserPortalNotificationsUseCase.execute(
            new DeleteUserPortalNotificationsUseCase.Input(GraviteeContext.getExecutionContext(), getAuthenticatedUser(), notificationId)
        );
        return Response.noContent().build();
    }

    /**
     * {@code size=-1} asks for every notification. {@code size=0} is served as a page of one, since the repositories
     * reject an empty page; the response then has no data and no pagination block, so a count is read with
     * {@code size=1}.
     */
    private static PageableImpl toPageable(PaginationParam paginationParam) {
        if (!paginationParam.hasPagination()) {
            return new PageableImpl(1, Integer.MAX_VALUE);
        }
        return new PageableImpl(paginationParam.getPage(), Math.max(paginationParam.getSize(), 1));
    }
}
