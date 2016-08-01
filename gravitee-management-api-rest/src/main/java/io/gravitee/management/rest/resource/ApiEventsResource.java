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
package io.gravitee.management.rest.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.*;

import io.gravitee.common.http.MediaType;

import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.param.EventTypeListParam;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.EventService;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author GraviteeSource Team
 */
@ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
public class ApiEventsResource extends AbstractResource {

    @PathParam("api")
    private String api;

    @Inject
    private EventService eventService;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventEntity> events(@DefaultValue("all") @QueryParam("type") EventTypeListParam eventTypeListParam) {
        return eventService.findByApi(api).stream()
                .filter(event -> eventTypeListParam.getEventTypes().contains(event.getType()))
                .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
                .collect(Collectors.toList());
    }

}
