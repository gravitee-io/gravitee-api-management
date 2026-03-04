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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Output;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Type;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.rest.api.model.v4.analytics.GroupBy;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.Stats;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant FROM = Instant.parse("2023-10-21T10:15:30Z");
    private static final Instant TO = Instant.parse("2023-10-22T10:15:30Z");

    private final FakeAnalyticsQueryService analyticsQueryService = Mockito.spy(new FakeAnalyticsQueryService());
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

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_definition_is_not_v4_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, type, FROM, TO, fieldFor(type), sizeFor(type), orderFor(type))
                )
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_does_not_belong_to_environment_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId("another-environment").build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, type, FROM, TO, fieldFor(type), sizeFor(type), orderFor(type))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_is_tcp_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, type, FROM, TO, fieldFor(type), sizeFor(type), orderFor(type))
                )
            )
            .isInstanceOf(TcpProxyNotSupportedException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_throw_if_api_is_not_proxy_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, type, FROM, TO, fieldFor(type), sizeFor(type), orderFor(type))
                )
            )
            .isInstanceOf(ApiInvalidTypeException.class);
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    void should_validate_api_scope_for_all_query_types(Type type) {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, type, FROM, TO, fieldFor(type), sizeFor(type), orderFor(type))
        );

        assertThat(result.type()).isEqualTo(type);
    }

    @Test
    void should_return_count_from_query_service_for_count_type() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.requestsCount = RequestsCount.builder().total(56L).build();

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Type.COUNT, FROM, TO, null, null, null)
        );

        assertThat(result.type()).isEqualTo(Type.COUNT);
        assertThat(result.count()).isEqualTo(56L);
        verify(analyticsQueryService).searchRequestsCount(any(), eq(MY_API), eq(FROM), eq(TO));
    }

    @Test
    void should_return_zero_when_no_count_is_found() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.requestsCount = null;

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Type.COUNT, FROM, TO, null, null, null)
        );

        assertThat(result.type()).isEqualTo(Type.COUNT);
        assertThat(result.count()).isZero();
    }

    @Test
    void should_return_stats_from_query_service_for_stats_type() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.stats = Stats.builder().count(11L).min(2D).max(1500D).avg(125.5D).sum(1549567D).build();

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Type.STATS, FROM, TO, "gateway-response-time-ms", null, null)
        );

        assertThat(result.type()).isEqualTo(Type.STATS);
        assertThat(result.count()).isEqualTo(11L);
        assertThat(result.min()).isEqualTo(2D);
        assertThat(result.max()).isEqualTo(1500D);
        assertThat(result.avg()).isEqualTo(125.5D);
        assertThat(result.sum()).isEqualTo(1549567D);
        verify(analyticsQueryService).searchStats(any(), eq(MY_API), eq(FROM), eq(TO), eq("gateway-response-time-ms"));
    }

    @Test
    void should_return_zeroed_stats_when_no_stats_are_found() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.stats = null;

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Type.STATS, FROM, TO, "status", null, null)
        );

        assertThat(result.type()).isEqualTo(Type.STATS);
        assertThat(result.count()).isZero();
        assertThat(result.min()).isZero();
        assertThat(result.max()).isZero();
        assertThat(result.avg()).isZero();
        assertThat(result.sum()).isZero();
    }

    @Test
    void should_throw_if_stats_field_is_not_supported() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, Type.STATS, FROM, TO, "invalid-field", null, null)
                )
            )
            .hasMessage("Unsupported stats field");
    }

    @Test
    void should_return_group_by_from_query_service_for_group_by_type() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));
        var values = new java.util.LinkedHashMap<String, Long>();
        values.put("404", 7L);
        values.put("200", 3L);
        analyticsQueryService.groupBy =
            GroupBy
                .builder()
                .values(values)
                .metadata(
                    new java.util.LinkedHashMap<>(
                        java.util.Map.of("404", java.util.Map.of("name", "404"), "200", java.util.Map.of("name", "200"))
                    )
                )
                .build();

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Type.GROUP_BY, FROM, TO, "status", null, null)
        );

        assertThat(result.type()).isEqualTo(Type.GROUP_BY);
        assertThat(result.values()).containsExactly(java.util.Map.entry("404", 7L), java.util.Map.entry("200", 3L));
        assertThat(result.metadata()).containsEntry("404", java.util.Map.of("name", "404"));
        verify(analyticsQueryService)
            .searchGroupBy(
                any(),
                eq(MY_API),
                eq(FROM),
                eq(TO),
                eq("status"),
                eq(10),
                eq(io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService.GroupByOrder.DESC)
            );
    }

    @Test
    void should_return_empty_group_by_when_not_found() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.groupBy = null;

        Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Type.GROUP_BY, FROM, TO, "status", null, null)
        );

        assertThat(result.type()).isEqualTo(Type.GROUP_BY);
        assertThat(result.values()).isEmpty();
        assertThat(result.metadata()).isEmpty();
    }

    @Test
    void should_throw_if_group_by_field_is_not_supported() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, Type.GROUP_BY, FROM, TO, "invalid-field", null, null)
                )
            )
            .hasMessage("Unsupported group by field");
    }

    private static String fieldFor(Type type) {
        return (type == Type.STATS || type == Type.GROUP_BY) ? "status" : null;
    }

    private static Integer sizeFor(Type type) {
        return type == Type.GROUP_BY ? 10 : null;
    }

    private static SearchApiAnalyticsUseCase.GroupByOrder orderFor(Type type) {
        return type == Type.GROUP_BY ? SearchApiAnalyticsUseCase.GroupByOrder.DESC : null;
    }
}
