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
package io.gravitee.rest.api.management.v2.rest.resource.param;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class AnalyticsParam {

    @QueryParam("type")
    @Parameter(description = "The type of analytics query", required = true)
    private AnalyticsType type;

    @QueryParam("from")
    @Parameter(description = "Timestamp used to define the start date of the time window to query (epoch millis)")
    private Long from;

    @QueryParam("to")
    @Parameter(description = "Timestamp used to define the end date of the time window to query (epoch millis)")
    private Long to;

    @QueryParam("field")
    @Parameter(description = "The field to aggregate on (required for STATS and GROUP_BY)")
    private String field;

    @QueryParam("interval")
    @Parameter(description = "The time interval for date histogram data (in milliseconds)", example = "600000")
    private Long interval;

    @QueryParam("size")
    @DefaultValue("10")
    @Parameter(description = "The number of buckets to retrieve for GROUP_BY queries")
    private int size;

    public AnalyticsType getType() {
        return type;
    }

    public void setType(AnalyticsType type) {
        this.type = type;
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

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void validate() throws WebApplicationException {
        if (type == null) {
            throw badRequest("Query parameter 'type' is required");
        }

        if (from == null) {
            throw badRequest("Query parameter 'from' is required");
        }

        if (to == null) {
            throw badRequest("Query parameter 'to' is required");
        }

        if (from >= to) {
            throw badRequest("'from' query parameter value must be less than 'to'");
        }

        if (field != null && !AnalyticsFieldParam.isSupported(field)) {
            throw badRequest("Query parameter 'field' value '" + field + "' is not supported");
        }

        if (
            (type == AnalyticsType.STATS || type == AnalyticsType.GROUP_BY || type == AnalyticsType.DATE_HISTO) &&
            (field == null || field.trim().isEmpty())
        ) {
            throw badRequest("'field' query parameter is required for '" + type + "' request");
        }

        if (type == AnalyticsType.DATE_HISTO) {
            if (interval == null) {
                throw badRequest("'interval' query parameter is required for 'DATE_HISTO' request");
            }
            if (interval < 1_000 || interval > 1_000_000_000) {
                throw badRequest("Query parameter 'interval' must be >= 1000 and <= 1000000000");
            }
        }
    }

    private static WebApplicationException badRequest(String message) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(message).build());
    }
}
