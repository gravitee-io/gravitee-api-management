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
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetApiAnalyticsUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant FROM = Instant.parse("2024-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2024-10-01T03:00:00Z");
    private static final long ONE_HOUR_IN_MS = Duration.ofHours(1).toMillis();

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private GetApiAnalyticsUseCase cut;

    @BeforeEach
    void set_up() {
        cut = new GetApiAnalyticsUseCase(analyticsQueryService, apiCrudService);
    }

    @AfterEach
    void tear_down() {
        analyticsQueryService.reset();
        apiCrudService.reset();
    }

    @Test
    void should_return_count_output_when_type_is_count() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.requestsCount = RequestsCount.builder().total(42L).build();

        var output = cut.execute(
            GraviteeContext.getExecutionContext(),
            new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "COUNT", FROM, TO, null, null, null)
        );

        assertThat(output).isInstanceOf(GetApiAnalyticsUseCase.CountOutput.class);
        assertThat(((GetApiAnalyticsUseCase.CountOutput) output).count()).isEqualTo(42L);
    }

    @Test
    void should_return_zero_count_when_requests_count_is_empty() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.requestsCount = null;

        var output = cut.execute(
            GraviteeContext.getExecutionContext(),
            new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "COUNT", FROM, TO, null, null, null)
        );

        assertThat(output).isInstanceOf(GetApiAnalyticsUseCase.CountOutput.class);
        assertThat(((GetApiAnalyticsUseCase.CountOutput) output).count()).isZero();
    }

    @Test
    void should_return_date_histo_with_gap_fill_and_aligned_arrays() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.responseStatusOvertime =
            ResponseStatusOvertime
                .builder()
                .timeRange(new ResponseStatusOvertime.TimeRange(FROM, TO, Duration.ofHours(1)))
                .data(Map.of("200", List.of(10L, 12L, 14L), "500", List.of(1L)))
                .build();

        var output = cut.execute(
            GraviteeContext.getExecutionContext(),
            new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "DATE_HISTO", FROM, TO, "status", ONE_HOUR_IN_MS, "DESC")
        );

        assertThat(output).isInstanceOf(GetApiAnalyticsUseCase.DateHistoOutput.class);
        var dateHisto = (GetApiAnalyticsUseCase.DateHistoOutput) output;

        assertThat(dateHisto.timestamp())
            .containsExactly(
                FROM.toEpochMilli(),
                FROM.plus(Duration.ofHours(1)).toEpochMilli(),
                FROM.plus(Duration.ofHours(2)).toEpochMilli()
            );
        assertThat(dateHisto.values()).hasSize(2);
        assertThat(dateHisto.values()).allSatisfy(series -> assertThat(series.buckets()).hasSize(dateHisto.timestamp().size()));
        assertThat(dateHisto.values())
            .anySatisfy(series -> {
                if ("500".equals(series.field())) {
                    assertThat(series.buckets()).containsExactly(1L, 0L, 0L);
                    assertThat(series.metadata()).containsEntry("name", "500");
                }
            });
    }

    @Test
    void should_return_empty_date_histo_when_no_data_is_returned() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));
        analyticsQueryService.responseStatusOvertime = null;

        var output = cut.execute(
            GraviteeContext.getExecutionContext(),
            new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "DATE_HISTO", FROM, TO, "status", ONE_HOUR_IN_MS, "ASC")
        );

        assertThat(output).isInstanceOf(GetApiAnalyticsUseCase.DateHistoOutput.class);
        var dateHisto = (GetApiAnalyticsUseCase.DateHistoOutput) output;
        assertThat(dateHisto.timestamp()).isEmpty();
        assertThat(dateHisto.values()).isEmpty();
    }

    @Test
    void should_throw_validation_exception_when_stats_field_is_unknown() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "STATS", FROM, TO, "unknown-field", null, null)
                )
            )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Unknown field 'unknown-field' for STATS");
    }

    @Test
    void should_throw_validation_exception_when_order_is_invalid() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "COUNT", FROM, TO, null, null, "SIDEWAYS")
                )
            )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("order must be ASC or DESC");
    }

    @Test
    void should_throw_api_not_found_when_api_does_not_belong_to_environment() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4().toBuilder().environmentId("another-env").build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "COUNT", FROM, TO, null, null, null)
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_api_invalid_definition_version_when_api_is_not_v4() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2().toBuilder().environmentId(ENV_ID).build()));

        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new GetApiAnalyticsUseCase.Input(MY_API, ENV_ID, "COUNT", FROM, TO, null, null, null)
                )
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }
}
