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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsParamTest {

    @Nested
    class CountValidation {

        @Test
        void should_pass_for_valid_count_params() {
            var param = buildCountParam(1000L, 2000L);
            assertThatCode(param::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    class StatsValidation {

        @Test
        void should_pass_for_valid_stats_params() {
            var param = buildParam(AnalyticsType.STATS, 1000L, 2000L);
            param.setField("gateway-response-time-ms");
            assertThatCode(param::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    class GroupByValidation {

        @Test
        void should_pass_for_valid_group_by_params() {
            var param = buildParam(AnalyticsType.GROUP_BY, 1000L, 2000L);
            param.setField("status");
            assertThatCode(param::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    class DateHistoValidation {

        @Test
        void should_pass_for_valid_date_histo_params() {
            var param = buildParam(AnalyticsType.DATE_HISTO, 1000L, 2000L);
            param.setField("status");
            param.setInterval(60_000L);
            assertThatCode(param::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    class TypeValidation {

        @Test
        void should_reject_missing_type() {
            var param = buildParam(null, 1000L, 2000L);
            assertBadRequest(param, "Query parameter 'type' is required");
        }
    }

    @Nested
    class FromToValidation {

        @Test
        void should_reject_missing_from() {
            var param = buildParam(AnalyticsType.COUNT, null, 2000L);
            assertBadRequest(param, "Query parameter 'from' is required");
        }

        @Test
        void should_reject_missing_to() {
            var param = buildParam(AnalyticsType.COUNT, 1000L, null);
            assertBadRequest(param, "Query parameter 'to' is required");
        }

        @Test
        void should_reject_from_equal_to_to() {
            var param = buildCountParam(1000L, 1000L);
            assertBadRequest(param, "'from' query parameter value must be less than 'to'");
        }

        @Test
        void should_reject_from_greater_than_to() {
            var param = buildCountParam(2000L, 1000L);
            assertBadRequest(param, "'from' query parameter value must be less than 'to'");
        }
    }

    @Nested
    class FieldValidation {

        @Test
        void should_reject_unsupported_field() {
            var param = buildParam(AnalyticsType.STATS, 1000L, 2000L);
            param.setField("not-a-real-field");
            assertBadRequest(param, "Query parameter 'field' value 'not-a-real-field' is not supported");
        }

        @Test
        void should_reject_missing_field_for_stats() {
            var param = buildParam(AnalyticsType.STATS, 1000L, 2000L);
            assertBadRequest(param, "'field' query parameter is required for 'STATS' request");
        }

        @Test
        void should_reject_missing_field_for_group_by() {
            var param = buildParam(AnalyticsType.GROUP_BY, 1000L, 2000L);
            assertBadRequest(param, "'field' query parameter is required for 'GROUP_BY' request");
        }

        @Test
        void should_reject_blank_field_for_stats() {
            var param = buildParam(AnalyticsType.STATS, 1000L, 2000L);
            param.setField("   ");
            assertBadRequest(param, "'field' query parameter is required for 'STATS' request");
        }
    }

    @Nested
    class IntervalValidation {

        @Test
        void should_reject_missing_interval_for_date_histo() {
            var param = buildParam(AnalyticsType.DATE_HISTO, 1000L, 2000L);
            param.setField("status");
            assertBadRequest(param, "'interval' query parameter is required for 'DATE_HISTO' request");
        }

        @Test
        void should_reject_interval_below_minimum() {
            var param = buildParam(AnalyticsType.DATE_HISTO, 1000L, 2000L);
            param.setField("status");
            param.setInterval(999L);
            assertBadRequest(param, "Query parameter 'interval' must be >= 1000 and <= 1000000000");
        }

        @Test
        void should_reject_interval_above_maximum() {
            var param = buildParam(AnalyticsType.DATE_HISTO, 1000L, 2000L);
            param.setField("status");
            param.setInterval(1_000_000_001L);
            assertBadRequest(param, "Query parameter 'interval' must be >= 1000 and <= 1000000000");
        }

        @Test
        void should_accept_interval_at_minimum_boundary() {
            var param = buildParam(AnalyticsType.DATE_HISTO, 1000L, 2000L);
            param.setField("status");
            param.setInterval(1000L);
            assertThatCode(param::validate).doesNotThrowAnyException();
        }

        @Test
        void should_accept_interval_at_maximum_boundary() {
            var param = buildParam(AnalyticsType.DATE_HISTO, 1000L, 2000L);
            param.setField("status");
            param.setInterval(1_000_000_000L);
            assertThatCode(param::validate).doesNotThrowAnyException();
        }

        @Test
        void should_not_require_interval_for_count() {
            var param = buildCountParam(1000L, 2000L);
            assertThatCode(param::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    class FieldParamValues {

        @Test
        void should_accept_all_supported_fields_for_stats() {
            String[] supportedFields = {
                "status",
                "mapped-status",
                "application",
                "plan",
                "host",
                "uri",
                "gateway-latency-ms",
                "gateway-response-time-ms",
                "endpoint-response-time-ms",
                "request-content-length",
            };
            for (String field : supportedFields) {
                var param = buildParam(AnalyticsType.STATS, 1000L, 2000L);
                param.setField(field);
                assertThatCode(param::validate).as("field '%s' should be accepted", field).doesNotThrowAnyException();
            }
        }
    }

    private static AnalyticsParam buildCountParam(Long from, Long to) {
        return buildParam(AnalyticsType.COUNT, from, to);
    }

    private static AnalyticsParam buildParam(AnalyticsType type, Long from, Long to) {
        var param = new AnalyticsParam();
        param.setType(type);
        param.setFrom(from);
        param.setTo(to);
        return param;
    }

    private static void assertBadRequest(AnalyticsParam param, String expectedMessage) {
        assertThatThrownBy(param::validate)
            .isInstanceOf(WebApplicationException.class)
            .satisfies(ex -> {
                var wae = (WebApplicationException) ex;
                assertThat(wae.getResponse().getStatus()).isEqualTo(400);
                assertThat(wae.getResponse().getEntity()).isEqualTo(expectedMessage);
            });
    }
}
