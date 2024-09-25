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
package io.gravitee.apim.core.scoring.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PageFixtures;
import fixtures.core.model.ScoringRulesetFixture;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AsyncJobCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.ScoringProviderInMemory;
import inmemory.ScoringRulesetQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ScoreApiRequestUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final ScoringRuleset CUSTOM_RULESET_1 = ScoringRulesetFixture.aRuleset("ruleset1").withReferenceId(ENVIRONMENT_ID);
    private static final ScoringRuleset CUSTOM_RULESET_2 = ScoringRulesetFixture.aRuleset("ruleset2").withReferenceId(ENVIRONMENT_ID);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AsyncJobCrudServiceInMemory asyncJobCrudService = new AsyncJobCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    ScoringProviderInMemory scoringProvider = new ScoringProviderInMemory();
    ScoringRulesetQueryServiceInMemory scoringRulesetQueryService = new ScoringRulesetQueryServiceInMemory();

    ScoreApiRequestUseCase scoreApiRequestUseCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        scoreApiRequestUseCase =
            new ScoreApiRequestUseCase(
                apiCrudService,
                new ApiDocumentationDomainService(pageQueryService, new PlanQueryServiceInMemory()),
                scoringProvider,
                asyncJobCrudService,
                scoringRulesetQueryService
            );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudService, asyncJobCrudService, pageQueryService, scoringRulesetQueryService).forEach(InMemoryAlternative::reset);
        scoringProvider.reset();
    }

    @Test
    public void should_trigger_scoring_for_swagger_page() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());
        var page = givenExistingPage(
            PageFixtures.aPage().toBuilder().referenceType(Page.ReferenceType.API).referenceId(api.getId()).type(Page.Type.SWAGGER).build()
        );

        // When
        scoreApiRequestUseCase
            .execute(new ScoreApiRequestUseCase.Input(api.getId(), AUDIT_INFO))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        assertThat(scoringProvider.pendingRequests())
            .containsExactly(
                new ScoreRequest(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    api.getId(),
                    List.of(new ScoreRequest.AssetToScore(page.getId(), ScoringAssetType.SWAGGER, page.getName(), page.getContent()))
                )
            );
        assertThat(asyncJobCrudService.storage())
            .containsExactly(
                AsyncJob
                    .builder()
                    .id("generated-id")
                    .sourceId(api.getId())
                    .environmentId(ENVIRONMENT_ID)
                    .initiatorId(USER_ID)
                    .type(AsyncJob.Type.SCORING_REQUEST)
                    .status(AsyncJob.Status.PENDING)
                    .upperLimit(1L)
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    public void should_trigger_scoring_for_asyncapi_page() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());
        var page = givenExistingPage(
            PageFixtures.aPage().toBuilder().referenceType(Page.ReferenceType.API).referenceId(api.getId()).type(Page.Type.ASYNCAPI).build()
        );

        // When
        scoreApiRequestUseCase
            .execute(new ScoreApiRequestUseCase.Input(api.getId(), AUDIT_INFO))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        assertThat(scoringProvider.pendingRequests())
            .containsExactly(
                new ScoreRequest(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    api.getId(),
                    List.of(new ScoreRequest.AssetToScore(page.getId(), ScoringAssetType.ASYNCAPI, page.getName(), page.getContent()))
                )
            );
        assertThat(asyncJobCrudService.storage())
            .containsExactly(
                AsyncJob
                    .builder()
                    .id("generated-id")
                    .sourceId(api.getId())
                    .environmentId(ENVIRONMENT_ID)
                    .initiatorId(USER_ID)
                    .type(AsyncJob.Type.SCORING_REQUEST)
                    .status(AsyncJob.Status.PENDING)
                    .upperLimit(1L)
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    public void should_not_trigger_scoring_when_no_page() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());

        // When
        scoreApiRequestUseCase
            .execute(new ScoreApiRequestUseCase.Input(api.getId(), AUDIT_INFO))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        assertThat(scoringProvider.pendingRequests()).isEmpty();
    }

    @Test
    public void should_trigger_scoring_with_custom_rulesets() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());
        var page = givenExistingPage(
            PageFixtures.aPage().toBuilder().referenceType(Page.ReferenceType.API).referenceId(api.getId()).type(Page.Type.SWAGGER).build()
        );
        givenExistingRulesets(CUSTOM_RULESET_1, CUSTOM_RULESET_2);

        // When
        scoreApiRequestUseCase
            .execute(new ScoreApiRequestUseCase.Input(api.getId(), AUDIT_INFO))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        assertThat(scoringProvider.pendingRequests())
            .containsExactly(
                new ScoreRequest(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    api.getId(),
                    List.of(new ScoreRequest.AssetToScore(page.getId(), ScoringAssetType.SWAGGER, page.getName(), page.getContent())),
                    List.of(CUSTOM_RULESET_1.payload(), CUSTOM_RULESET_2.payload())
                )
            );
    }

    @ParameterizedTest
    @EnumSource(value = Page.Type.class, mode = EnumSource.Mode.EXCLUDE, names = { "SWAGGER", "ASYNCAPI" })
    public void should_not_trigger_scoring_when_no_page_type_supported(Page.Type pageType) {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());
        givenExistingPage(
            PageFixtures.aPage().toBuilder().referenceType(Page.ReferenceType.API).referenceId(api.getId()).type(pageType).build()
        );

        // When
        scoreApiRequestUseCase
            .execute(new ScoreApiRequestUseCase.Input(api.getId(), AUDIT_INFO))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        assertThat(scoringProvider.pendingRequests()).isEmpty();
    }

    private Api givenExistingApi(Api api) {
        apiCrudService.initWith(List.of(api));
        return api;
    }

    private Page givenExistingPage(Page page) {
        pageQueryService.initWith(List.of(page));
        return page;
    }

    private void givenExistingRulesets(ScoringRuleset... rulesets) {
        scoringRulesetQueryService.initWith(List.of(rulesets));
    }
}
