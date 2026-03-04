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
package io.gravitee.apim.core.analytics.use_case;

import static fixtures.core.model.ApiFixtures.MY_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.AnalyticsQueryType;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.AnalyticsResult;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Input;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant FROM = Instant.parse("2023-10-21T10:15:30Z");
    private static final Instant TO = Instant.parse("2023-10-22T10:15:30Z");

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchApiAnalyticsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchApiAnalyticsUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
        analyticsQueryService.reset();
    }

    @Nested
    class ApiValidation {

        @Test
        void should_throw_if_api_not_found() {
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput()))
                .isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_if_api_not_v4() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput()))
                .isInstanceOf(ApiInvalidDefinitionVersionException.class);
        }

        @Test
        void should_throw_if_api_is_tcp() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Analytics are not supported for TCP Proxy APIs");
        }

        @Test
        void should_throw_if_api_does_not_belong_to_environment() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
            var input = new Input(MY_API, "another-env", AnalyticsQueryType.COUNT, FROM, TO, null, null, 10);
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), input)).isInstanceOf(ApiNotFoundException.class);
        }
    }

    @Nested
    class CountQuery {

        @BeforeEach
        void setUp() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        }

        @Test
        void should_return_count() {
            analyticsQueryService.countResult = 12345L;

            var output = cut.execute(GraviteeContext.getExecutionContext(), countInput());

            assertThat(output.result()).isInstanceOf(AnalyticsResult.CountResult.class);
            assertThat(((AnalyticsResult.CountResult) output.result()).count()).isEqualTo(12345L);
        }

        @Test
        void should_return_zero_when_no_data() {
            analyticsQueryService.countResult = null;

            var output = cut.execute(GraviteeContext.getExecutionContext(), countInput());

            assertThat(output.result()).isInstanceOf(AnalyticsResult.CountResult.class);
            assertThat(((AnalyticsResult.CountResult) output.result()).count()).isZero();
        }
    }

    private static Input countInput() {
        return new Input(MY_API, ENV_ID, AnalyticsQueryType.COUNT, FROM, TO, null, null, 10);
    }
}
