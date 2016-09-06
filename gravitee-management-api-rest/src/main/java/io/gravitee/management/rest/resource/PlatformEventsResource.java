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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.EventEntity;
import io.gravitee.management.rest.resource.param.EventSearchParam;
import io.gravitee.management.service.EventService;
import io.gravitee.repository.management.model.Event;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Gateway"})
public class PlatformEventsResource {
    
    @Inject
    private EventService eventService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Page<EventEntity> listEvents(@BeanParam EventSearchParam eventSearchParam) {
        eventSearchParam.validate();

        Map<String, Object> properties = new HashMap<>();
        if (eventSearchParam.getApiIdsParam() != null &&
                eventSearchParam.getApiIdsParam().getIds() != null &&
                !eventSearchParam.getApiIdsParam().getIds().isEmpty()) {
            properties.put(Event.EventProperties.API_ID.getValue(), eventSearchParam.getApiIdsParam().getIds());
        }

        return eventService.search(
                eventSearchParam.getEventTypeListParam().getEventTypes(),
                properties,
                eventSearchParam.getFrom(),
                eventSearchParam.getTo(),
                eventSearchParam.getPage(),
                eventSearchParam.getSize());
    }
}
