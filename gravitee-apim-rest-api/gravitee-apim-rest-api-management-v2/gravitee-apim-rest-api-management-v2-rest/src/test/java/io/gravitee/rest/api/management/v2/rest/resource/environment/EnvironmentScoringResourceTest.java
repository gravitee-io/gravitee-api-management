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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.Mockito.doReturn;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.ScoringReportFixture;
import inmemory.ApiQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.ScoringReportQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentApiScore;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentApisScoringResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentScoringOverview;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentScoringResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final ZonedDateTime CREATED_AT = Instant.parse("2023-10-22T10:15:30Z").atZone(ZoneId.systemDefault());

    WebTarget scoringApisTarget;
    WebTarget scoringOverviewTarget;
    WebTarget apisTarget;

    @Inject
    ApiQueryServiceInMemory apiQueryService;

    @Inject
    ScoringReportQueryServiceInMemory scoringReportQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT;
    }

    @BeforeEach
    void setup() {
        scoringApisTarget = rootTarget().path("scoring").path("apis");
        scoringOverviewTarget = rootTarget().path("scoring").path("overview");
        apisTarget = rootTarget().path("apis");

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();

        Stream.of(apiQueryService, scoringReportQueryService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetApisScoring {

        @Test
        void should_return_environment_api_score() {
            // Given
            var expectedTotal = 50;

            apiQueryService.initWith(
                List.of(
                    ApiFixtures.aFederatedApi().toBuilder().id("api1").name("api-1").environmentId(ENVIRONMENT).build(),
                    ApiFixtures.aFederatedApi().toBuilder().id("api2").name("api-2").environmentId(ENVIRONMENT).build()
                )
            );
            scoringReportQueryService.initWith(List.of(aReport("report1").withApiId("api1")));

            // When
            Response response = scoringApisTarget.request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(EnvironmentApisScoringResponse.class)
                .satisfies(result -> {
                    assertThat(result.getData())
                        .hasSize(2)
                        .containsOnly(
                            EnvironmentApiScore
                                .builder()
                                .id("api1")
                                .name("api-1")
                                .pictureUrl(apisTarget.path("api1").path("picture").queryParam("hash", "1580674922000").getUri().toString())
                                .errors(1)
                                .warnings(1)
                                .infos(1)
                                .hints(1)
                                .build(),
                            EnvironmentApiScore
                                .builder()
                                .id("api2")
                                .name("api-2")
                                .pictureUrl(apisTarget.path("api2").path("picture").queryParam("hash", "1580674922000").getUri().toString())
                                .build()
                        );
                    assertThat(result.getPagination())
                        .isEqualTo(Pagination.builder().page(1).perPage(10).pageItemsCount(2).pageCount(1).totalCount(2L).build());
                });
        }

        @Test
        void should_compute_pagination() {
            // Given
            var expectedTotal = 15;
            var pageNumber = 2;
            var pageSize = 5;

            apiQueryService.initWith(
                IntStream
                    .range(0, expectedTotal)
                    .mapToObj(i -> ApiFixtures.aFederatedApi().toBuilder().id(String.valueOf(i)).environmentId(ENVIRONMENT).build())
                    .map(api -> (Api) api)
                    .toList()
            );
            scoringReportQueryService.initWith(
                IntStream.range(0, expectedTotal).mapToObj(String::valueOf).map(id -> aReport(id).withApiId(id)).toList()
            );

            // When
            Response response = scoringApisTarget.queryParam("page", pageNumber).queryParam("perPage", pageSize).request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(EnvironmentApisScoringResponse.class)
                .extracting(EnvironmentApisScoringResponse::getPagination)
                .isEqualTo(
                    Pagination
                        .builder()
                        .page(pageNumber)
                        .perPage(pageSize)
                        .pageItemsCount(pageSize)
                        .pageCount(3)
                        .totalCount((long) expectedTotal)
                        .build()
                );
        }

        @Test
        void should_compute_links() {
            // Given
            var expectedTotal = 20;
            var page = 2;
            var pageSize = 5;

            apiQueryService.initWith(
                IntStream
                    .range(0, expectedTotal)
                    .mapToObj(i -> ApiFixtures.aFederatedApi().toBuilder().id(String.valueOf(i)).environmentId(ENVIRONMENT).build())
                    .map(api -> (Api) api)
                    .toList()
            );
            scoringReportQueryService.initWith(
                IntStream.range(0, expectedTotal).mapToObj(String::valueOf).map(id -> aReport(id).withApiId(id)).toList()
            );

            // When
            Response response = scoringApisTarget.queryParam("page", page).queryParam("perPage", pageSize).request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(EnvironmentApisScoringResponse.class)
                .extracting(EnvironmentApisScoringResponse::getLinks)
                .isEqualTo(
                    Links
                        .builder()
                        .self(scoringApisTarget.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                        .first(scoringApisTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .last(scoringApisTarget.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                        .previous(scoringApisTarget.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .next(scoringApisTarget.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
                        .build()
                );
        }
    }

    @Nested
    class GetScoringEnvironmentOverview {

        @Test
        void should_return_environment_overview() {
            // Given
            scoringReportQueryService.initWith(
                List.of(
                    aReport("report1").toBuilder().apiId("api1").environmentId(ENVIRONMENT).build(),
                    aReport("report2").toBuilder().apiId("api2").environmentId(ENVIRONMENT).build(),
                    aReport("report3").toBuilder().apiId("api3").environmentId(ENVIRONMENT).build()
                )
            );

            // When
            Response response = scoringOverviewTarget.request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(EnvironmentScoringOverview.class)
                .isEqualTo(
                    EnvironmentScoringOverview.builder().id(ENVIRONMENT).score(0.84).errors(3).warnings(3).infos(3).hints(3).build()
                );
        }
    }

    private static ScoringReport aReport(String id) {
        return ScoringReportFixture
            .aScoringReport()
            .toBuilder()
            .id(id)
            .environmentId(ENVIRONMENT)
            .createdAt(CREATED_AT)
            .summary(new ScoringReport.Summary(0.84D, 1L, 1L, 1L, 1L))
            .assets(List.of())
            .build();
    }
}
