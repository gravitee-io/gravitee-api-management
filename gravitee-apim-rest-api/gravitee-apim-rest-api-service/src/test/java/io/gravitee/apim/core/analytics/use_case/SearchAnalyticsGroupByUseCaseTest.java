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
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsGroupByUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsGroupByUseCase.Output;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.model.v4.analytics.GroupByResult;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAnalyticsGroupByUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant FROM = Instant.parse("2023-10-22T10:15:30Z").minus(1, ChronoUnit.DAYS);
    private static final Instant TO = Instant.parse("2023-10-22T10:15:30Z");

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchAnalyticsGroupByUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchAnalyticsGroupByUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_field_is_null() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, null, null, FROM, TO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid field");
    }

    @Test
    void should_throw_if_field_is_not_allowed() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "invalid-field", null, FROM, TO))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid field");
    }

    @Test
    void should_throw_if_api_not_found() {
        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "status", null, FROM, TO)))
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "status", null, FROM, TO)))
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "status", null, FROM, TO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_return_empty_map_when_no_data() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByResult = null;
        final Output result = cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "status", null, FROM, TO));
        assertThat(result.groupBy().getValues()).isEmpty();
    }

    @Test
    void should_return_group_by_values() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByResult = GroupByResult.builder().values(Map.of("200", 150L, "404", 10L, "500", 3L)).build();
        final Output result = cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "status", 10, FROM, TO));
        assertThat(result.groupBy().getValues()).containsEntry("200", 150L).containsEntry("404", 10L).containsEntry("500", 3L);
    }

    @Test
    void should_use_default_size_when_null() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByResult = GroupByResult.builder().values(Map.of("200", 50L)).build();
        final Output result = cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "status", null, FROM, TO));
        assertThat(result.groupBy().getValues()).containsEntry("200", 50L);
    }

    @Test
    void should_accept_application_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByResult = GroupByResult.builder().values(Map.of("app-1", 100L)).build();
        final Output result = cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "application", 5, FROM, TO));
        assertThat(result.groupBy().getValues()).containsEntry("app-1", 100L);
    }
}
