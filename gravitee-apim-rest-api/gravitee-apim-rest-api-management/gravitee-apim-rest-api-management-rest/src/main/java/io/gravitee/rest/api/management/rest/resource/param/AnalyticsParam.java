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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsParam {

    @QueryParam("from")
    @Parameter(description = "Timestamp used to define the start date of the time window to query")
    private long from;

    @QueryParam("to")
    @Parameter(description = "Timestamp used to define the end date of the time window to query")
    private long to;

    @QueryParam("interval")
    @Parameter(description = "The time interval when getting histogram data (in milliseconds)", example = "600000")
    private long interval;

    @QueryParam("query")
    @Parameter(
        description = "The Lucene query used to filter data",
        example = "api:xxxx-xxxx-xxxx-xxxx AND plan:yyyy-yyyy-yyyy-yyyy AND host:\"demo.gravitee.io\" AND path:/test"
    )
    private String query;

    @QueryParam("field")
    @Parameter(description = "The field to query when doing `group_by` queries")
    private String field;

    @QueryParam("size")
    @Parameter(description = "The number of data to retrieve")
    private int size;

    @QueryParam("type")
    @Parameter(description = "The type of data to retrieve", required = true)
    private AnalyticsType type;

    @QueryParam("ranges")
    @Parameter(
        description = "Ranges allows you to group field's data. Mainly used to group HTTP statuses code with `group_by` queries",
        explode = Explode.FALSE,
        schema = @Schema(type = "array"),
        example = "100:199,200:299,300:399,400:499,500:599"
    )
    private RangesParam ranges;

    @QueryParam("aggs")
    @Parameter(
        description = "Aggregations are used when doing `date_histo` queries and allows you to group field's data. Mainly used to group HTTP statuses code",
        explode = Explode.FALSE,
        schema = @Schema(type = "array"),
        example = "avg:response-time,avg:api-response-time"
    )
    private AggregationsParam aggregations;

    @JsonIgnore
    private Order order;

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public AnalyticsType getType() {
        return type;
    }

    public void setType(AnalyticsType type) {
        this.type = type;
    }

    public List<Range> getRanges() {
        return (ranges == null) ? null : ranges.getValues();
    }

    public List<Aggregation> getAggregations() {
        return (aggregations == null) ? null : aggregations.getValues();
    }

    @QueryParam("order")
    @Parameter(
        description = "The field used to sort results. Can be asc or desc (prefix with minus '-') ",
        example = "order:-response-time"
    )
    public void setOrder(String param) {
        if (param != null) {
            order = Order.parse(param);
        }
    }

    public Order getOrder() {
        return order;
    }

    public void validate() throws WebApplicationException {
        if (type == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'type' is not valid").build()
            );
        }

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

        if (interval == -1) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'interval' is not valid").build()
            );
        }

        if (interval < 1_000 || interval > 1_000_000_000) {
            throw new WebApplicationException(
                Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 1000000000")
                    .build()
            );
        }

        if (from >= to) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("'from' query parameter value must be greater than 'to'").build()
            );
        }

        if (type == AnalyticsType.GROUP_BY) {
            // we need a field and, optionally, a list of ranges
            if (field == null || field.trim().isEmpty()) {
                throw new WebApplicationException(
                    Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("'field' query parameter is required for 'group_by' request")
                        .build()
                );
            }
        }
    }
}
