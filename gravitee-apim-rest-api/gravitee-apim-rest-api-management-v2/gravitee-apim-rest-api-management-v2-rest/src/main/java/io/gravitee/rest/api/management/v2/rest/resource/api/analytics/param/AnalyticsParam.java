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
package io.gravitee.rest.api.management.v2.rest.resource.api.analytics.param;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class AnalyticsParam {

    @QueryParam("type")
    private AnalyticsType type;

    @QueryParam("from")
    private Long from;

    @QueryParam("to")
    private Long to;

    @QueryParam("field")
    private String field;

    @QueryParam("interval")
    private Long interval;

    @QueryParam("query")
    private String query;

    @QueryParam("size")
    private int size = 10;

    @QueryParam("order")
    private String order;

    public AnalyticsType getType() {
        return type;
    }

    public Long getFrom() {
        return from;
    }

    public Long getTo() {
        return to;
    }

    public String getField() {
        return field;
    }

    public Long getInterval() {
        return interval;
    }

    public String getQuery() {
        return query;
    }

    public int getSize() {
        return size;
    }

    public String getOrder() {
        return order;
    }

    public void validate() {
        if (type == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'type' is required").build()
            );
        }

        if (from == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'from' is required").build()
            );
        }

        if (to == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'to' is required").build()
            );
        }

        if (from >= to) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("'from' query parameter value must be lesser than 'to'").build()
            );
        }

        if (type == AnalyticsType.STATS || type == AnalyticsType.GROUP_BY) {
            if (field == null || field.isBlank()) {
                throw new WebApplicationException(
                    Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("'field' query parameter is required for '" + type.name().toLowerCase() + "' requests")
                        .build()
                );
            }
        }

        if (type == AnalyticsType.DATE_HISTO) {
            if (field == null || field.isBlank()) {
                throw new WebApplicationException(
                    Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("'field' query parameter is required for 'date_histo' requests")
                        .build()
                );
            }
            if (interval == null) {
                throw new WebApplicationException(
                    Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("'interval' query parameter is required for 'date_histo' requests")
                        .build()
                );
            }
            if (interval < 1_000 || interval > 1_000_000_000) {
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity("'interval' must be >= 1000 and <= 1000000000").build()
                );
            }
        }
    }
}
