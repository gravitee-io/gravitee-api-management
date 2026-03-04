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

import io.gravitee.rest.api.management.v2.rest.validation.IntervalParamConstraint;
import io.gravitee.rest.api.management.v2.rest.validation.TimeInterval;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
@IntervalParamConstraint
public class SearchApiAnalyticsParam implements TimeInterval {

    public static final String TYPE_QUERY_PARAM_NAME = "type";
    public static final String FROM_QUERY_PARAM_NAME = "from";
    public static final String TO_QUERY_PARAM_NAME = "to";
    public static final String FIELD_QUERY_PARAM_NAME = "field";
    public static final String INTERVAL_QUERY_PARAM_NAME = "interval";
    public static final String SIZE_QUERY_PARAM_NAME = "size";
    public static final String ORDER_QUERY_PARAM_NAME = "order";

    @NotNull
    @QueryParam(TYPE_QUERY_PARAM_NAME)
    private Type type;

    @Min(0)
    @NotNull
    @QueryParam(FROM_QUERY_PARAM_NAME)
    private Long from;

    @Min(0)
    @NotNull
    @QueryParam(TO_QUERY_PARAM_NAME)
    private Long to;

    @QueryParam(FIELD_QUERY_PARAM_NAME)
    private String field;

    @Min(1)
    @QueryParam(INTERVAL_QUERY_PARAM_NAME)
    private Long interval;

    @Min(1)
    @QueryParam(SIZE_QUERY_PARAM_NAME)
    private Integer size;

    @QueryParam(ORDER_QUERY_PARAM_NAME)
    private Order order;

    public enum Type {
        COUNT,
        STATS,
        GROUP_BY,
        DATE_HISTO,
    }

    public enum Order {
        ASC,
        DESC,
    }
}
