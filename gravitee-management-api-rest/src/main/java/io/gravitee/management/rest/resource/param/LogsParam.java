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
package io.gravitee.management.rest.resource.param;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogsParam {

    @QueryParam("from")
    @ApiParam(value = "Timestamp used to define the start date of the time window to query")
    private long from;

    @QueryParam("to")
    @ApiParam(value = "Timestamp used to define the end date of the time window to query")
    private long to;

    @ApiParam(name="query",
            value = "The expresion used to search for logs. It looks like 'transaction:123-456-789 AND uri=\\\\/path\\\\/to\\\\/resource* AND response-time:[100 TO 200]'." +
                    " Reserved characters that must be escaped + - = && || > < ! ( ) { } [ ] ^ \" ~ * ? : \\ /")
    @QueryParam("query")
    private String query;

    @QueryParam("size")
    @DefaultValue("20")
    @ApiParam(value = "The number of data to retrieve")
    private int size;

    @QueryParam("page")
    @DefaultValue("1")
    private int page;

    @QueryParam("field")
    @ApiParam(value = "The field to query when doing `group_by` queries")
    private String field;

    @QueryParam("order")
    @ApiParam(value = "true means ASC order, false means DESC")
    private boolean order;

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isOrder() {
        return order;
    }

    public void setOrder(boolean order) {
        this.order = order;
    }

    public void validate() throws WebApplicationException {
        if (from == -1) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Query parameter 'from' is not valid")
                    .build());
        }

        if (to == -1) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Query parameter 'to' is not valid")
                    .build());
        }

        if (from >= to) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'from' query parameter value must be greater than 'to'")
                    .build());
        }

        if (page < 0) {
            page = 0;
        }

        if (size < 0) {
            size = 20;
        }
    }
}
