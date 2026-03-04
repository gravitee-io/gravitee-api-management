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

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Output;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Type;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsUseCaseTest {

    private static final String ENV_ID = "environment-id";

    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchApiAnalyticsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchApiAnalyticsUseCase(apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_definition_is_not_v4_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, type)))
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_does_not_belong_to_environment_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId("another-environment").build()));

        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, type)))
            .isInstanceOf(ApiNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_is_tcp_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, type)))
            .isInstanceOf(TcpProxyNotSupportedException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_is_not_proxy_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, type)))
            .isInstanceOf(ApiInvalidTypeException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_validate_api_scope_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));

        Output result = cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, type));

        assertThat(result.type()).isEqualTo(type);
    }
}
