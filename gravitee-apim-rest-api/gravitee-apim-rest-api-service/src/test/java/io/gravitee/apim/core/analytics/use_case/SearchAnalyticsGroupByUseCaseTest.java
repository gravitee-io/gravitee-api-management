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
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsGroupByUseCase.Input;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAnalyticsGroupByUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudServiceInMemory = new ApplicationCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
    private SearchAnalyticsGroupByUseCase cut;

    @BeforeEach
    void setUp() {
        cut =
            new SearchAnalyticsGroupByUseCase(
                analyticsQueryService,
                apiCrudServiceInMemory,
                applicationCrudServiceInMemory,
                planCrudServiceInMemory
            );
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
        applicationCrudServiceInMemory.reset();
        planCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_api_not_found() {
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", 10, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", 10, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", 10, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_throw_if_api_not_in_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, "another-environment", "status", 10, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_field_is_null() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, null, 10, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("field is required for GROUP_BY analytics");
    }

    @Test
    void should_throw_if_field_not_in_allowlist() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "unsupported-field", 10, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported field for GROUP_BY analytics");
    }

    @Test
    void should_return_group_by_without_metadata_for_status_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByValues = Map.of("200", 50L, "404", 10L);

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "status", 10, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.groupBy().values()).containsEntry("200", 50L).containsEntry("404", 10L);
        assertThat(result.groupBy().metadata()).isEmpty();
    }

    @Test
    void should_return_group_by_with_application_metadata() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByValues = Map.of("app-1", 30L);
        applicationCrudServiceInMemory.initWith(List.of(BaseApplicationEntity.builder().id("app-1").name("My Application").build()));

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "application", 10, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.groupBy().values()).containsEntry("app-1", 30L);
        assertThat(result.groupBy().metadata()).containsKey("app-1");
        assertThat(result.groupBy().metadata().get("app-1")).containsEntry("name", "My Application");
    }

    @Test
    void should_return_group_by_with_deleted_application_in_metadata() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByValues = Map.of("deleted-app-id", 5L);
        // application not in storage → ApplicationNotFoundException

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "application", 10, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.groupBy().metadata().get("deleted-app-id"))
            .containsEntry("name", "deleted-app-id")
            .containsEntry("deleted", "true")
            .containsEntry("unknown", "true");
    }

    @Test
    void should_return_group_by_with_plan_metadata() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.groupByValues = Map.of("my-plan", 20L);
        planCrudServiceInMemory.initWith(List.of(PlanFixtures.aPlanHttpV4()));

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "plan", 10, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.groupBy().values()).containsEntry("my-plan", 20L);
        assertThat(result.groupBy().metadata().get("my-plan")).containsEntry("name", "My plan");
    }
}
