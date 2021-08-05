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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.TicketEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.mapper.TicketMapper;
import io.gravitee.rest.api.portal.rest.model.TicketInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.resource.param.TicketsParam;
import io.gravitee.rest.api.service.TicketService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketsResource extends AbstractResource {

    @Inject
    private TicketService ticketService;

    @Inject
    private TicketMapper ticketMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid @NotNull(message = "Input must not be null.") final TicketInput ticketInput) {
        ticketService.create(
            getAuthenticatedUser(),
            ticketMapper.convert(ticketInput),
            GraviteeContext.getCurrentEnvironment(),
            ParameterReferenceType.ENVIRONMENT
        );
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTickets(@Valid @BeanParam PaginationParam paginationParam, @BeanParam TicketsParam ticketsParam) {
        TicketQuery query = new TicketQuery();
        query.setApi(ticketsParam.getApi());
        query.setApplication(ticketsParam.getApplication());
        query.setApi(ticketsParam.getApi());
        query.setFromUser(getAuthenticatedUser());

        Sortable sortable = null;
        if (ticketsParam.getOrder() != null) {
            sortable = new SortableImpl(ticketsParam.getOrder().getField(), ticketsParam.getOrder().isSorted());
        }

        Page<TicketEntity> tickets = ticketService.search(
            query,
            sortable,
            new PageableImpl(paginationParam.getPage(), paginationParam.getSize())
        );

        final Map<String, Object> metadataTotal = new HashMap<>();
        metadataTotal.put(METADATA_DATA_TOTAL_KEY, tickets.getTotalElements());

        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        metadata.put(METADATA_DATA_KEY, metadataTotal);

        return createListResponse(tickets.getContent(), paginationParam, metadata, false);
    }
}
