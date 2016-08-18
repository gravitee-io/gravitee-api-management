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

import io.gravitee.common.data.domain.Order;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AnalyticsParam {

    @QueryParam("from")
    private long from;

    @QueryParam("to")
    private long to;

    @QueryParam("interval")
    private long interval;

    @QueryParam("query")
    private String query;

    @QueryParam("key")
    private String key;

    @QueryParam("field")
    private String field;

    @QueryParam("size")
    private int size;

    @QueryParam("aggType")
    @DefaultValue("terms")
    private AnalyticsAggTypeParam aggType;

    @QueryParam("type")
    @DefaultValue("HITS")
    private AnalyticsTypeParam type;

    @QueryParam("orderField")
    private String orderField;

    @QueryParam("orderDirection")
    @DefaultValue("asc")
    private DirectionParam orderDirection;

    @QueryParam("orderMode")
    @DefaultValue("avg")
    private OrderModeParam orderMode;

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public List<String> getAggType() {
        return aggType.getAggTypes();
    }

    public void setAggType(AnalyticsAggTypeParam aggType) {
        this.aggType = aggType;
    }

    public AnalyticsTypeParam getTypeParam() {
        return type;
    }

    public void setTypeParam(AnalyticsTypeParam type) {
        this.type = type;
    }

    public String getOrderField() {
        return orderField;
    }

    public void setOrderField(String orderField) {
        this.orderField = orderField;
    }

    public DirectionParam getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(DirectionParam orderDirection) {
        this.orderDirection = orderDirection;
    }

    public OrderModeParam getOrderMode() {
        return orderMode;
    }

    public Order getOrder() {
        if (getOrderField() != null && getOrderDirection() != null && getOrderMode() != null) {
            return new Order(getOrderField(),
                    Order.Direction.valueOf(getOrderDirection().getValue().toString()),
                    Order.Mode.valueOf(getOrderMode().getValue().toString()));
        } else {
            return null;
        }
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

        if (interval == -1) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Query parameter 'interval' is not valid")
                    .build());
        }

        if (interval < 1_000 || interval > 20_000_000) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 10000000")
                    .build());
        }

        if (from >= to) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'from' query parameter value must be greater than 'to'")
                    .build());
        }
    }
}
