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
package io.gravitee.apim.core.promotion.use_case;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PromotionCrudServiceInMemory;
import inmemory.PromotionQueryServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.promotion.domain_service.PromotionContextDomainService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PromotionContextDomainServiceTest {

    private static final String API_ID = "api-id";
    private static final String CROSS_ID = "api-cross-id";
    private static final String PROMOTION_ID = "promotion-id";
    private static final String DEFAULT_ENV_ID = "DEFAULT";
    private static final String TARGET_ENV_ID = "TARGET-ENV-ID";
    private static final String TARGET_ENV_COCKPIT_ID = "TARGET-ENV-COCKPIT-ID";
    private final PromotionCrudServiceInMemory promotionCrudService = new PromotionCrudServiceInMemory();
    private final PromotionQueryServiceInMemory promotionQueryService = new PromotionQueryServiceInMemory(promotionCrudService);
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory(apiCrudService);
    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private final PromotionContextDomainService service = new PromotionContextDomainService(
        promotionCrudService,
        promotionQueryService,
        apiQueryServiceInMemory,
        apiCrudService,
        environmentCrudService,
        new JsonMapper()
    );

    @BeforeEach
    void setUp() {
        environmentCrudService.initWith(
            List.of(
                Environment.builder().id(DEFAULT_ENV_ID).build(),
                Environment.builder().id(TARGET_ENV_ID).cockpitId(TARGET_ENV_COCKPIT_ID).build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(promotionCrudService, apiQueryServiceInMemory, environmentCrudService, apiCrudService).forEach(
            InMemoryAlternative::reset
        );
    }

    @Test
    void should_find_promotion_with_api() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(
                """
                {
                    "api": {
                        "crossId": "api-cross-id",
                        "definitionVersion": "V4",
                        "name": "My Api"
                    }
                }
                """
            )
            .build();
        promotionCrudService.initWith(List.of(promotion));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isEqualTo(promotion);
        assertThat(result.expectedDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(result.existingPromotedApi()).isNull();
        assertThat(result.targetEnvId()).isEqualTo(TARGET_ENV_ID);
    }

    @Test
    void should_return_existing_target_api_when_crossId_matches() {
        Promotion promotion = aV4Promotion();
        promotionCrudService.initWith(List.of(promotion));
        var targetApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId(CROSS_ID)
            .environmentId(TARGET_ENV_ID)
            .build();
        apiQueryServiceInMemory.initWith(List.of(targetApi));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(targetApi);
    }

    @Test
    void should_not_resolve_target_api_when_crossId_does_not_match_and_there_is_no_last_accepted_promotion() {
        Promotion promotion = aV4Promotion();
        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(
            List.of(
                ApiFixtures.aProxyApiV4()
                    .toBuilder()
                    .id("already-promoted-api")
                    .crossId("different-cross-id")
                    .environmentId(TARGET_ENV_ID)
                    .build()
            )
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_resolve_target_api_from_last_accepted_promotion_when_crossId_does_not_match() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = aV4Promotion()
            .toBuilder()
            .id("last-accepted-promotion")
            .status(PromotionStatus.ACCEPTED)
            .targetApiId("already-promoted-api")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        var targetApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("already-promoted-api")
            .crossId("different-cross-id")
            .environmentId(TARGET_ENV_ID)
            .build();
        apiQueryServiceInMemory.initWith(List.of(targetApi));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(targetApi);
    }

    @Test
    void should_prefer_crossId_match_over_last_accepted_promotion() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = aV4Promotion()
            .toBuilder()
            .id("last-accepted-promotion")
            .status(PromotionStatus.ACCEPTED)
            .targetApiId("api-from-last-promotion")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        var apiMatchingCrossId = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("api-matching-cross-id")
            .crossId(CROSS_ID)
            .environmentId(TARGET_ENV_ID)
            .build();
        var apiFromLastPromotion = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("api-from-last-promotion")
            .crossId("different-cross-id")
            .environmentId(TARGET_ENV_ID)
            .build();
        apiQueryServiceInMemory.initWith(List.of(apiMatchingCrossId, apiFromLastPromotion));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(apiMatchingCrossId);
    }

    @Test
    void should_resolve_most_recent_accepted_promotion_when_multiple_exist() {
        Promotion promotion = aV4Promotion();
        Promotion olderAccepted = aV4Promotion()
            .toBuilder()
            .id("older-accepted-promotion")
            .status(PromotionStatus.ACCEPTED)
            .targetApiId("older-promoted-api")
            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
            .build();
        Promotion newerAccepted = aV4Promotion()
            .toBuilder()
            .id("newer-accepted-promotion")
            .status(PromotionStatus.ACCEPTED)
            .targetApiId("newer-promoted-api")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        promotionCrudService.initWith(List.of(promotion, olderAccepted, newerAccepted));
        var olderApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("older-promoted-api")
            .crossId("older-cross-id")
            .environmentId(TARGET_ENV_ID)
            .build();
        var newerApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id("newer-promoted-api")
            .crossId("newer-cross-id")
            .environmentId(TARGET_ENV_ID)
            .build();
        apiQueryServiceInMemory.initWith(List.of(olderApi, newerApi));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(newerApi);
    }

    @Test
    void should_not_resolve_target_api_when_last_accepted_promotion_points_to_missing_api() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = aV4Promotion()
            .toBuilder()
            .id("last-accepted-promotion")
            .status(PromotionStatus.ACCEPTED)
            .targetApiId("deleted-promoted-api")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        promotionCrudService.initWith(List.of(promotion, lastAccepted));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_from_history_when_it_lives_in_another_environment() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "api-in-other-env", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        apiQueryServiceInMemory.initWith(
            List.of(
                ApiFixtures.aProxyApiV4()
                    .toBuilder()
                    .id("api-in-other-env")
                    .crossId("different-cross-id")
                    .environmentId(DEFAULT_ENV_ID)
                    .build()
            )
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_from_history_when_it_has_no_environment_id() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "api-with-no-env", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-with-no-env").crossId("different-cross-id").environmentId(null).build())
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_from_promotions_that_are_not_accepted() {
        Promotion promotion = aV4Promotion();
        Promotion rejected = anAcceptedPromotion("rejected-promotion", "rejected-target-api", "2026-01-01T00:00:00Z")
            .toBuilder()
            .status(PromotionStatus.REJECTED)
            .build();
        Promotion pending = anAcceptedPromotion("pending-promotion", "pending-target-api", "2026-02-01T00:00:00Z")
            .toBuilder()
            .status(PromotionStatus.TO_BE_VALIDATED)
            .build();
        Promotion inError = anAcceptedPromotion("error-promotion", "error-target-api", "2026-03-01T00:00:00Z")
            .toBuilder()
            .status(PromotionStatus.ERROR)
            .build();
        promotionCrudService.initWith(List.of(promotion, rejected, pending, inError));
        apiQueryServiceInMemory.initWith(
            List.of(
                aTargetApi("rejected-target-api", "cross-1"),
                aTargetApi("pending-target-api", "cross-2"),
                aTargetApi("error-target-api", "cross-3")
            )
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_from_accepted_promotions_of_another_source_api() {
        Promotion promotion = aV4Promotion();
        Promotion otherApiPromotion = anAcceptedPromotion("other-api-promotion", "other-api-target", "2026-01-01T00:00:00Z")
            .toBuilder()
            .apiId("another-source-api")
            .build();
        promotionCrudService.initWith(List.of(promotion, otherApiPromotion));
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("other-api-target", "different-cross-id")));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_from_accepted_promotions_to_another_target_environment() {
        Promotion promotion = aV4Promotion();
        Promotion otherEnvPromotion = anAcceptedPromotion("other-env-promotion", "other-env-target", "2026-01-01T00:00:00Z")
            .toBuilder()
            .targetEnvCockpitId("ANOTHER-ENV-COCKPIT-ID")
            .build();
        promotionCrudService.initWith(List.of(promotion, otherEnvPromotion));
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("other-env-target", "different-cross-id")));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_when_history_has_no_target_api_id_and_the_definition_declares_no_path() {
        Promotion promotion = aV4Promotion();
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        // aV4Promotion() declares no listener, so the context path fallback has nothing to match on either.
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("already-promoted-api", "different-cross-id")));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_use_the_promotion_being_processed_as_history() {
        Promotion promotion = aV4Promotion().toBuilder().status(PromotionStatus.ACCEPTED).targetApiId("self-target-api").build();
        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("self-target-api", "different-cross-id")));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_fall_back_to_older_history_when_newest_target_api_is_missing() {
        // Newest accepted promotion points to an API that has been deleted since; the older one is still valid.
        Promotion promotion = aV4Promotion();
        Promotion olderAccepted = anAcceptedPromotion("older-accepted-promotion", "older-promoted-api", "2025-01-01T00:00:00Z");
        Promotion newerAccepted = anAcceptedPromotion("newer-accepted-promotion", "deleted-promoted-api", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, olderAccepted, newerAccepted));
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("older-promoted-api", "older-cross-id")));

        var result = service.getPromotionContext(promotion.getId(), true);

        // Documented behaviour: the most recent accepted promotion is authoritative. If its API is gone the
        // promotion creates a new API; we do not silently pick an older API that may have been replaced on purpose.
        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_throw_when_api_resolved_from_history_has_a_different_definition_version() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "v2-target-api", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        apiQueryServiceInMemory.initWith(
            List.of(
                ApiFixtures.aProxyApiV2().toBuilder().id("v2-target-api").crossId("different-cross-id").environmentId(TARGET_ENV_ID).build()
            )
        );

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));

        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessageContaining("different definition version");
    }

    @Test
    void should_resolve_target_api_from_history_when_rejecting_without_definition_version_check() {
        Promotion promotion = aV4Promotion();
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "v2-target-api", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        var v2TargetApi = ApiFixtures.aProxyApiV2()
            .toBuilder()
            .id("v2-target-api")
            .crossId("different-cross-id")
            .environmentId(TARGET_ENV_ID)
            .build();
        apiQueryServiceInMemory.initWith(List.of(v2TargetApi));

        var result = service.getPromotionContext(promotion.getId(), false);

        assertThat(result.existingPromotedApi()).isEqualTo(v2TargetApi);
    }

    @Test
    void should_resolve_target_api_from_history_when_created_at_is_missing() {
        Promotion promotion = aV4Promotion();
        Promotion withoutDate = anAcceptedPromotion("no-date-promotion", "no-date-target-api", null);
        Promotion withDate = anAcceptedPromotion("dated-promotion", "dated-target-api", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, withoutDate, withDate));
        var datedApi = aTargetApi("dated-target-api", "cross-dated");
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("no-date-target-api", "cross-no-date"), datedApi));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(datedApi);
    }

    @Test
    void should_not_search_history_when_promotion_has_no_source_api_id() {
        Promotion promotion = aV4Promotion().toBuilder().apiId(null).build();
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "already-promoted-api", "2026-01-01T00:00:00Z")
            .toBuilder()
            .apiId(null)
            .build();
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        apiQueryServiceInMemory.initWith(List.of(aTargetApi("already-promoted-api", "different-cross-id")));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Context path resolution: the path an environment upgrading from an affected version takes, where every
    // accepted promotion row predates targetApiId persistence and therefore has target_api_id = NULL.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    void should_resolve_target_api_by_context_path_when_history_predates_target_api_id_persistence() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        // Accepted before the fix: the row proves the API was promoted here, but carries no targetApiId.
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        var apiServingThePath = anApiServingTheContextPath("already-promoted-api", "diverged-cross-id", TARGET_ENV_ID);
        apiQueryServiceInMemory.initWith(List.of(apiServingThePath));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(apiServingThePath);
    }

    @Test
    void should_not_resolve_target_api_by_context_path_when_rejecting() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        apiQueryServiceInMemory.initWith(List.of(anApiServingTheContextPath("already-promoted-api", "diverged-cross-id", TARGET_ENV_ID)));

        var result = service.getPromotionContext(promotion.getId(), false);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_by_context_path_when_the_api_was_never_promoted_to_this_environment() {
        // No accepted promotion for this source API: an API owning the path is a stranger, never overwrite it.
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(List.of(anApiServingTheContextPath("unrelated-api", "unrelated-cross-id", TARGET_ENV_ID)));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_by_overlapping_nested_path() {
        // scanPaths reports containment, so /orders overlaps /orders/v2. That is not identity — updating
        // the nested API would replace an unrelated definition.
        Promotion promotion = aV4PromotionServing("/orders");
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        apiQueryServiceInMemory.initWith(
            List.of(anApiServingTheContextPath("unrelated-nested-api", "unrelated-cross-id", TARGET_ENV_ID, "/orders/v2"))
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_by_context_path_when_several_apis_conflict() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        apiQueryServiceInMemory.initWith(
            List.of(
                anApiServingTheContextPath("first-conflicting-api", "cross-1", TARGET_ENV_ID),
                // A sub-path of the promoted one also conflicts, so the match is ambiguous.
                anApiServingTheContextPath("second-conflicting-api", "cross-2", TARGET_ENV_ID, CONTEXT_PATH + "/sub")
            )
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_not_resolve_target_api_by_context_path_in_another_environment() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        apiQueryServiceInMemory.initWith(List.of(anApiServingTheContextPath("api-in-other-env", "diverged-cross-id", DEFAULT_ENV_ID)));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isNull();
    }

    @Test
    void should_prefer_promotion_history_over_context_path() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "api-from-history", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        var apiFromHistory = anApiServingTheContextPath("api-from-history", "cross-history", TARGET_ENV_ID, "/another-path");
        apiQueryServiceInMemory.initWith(
            List.of(apiFromHistory, anApiServingTheContextPath("api-on-the-path", "cross-path", TARGET_ENV_ID))
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(apiFromHistory);
    }

    @Test
    void should_prefer_cross_id_over_context_path() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        var apiWithMatchingCrossId = anApiServingTheContextPath("api-with-cross-id", CROSS_ID, TARGET_ENV_ID, "/another-path");
        apiQueryServiceInMemory.initWith(
            List.of(apiWithMatchingCrossId, anApiServingTheContextPath("api-on-the-path", "cross-path", TARGET_ENV_ID))
        );

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.existingPromotedApi()).isEqualTo(apiWithMatchingCrossId);
    }

    @Test
    void should_report_the_context_path_when_the_api_owning_it_has_a_different_definition_version() {
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH);
        Promotion legacyAccepted = anAcceptedPromotion("legacy-accepted-promotion", null, "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, legacyAccepted));
        var v2ApiOnThePath = ApiFixtures.aProxyApiV2()
            .toBuilder()
            .id("v2-api-on-the-path")
            .crossId("diverged-cross-id")
            .environmentId(TARGET_ENV_ID)
            .build();
        ((io.gravitee.definition.model.Api) v2ApiOnThePath.getApiDefinitionValue()).getProxy().setVirtualHosts(
            List.of(new io.gravitee.definition.model.VirtualHost(CONTEXT_PATH))
        );
        apiQueryServiceInMemory.initWith(List.of(v2ApiOnThePath));

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));

        // The operator needs to know the conflict is on the context path, not on the crossId.
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("context path")
            .hasMessageContaining("v2-api-on-the-path");
    }

    @Test
    void should_not_apply_any_fallback_for_v2_promotions() {
        // V2 resolves its own target inside PromotionServiceImpl; widening resolution here would make the definition
        // version guard reject V2 promotions that used to succeed.
        Promotion promotion = aV4PromotionServing(CONTEXT_PATH).toBuilder().apiDefinition(aV2Definition(CONTEXT_PATH)).build();
        Promotion lastAccepted = anAcceptedPromotion("last-accepted-promotion", "already-promoted-api", "2026-01-01T00:00:00Z");
        promotionCrudService.initWith(List.of(promotion, lastAccepted));
        apiQueryServiceInMemory.initWith(List.of(anApiServingTheContextPath("already-promoted-api", "diverged-cross-id", TARGET_ENV_ID)));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result.expectedDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
        assertThat(result.existingPromotedApi()).isNull();
    }

    private static final String CONTEXT_PATH = "/http_proxy";

    private static io.gravitee.apim.core.api.model.Api anApiServingTheContextPath(String id, String crossId, String environmentId) {
        return anApiServingTheContextPath(id, crossId, environmentId, CONTEXT_PATH);
    }

    private static io.gravitee.apim.core.api.model.Api anApiServingTheContextPath(
        String id,
        String crossId,
        String environmentId,
        String path
    ) {
        var api = ApiFixtures.aProxyApiV4().toBuilder().id(id).crossId(crossId).environmentId(environmentId).build();
        ((io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue()).setListeners(
            List.of(
                io.gravitee.definition.model.v4.listener.http.HttpListener.builder()
                    .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path(path).build()))
                    .build()
            )
        );
        return api;
    }

    private static Promotion aV4PromotionServing(String path) {
        return aV4Promotion()
            .toBuilder()
            .apiDefinition(
                """
                {
                    "api": {
                        "crossId": "%s",
                        "definitionVersion": "V4",
                        "name": "My Api",
                        "listeners": [
                            { "type": "HTTP", "paths": [ { "path": "%s" } ] }
                        ]
                    }
                }
                """.formatted(CROSS_ID, path)
            )
            .build();
    }

    private static String aV2Definition(String path) {
        return """
        {
            "gravitee": "2.0.0",
            "crossId": "%s",
            "name": "My Api",
            "proxy": { "virtual_hosts": [ { "path": "%s" } ] }
        }
        """.formatted(CROSS_ID, path);
    }

    private static Promotion anAcceptedPromotion(String id, String targetApiId, String createdAt) {
        return aV4Promotion()
            .toBuilder()
            .id(id)
            .status(PromotionStatus.ACCEPTED)
            .targetApiId(targetApiId)
            .createdAt(createdAt == null ? null : Instant.parse(createdAt))
            .build();
    }

    private static io.gravitee.apim.core.api.model.Api aTargetApi(String id, String crossId) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(id).crossId(crossId).environmentId(TARGET_ENV_ID).build();
    }

    private static Promotion aV4Promotion() {
        return Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(
                """
                {
                    "api": {
                        "crossId": "api-cross-id",
                        "definitionVersion": "V4",
                        "name": "My Api"
                    }
                }
                """
            )
            .build();
    }

    @Test
    @SneakyThrows
    void should_find_promotion_with_api_v2() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/legacy-v2-export.json"), StandardCharsets.UTF_8))
            .build();
        promotionCrudService.initWith(List.of(promotion));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isEqualTo(promotion);
        assertThat(result.expectedDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
    }

    @Test
    void should_throw_exception_definition_version_not_exist() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(
                """
                    {
                        "api": {
                            "crossId": "api-cross-id",
                            "definitionVersion": "yolo"
                        }
                    }
                """
            )
            .build();
        promotionCrudService.initWith(List.of(promotion));

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        assertThat(throwable).hasMessage("An error occurred while try to parse promotion definition version promotion-id");
    }

    @Test
    void should_throw_exception_when_crossId_not_found() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition("{}")
            .build();
        promotionCrudService.initWith(List.of(promotion));

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        assertThat(throwable).hasMessage("An error occurred while trying to extract crossId from promotion promotion-id");
    }

    @Test
    void should_throw_exception_when_definition_version_not_found() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition("{\"crossId\": \"api-cross-id\"}")
            .build();
        promotionCrudService.initWith(List.of(promotion));

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(throwable).hasMessage("Could not determine definition version for promotion promotion-id");
    }

    @Test
    @SneakyThrows
    void should_throw_exception_when_target_api_is_migrated() {
        String v2CrossId = "26133db6-9861-4dd7-933d-b698619dd7e6";
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/legacy-v2-export.json"), StandardCharsets.UTF_8))
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV4().toBuilder().id("target-v4-api").crossId(v2CrossId).environmentId(TARGET_ENV_ID).build())
        );

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(throwable).hasMessage(
            "An API with the same crossId already exists with a different definition version (API target-v4-api)"
        );
    }

    @Test
    void should_throw_exception_when_source_api_is_migrated() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .apiDefinition(
                """
                {
                    "api": {
                        "crossId": "api-cross-id",
                        "definitionVersion": "V4",
                        "name": "My Api"
                    }
                }
                """
            )
            .targetEnvCockpitId("TARGET-ENV")
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV2().toBuilder().id("target-v2-api").crossId(CROSS_ID).environmentId("TARGET-ENV-ID").build())
        );
        environmentCrudService.initWith(
            List.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("TARGET-ENV-ID").cockpitId("TARGET-ENV").build())
        );

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(throwable).hasMessage(
            "An API with the same crossId already exists with a different definition version (API target-v2-api)"
        );
    }

    @Test
    void should_allow_promotion_rejection_when_versions_missmatch() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .apiDefinition(
                """
                {
                    "api": {
                        "crossId": "api-cross-id",
                        "definitionVersion": "V4",
                        "name": "My Api"
                    }
                }
                """
            )
            .targetEnvCockpitId("TARGET-ENV")
            .build();

        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV2().toBuilder().id("target-v2-api").crossId(CROSS_ID).environmentId("TARGET-ENV-ID").build())
        );
        environmentCrudService.initWith(
            List.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("TARGET-ENV-ID").cockpitId("TARGET-ENV").build())
        );

        assertThat(service.getPromotionContext(promotion.getId(), false)).isNotNull();
    }
}
