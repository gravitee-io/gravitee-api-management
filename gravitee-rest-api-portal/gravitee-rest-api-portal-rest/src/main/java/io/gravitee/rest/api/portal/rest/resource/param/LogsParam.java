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
package io.gravitee.rest.api.portal.rest.resource.param;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.QueryParam;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
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

    @QueryParam("field")
    @ApiParam(value = "The field to query when doing `group_by` queries")
    private String field;
    
    @QueryParam("order")
    @ApiParam(value = "true means ASC order, false means DESC")
    private String order;

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

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public void validate() {
        if (from == -1) {
            throw new BadRequestException("Query parameter 'from' is not valid");
        }

        if (to == -1) {
            throw new BadRequestException("Query parameter 'to' is not valid");
        }

        if (from >= to) {
            throw new BadRequestException("'from' query parameter value must not be greater than 'to'");
        }
        
        if (!"ASC".equals(order) && !"DESC".equals(order)) {
            throw new BadRequestException("'order' query parameter value must be 'ASC' or 'DESC'");
        }
    }
}
