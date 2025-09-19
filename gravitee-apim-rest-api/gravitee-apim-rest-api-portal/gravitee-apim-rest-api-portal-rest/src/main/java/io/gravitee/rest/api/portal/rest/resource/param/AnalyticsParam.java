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
package io.gravitee.rest.api.portal.rest.resource.param;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import java.util.List;

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
    @Parameter(
        description = "The type of data to retrieve",
        required = true,
        schema = @Schema(allowableValues = { "group_by", "date_histo", "count" })
    )
    private AnalyticsTypeParam type;

    @QueryParam("ranges")
    @Parameter(
        description = "Ranges allows you to group field's data. Mainly used to group HTTP statuses code with `group_by` queries",
        example = "100:199;200:299;300:399;400:499;500:599"
    )
    private RangesParam ranges;

    @QueryParam("aggs")
    @Parameter(
        description = "Aggregations are used when doing `date_histo` queries and allows you to group field's data. Mainly used to group HTTP statuses code",
        example = "field:status or avg:response-time;avg:api-response-time"
    )
    private AggregationsParam aggs;

    @QueryParam("order")
    @Parameter(
        description = "The field used to sort results. Can be asc or desc (prefix with minus '-') ",
        example = "order:-response-time"
    )
    private OrderParam order;

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

    public AnalyticsTypeParam getTypeParam() {
        return type;
    }

    public void setTypeParam(AnalyticsTypeParam type) {
        this.type = type;
    }

    public AnalyticsTypeParam.AnalyticsType getType() {
        return type.getValue();
    }

    public List<Range> getRanges() {
        return (ranges == null) ? null : ranges.getValue();
    }

    public List<Aggregation> getAggregations() {
        return (aggs == null) ? null : aggs.getValue();
    }

    public OrderParam.Order getOrder() {
        return (order == null) ? null : order.getValue();
    }

    public void validate() {
        if (type == null) {
            throw new BadRequestException("Query parameter 'type' must be present and one of : GROUP_BY, DATE_HISTO, COUNT");
        }

        if (type.getValue() == null) {
            throw new BadRequestException("Query parameter 'type' is not valid");
        }

        if (from == -1) {
            throw new BadRequestException("Query parameter 'from' is not valid");
        }

        if (to == -1) {
            throw new BadRequestException("Query parameter 'to' is not valid");
        }

        if (from >= to) {
            throw new BadRequestException("'from' query parameter value must not be greater than 'to'");
        }

        if (interval == -1) {
            throw new BadRequestException("Query parameter 'interval' is not valid");
        }

        if (type.getValue() == AnalyticsTypeParam.AnalyticsType.DATE_HISTO && (interval < 1_000 || interval > 1_000_000_000)) {
            throw new BadRequestException("Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 1000000000");
        }

        if (
            (type.getValue() == AnalyticsTypeParam.AnalyticsType.GROUP_BY || type.getValue() == AnalyticsTypeParam.AnalyticsType.STATS) &&
            (field == null || field.trim().isEmpty())
        ) {
            // we need a field and, optionally, a list of ranges
            throw new BadRequestException("'field' query parameter is required for '" + type.getValue().name().toLowerCase() + "' request");
        }
    }
}
