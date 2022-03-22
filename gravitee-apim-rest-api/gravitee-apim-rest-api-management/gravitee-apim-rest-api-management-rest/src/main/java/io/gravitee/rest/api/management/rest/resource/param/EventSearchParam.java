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
package io.gravitee.rest.api.management.rest.resource.param;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventSearchParam {

    @QueryParam("type")
    @Parameter(explode = Explode.FALSE, schema = @Schema(type = "array"))
    private EventTypeListParam eventTypeListParam;

    @QueryParam("from")
    private Long from;

    @QueryParam("to")
    private Long to;

    @QueryParam("page")
    @DefaultValue("0")
    private Integer page;

    @QueryParam("size")
    @DefaultValue("10")
    private Integer size;

    @QueryParam("api_ids")
    @Parameter(explode = Explode.FALSE, schema = @Schema(type = "array"))
    private ListStringParam apiIdsParam;

    public EventTypeListParam getEventTypeListParam() {
        return eventTypeListParam;
    }

    public void setEventTypeListParam(EventTypeListParam eventTypeListParam) {
        this.eventTypeListParam = eventTypeListParam;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getTo() {
        return to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public ListStringParam getApiIdsParam() {
        return apiIdsParam;
    }

    public void setApiIdsParam(ListStringParam apiIdsParam) {
        this.apiIdsParam = apiIdsParam;
    }

    public void validate() throws WebApplicationException {
        if (from == -1) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'from' is not valid").build()
            );
        }

        if (to == -1) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'to' is not valid").build()
            );
        }

        if (page == -1) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'page' is not valid").build()
            );
        }

        if (size < 0 || size > 100) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'size' must be >= 0 and <= 100").build()
            );
        }

        if (from >= to) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("'from' query parameter value must be greater than 'to'").build()
            );
        }
    }
}
