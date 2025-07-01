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

import io.gravitee.rest.api.management.v2.rest.model.AnalyticsType;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class ApiAnalyticsParam {

    @QueryParam("from")
    private Long from;

    @QueryParam("to")
    private Long to;

    @QueryParam("interval")
    private Long interval;

    @QueryParam("field")
    private String field;

    @QueryParam("size")
    private Integer size;

    @QueryParam("type")
    private AnalyticsType type;

    @QueryParam("ranges")
    private Ranges ranges;

    @QueryParam("aggregations")
    private Aggregations aggregations;

    @QueryParam("order")
    private String order;

    @Getter
    @Setter
    public static class Ranges extends AbstractListParam<Range> {

        public Ranges() {
            super(null);
        }

        public Ranges(String rangesStr) {
            super(rangesStr);
        }

        public static Ranges fromString(String rangesStr) {
            return new Ranges(rangesStr);
        }

        @Override
        protected Range parseValue(String param) {
            // param is expected to be "from:to"
            String[] bounds = param.split(":");
            if (bounds.length == 2) {
                try {
                    int from = Integer.parseInt(bounds[0]);
                    int to = Integer.parseInt(bounds[1]);
                    return new Range(from, to);
                } catch (NumberFormatException ignored) {
                    log.debug("NumberFormatException ignored in ApiAnalyticsParam");
                }
            }
            return null;
        }

        public List<Range> getRanges() {
            return this;
        }
    }

    @Getter
    @Setter
    public static class Range {

        private int from;
        private int to;

        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    @Getter
    @Setter
    public static class Aggregations extends AbstractListParam<Aggregation> {

        public Aggregations() {
            super(null);
        }

        public Aggregations(String aggregationsStr) {
            super(aggregationsStr);
        }

        public static Aggregations fromString(String aggregationsStr) {
            return new Aggregations(aggregationsStr);
        }

        @Override
        protected Aggregation parseValue(String param) {
            // param is expected to be "type:field"
            String[] parts = param.split(":");
            if (parts.length == 2) {
                return new Aggregation(parts[0], parts[1]);
            }
            return null;
        }

        public List<Aggregation> getAggregations() {
            return this;
        }
    }

    @Getter
    @Setter
    public static class Aggregation {

        private String type;
        private String field;

        public Aggregation(String type, String field) {
            this.type = type;
            this.field = field;
        }
    }

    public void setRanges(String rangesStr) {
        this.ranges = Ranges.fromString(rangesStr);
    }

    public void setAggregations(String aggregationsStr) {
        this.aggregations = Aggregations.fromString(aggregationsStr);
    }
}
