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
package io.gravitee.apim.core.analytics.domain_service;

import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import lombok.RequiredArgsConstructor;

public interface ApiAnalyticsSpecification {
    boolean satisfies(Api api, ExecutionContext executionContext, long from, long to);

    void throwIfNotSatisfied(Api api, ExecutionContext executionContext, long from, long to);

    @RequiredArgsConstructor
    class And implements ApiAnalyticsSpecification {

        private final List<ApiAnalyticsSpecification> specifications;

        @Override
        public boolean satisfies(Api api, ExecutionContext executionContext, long from, long to) {
            return specifications.stream().allMatch(spec -> spec.satisfies(api, executionContext, from, to));
        }

        @Override
        public void throwIfNotSatisfied(Api api, ExecutionContext executionContext, long from, long to) {
            specifications.forEach(spec -> spec.throwIfNotSatisfied(api, executionContext, from, to));
        }
    }

    class ApiV4Specification implements ApiAnalyticsSpecification {

        @Override
        public boolean satisfies(Api api, ExecutionContext executionContext, long from, long to) {
            return api.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V4;
        }

        @Override
        public void throwIfNotSatisfied(Api api, ExecutionContext executionContext, long from, long to) {
            if (!satisfies(api, executionContext, from, to)) {
                throw new ApiInvalidDefinitionVersionException(api.getId());
            }
        }
    }

    class ApiProxySpecification implements ApiAnalyticsSpecification {

        @Override
        public boolean satisfies(Api api, ExecutionContext executionContext, long from, long to) {
            return !api.isTcpProxy();
        }

        @Override
        public void throwIfNotSatisfied(Api api, ExecutionContext executionContext, long from, long to) {
            if (!satisfies(api, executionContext, from, to)) {
                throw new TcpProxyNotSupportedException(api.getId());
            }
        }
    }

    class ApiMultiTenancyAccessSpecification implements ApiAnalyticsSpecification {

        @Override
        public boolean satisfies(Api api, ExecutionContext executionContext, long from, long to) {
            return api.belongsToEnvironment(executionContext.getEnvironmentId());
        }

        @Override
        public void throwIfNotSatisfied(Api api, ExecutionContext executionContext, long from, long to) {
            if (!satisfies(api, executionContext, from, to)) {
                throw new ApiNotFoundException(api.getId());
            }
        }
    }

    class TimeRangeSpecification implements ApiAnalyticsSpecification {

        @Override
        public boolean satisfies(Api api, ExecutionContext executionContext, long from, long to) {
            return from <= to;
        }

        @Override
        public void throwIfNotSatisfied(Api api, ExecutionContext executionContext, long from, long to) {
            if (!satisfies(api, executionContext, from, to)) {
                throw new IllegalTimeRangeException();
            }
        }
    }

    static ApiAnalyticsSpecification forSearchHistogramAnalytics() {
        return new And(
            List.of(
                new ApiV4Specification(),
                new ApiProxySpecification(),
                new ApiMultiTenancyAccessSpecification(),
                new TimeRangeSpecification()
            )
        );
    }

    static ApiAnalyticsSpecification forSearchGroupByAnalytics() {
        return new And(
            List.of(
                new ApiV4Specification(),
                new ApiProxySpecification(),
                new ApiMultiTenancyAccessSpecification(),
                new TimeRangeSpecification()
            )
        );
    }

    static ApiAnalyticsSpecification forRequestsCountAnalytics() {
        return new And(
            List.of(
                new ApiV4Specification(),
                new ApiProxySpecification(),
                new ApiMultiTenancyAccessSpecification(),
                new TimeRangeSpecification()
            )
        );
    }

    static ApiAnalyticsSpecification forSearchStatsAnalytics() {
        return new And(
            List.of(
                new ApiV4Specification(),
                new ApiProxySpecification(),
                new ApiMultiTenancyAccessSpecification(),
                new TimeRangeSpecification()
            )
        );
    }
}
