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
import static org.assertj.core.api.Assertions.fail;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.use_case.SearchRequestsCountAnalyticsUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchRequestsCountAnalyticsUseCase.Output;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchRequestsCountAnalyticsUseCaseTest {

    public static final String ENV_ID = "environment-id";
    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchRequestsCountAnalyticsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchRequestsCountAnalyticsUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_no_api_does_not_belong_to_current_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() -> cut.execute(new Input(MY_API, "another-environment"))).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_no_api_found() {
        assertThatThrownBy(() -> cut.execute(new Input(MY_API, ENV_ID))).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_definition_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() -> cut.execute(new Input(MY_API, ENV_ID))).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_not_find_requests_count() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.requestsCount = null;
        final Output result = cut.execute(new Input(MY_API, ENV_ID));
        assertThat(result.requestsCount()).isEmpty();
    }

    @Test
    void should_get_requests_count_for_a_v4_api() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.requestsCount =
            RequestsCount.builder().total(56).countsByEntrypoint(Map.of("http-get", 26L, "http-post", 30L)).build();
        final Output result = cut.execute(new Input(MY_API, ENV_ID));
        assertThat(result.requestsCount())
            .hasValueSatisfying(requestsCount -> {
                assertThat(requestsCount.getTotal()).isEqualTo(56);
                assertThat(requestsCount.getCountsByEntrypoint()).isEqualTo(Map.of("http-get", 26L, "http-post", 30L));
            });
    }
}
