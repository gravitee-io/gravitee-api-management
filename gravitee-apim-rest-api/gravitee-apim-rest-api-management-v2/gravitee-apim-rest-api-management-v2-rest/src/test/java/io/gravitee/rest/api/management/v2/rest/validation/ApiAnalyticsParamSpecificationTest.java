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
package io.gravitee.rest.api.management.v2.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.rest.api.management.v2.rest.resource.param.ApiAnalyticsParam;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApiAnalyticsParamSpecificationTest {

    @Test
    void testHasFromParam() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setFrom(123L);
        assertTrue(ApiAnalyticsParamSpecification.hasFromParam().satisfies(param));
        param.setFrom(null);
        assertFalse(ApiAnalyticsParamSpecification.hasFromParam().satisfies(param));
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.hasFromParam().throwIfNotSatisfied(param));
    }

    @Test
    void testHasToParam() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setTo(456L);
        assertTrue(ApiAnalyticsParamSpecification.hasToParam().satisfies(param));
        param.setTo(null);
        assertFalse(ApiAnalyticsParamSpecification.hasToParam().satisfies(param));
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.hasToParam().throwIfNotSatisfied(param));
    }

    @Test
    void testHasField() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setField("field1");
        assertTrue(ApiAnalyticsParamSpecification.hasField().satisfies(param));
        param.setField("");
        assertFalse(ApiAnalyticsParamSpecification.hasField().satisfies(param));
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.hasField().throwIfNotSatisfied(param));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            // General Properties
            "gateway",
            "@timestamp",
            "transaction-id",
            "api-id",
            "api-name",
            "request-id",
            "plan-id",
            "application-id",
            "subscription-id",
            "client-identifier",
            "tenant",
            "zone",
            // HTTP Properties
            "http-method",
            "local-address",
            "remote-address",
            "host",
            "uri",
            "path",
            "mapped-path",
            // User-Agent Properties
            "user-agent",
            "user_agent.device.name",
            "user_agent.name",
            "user_agent.original",
            "user_agent.os.full",
            "user_agent.os.name",
            "user_agent.os.version",
            "user_agent.os_name",
            "user_agent.version",
            // Metrics Properties
            "request-content-length",
            "request-ended",
            "endpoint-response-time-ms",
            "status",
            "response-content-length",
            "gateway-latency-ms",
            "gateway-response-time-ms",
            // GeoIP Properties
            "geoip.continent_name",
            "geoip.country_iso_code",
            "geoip.region_name",
            "geoip.city_name",
            "geoip.location",
            // Security Properties
            "security-type",
            "security-token",
            // Error Properties
            "error-message",
            "error-key",
        }
    )
    void testHasField_parametrized(String field) {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setField(field);
        assertTrue(ApiAnalyticsParamSpecification.hasField().satisfies(param), "Should satisfy for field: " + field);
        assertDoesNotThrow(
            () -> ApiAnalyticsParamSpecification.hasField().throwIfNotSatisfied(param),
            "Should not throw for field: " + field
        );
    }

    @Test
    void testHasType() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setType("histogram");
        assertTrue(ApiAnalyticsParamSpecification.hasType().satisfies(param));
        param.setType(null);
        assertFalse(ApiAnalyticsParamSpecification.hasType().satisfies(param));
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.hasType().throwIfNotSatisfied(param));
    }

    @Test
    void testHasInterval() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setInterval(10L);
        assertTrue(ApiAnalyticsParamSpecification.hasInterval().satisfies(param));
        param.setInterval(0L);
        assertFalse(ApiAnalyticsParamSpecification.hasInterval().satisfies(param));
        param.setInterval(-1L);
        assertFalse(ApiAnalyticsParamSpecification.hasInterval().satisfies(param));
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.hasInterval().throwIfNotSatisfied(param));
    }

    @Test
    void testAggregationsNotBlank() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setAggregations("avg:field1,count:field2");
        assertTrue(ApiAnalyticsParamSpecification.aggregationsNotBlank().satisfies(param));
        param.setAggregations(":field1");
        assertFalse(ApiAnalyticsParamSpecification.aggregationsNotBlank().satisfies(param));
    }

    @Test
    void testAggregationsOfValidType() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setAggregations("avg:field1,max:field2");
        assertTrue(ApiAnalyticsParamSpecification.aggregationsOfValidType().satisfies(param));
        param.setAggregations("foo:field1");
        assertFalse(ApiAnalyticsParamSpecification.aggregationsOfValidType().satisfies(param));
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.aggregationsOfValidType().throwIfNotSatisfied(param));
    }

    @Test
    void testValidOrder() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setOrder("avg:field1");
        assertTrue(ApiAnalyticsParamSpecification.validOrder().satisfies(param));
        param.setOrder("-count:_key");
        assertTrue(ApiAnalyticsParamSpecification.validOrder().satisfies(param));
        param.setOrder("foo:bar");
        assertFalse(ApiAnalyticsParamSpecification.validOrder().satisfies(param));
        param.setOrder("-sum:field1");
        assertFalse(ApiAnalyticsParamSpecification.validOrder().satisfies(param));
        param.setOrder("count:");
        assertFalse(ApiAnalyticsParamSpecification.validOrder().satisfies(param));
        param.setOrder(":field1");
        assertFalse(ApiAnalyticsParamSpecification.validOrder().satisfies(param));
        param.setOrder("asc");
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.validOrder().throwIfNotSatisfied(param));
        param.setOrder("foo:bar");
        assertThrows(BadRequestException.class, () -> ApiAnalyticsParamSpecification.validOrder().throwIfNotSatisfied(param));
    }

    @Test
    void testAndSpecification() {
        ApiAnalyticsParam param = new ApiAnalyticsParam();
        param.setFrom(1L);
        param.setTo(2L);
        var andSpec = ApiAnalyticsParamSpecification.and(
            List.of(ApiAnalyticsParamSpecification.hasFromParam(), ApiAnalyticsParamSpecification.hasToParam())
        );
        assertTrue(andSpec.satisfies(param));
        param.setFrom(null);
        assertFalse(andSpec.satisfies(param));
        assertThrows(BadRequestException.class, () -> andSpec.throwIfNotSatisfied(param));
    }
}
