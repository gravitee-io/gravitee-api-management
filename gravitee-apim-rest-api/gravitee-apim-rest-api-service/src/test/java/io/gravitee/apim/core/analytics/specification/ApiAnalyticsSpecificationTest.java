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
package io.gravitee.apim.core.analytics.specification;

import static org.junit.jupiter.api.Assertions.*;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.analytics.domain_service.ApiAnalyticsSpecification;
import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiAnalyticsSpecificationTest {

    @Nested
    class ApiV4SpecificationTest {

        ApiAnalyticsSpecification spec = new ApiAnalyticsSpecification.ApiV4Specification();
        Api apiV4 = ApiFixtures.aProxyApiV4();
        Api apiV2 = ApiFixtures.aProxyApiV2();
        ExecutionContext ctx = null;

        @Test
        void shouldSatisfy() {
            assertTrue(spec.satisfies(apiV4, ctx, 0, 0));
        }

        @Test
        void shouldNotSatisfy() {
            assertFalse(spec.satisfies(apiV2, ctx, 0, 0));
        }

        @Test
        void shouldThrow() {
            assertThrows(ApiInvalidDefinitionVersionException.class, () -> spec.throwIfNotSatisfied(apiV2, ctx, 0, 0));
        }

        @Test
        void shouldNotThrow() {
            assertDoesNotThrow(() -> spec.throwIfNotSatisfied(apiV4, ctx, 0, 0));
        }
    }

    @Nested
    class ApiProxySpecificationTest {

        ApiAnalyticsSpecification spec = new ApiAnalyticsSpecification.ApiProxySpecification();
        Api apiProxy = ApiFixtures.aProxyApiV4();
        Api apiTcpProxy = ApiFixtures.aTcpApiV4();
        ExecutionContext ctx = null;

        @Test
        void shouldSatisfy() {
            assertTrue(spec.satisfies(apiProxy, ctx, 0, 0));
        }

        @Test
        void shouldNotSatisfy() {
            assertFalse(spec.satisfies(apiTcpProxy, ctx, 0, 0));
        }

        @Test
        void shouldThrow() {
            assertThrows(TcpProxyNotSupportedException.class, () -> spec.throwIfNotSatisfied(apiTcpProxy, ctx, 0, 0));
        }

        @Test
        void shouldNotThrow() {
            assertDoesNotThrow(() -> spec.throwIfNotSatisfied(apiProxy, ctx, 0, 0));
        }
    }

    @Nested
    class ApiMultiTenancyAccessSpecificationTest {

        ApiAnalyticsSpecification spec = new ApiAnalyticsSpecification.ApiMultiTenancyAccessSpecification();
        Api apiEnv = ApiFixtures.aProxyApiV4();
        Api apiOtherEnv = ApiFixtures.aProxyApiV4().toBuilder().environmentId("other-env").build();
        ExecutionContext ctx = new ExecutionContext("organization-id", "environment-id");

        @Test
        void shouldSatisfy() {
            assertTrue(spec.satisfies(apiEnv, ctx, 0, 0));
        }

        @Test
        void shouldNotSatisfy() {
            assertFalse(spec.satisfies(apiOtherEnv, ctx, 0, 0));
        }

        @Test
        void shouldThrow() {
            assertThrows(ApiNotFoundException.class, () -> spec.throwIfNotSatisfied(apiOtherEnv, ctx, 0, 0));
        }

        @Test
        void shouldNotThrow() {
            assertDoesNotThrow(() -> spec.throwIfNotSatisfied(apiEnv, ctx, 0, 0));
        }
    }

    @Nested
    class TimeRangeSpecificationTest {

        ApiAnalyticsSpecification spec = new ApiAnalyticsSpecification.TimeRangeSpecification();
        Api api = ApiFixtures.aProxyApiV4();
        ExecutionContext ctx = null;

        @Test
        void shouldSatisfy() {
            assertTrue(spec.satisfies(api, ctx, 1L, 2L));
            assertTrue(spec.satisfies(api, ctx, 2L, 2L));
        }

        @Test
        void shouldNotSatisfy() {
            assertFalse(spec.satisfies(api, ctx, 3L, 2L));
        }

        @Test
        void shouldThrow() {
            assertThrows(IllegalTimeRangeException.class, () -> spec.throwIfNotSatisfied(api, ctx, 3L, 2L));
        }

        @Test
        void shouldNotThrow() {
            assertDoesNotThrow(() -> spec.throwIfNotSatisfied(api, ctx, 1L, 2L));
        }
    }
}
