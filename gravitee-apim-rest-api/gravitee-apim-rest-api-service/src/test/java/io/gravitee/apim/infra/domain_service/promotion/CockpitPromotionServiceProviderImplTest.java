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
package io.gravitee.apim.infra.domain_service.promotion;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.PromotionFixtures;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionAuthor;
import io.gravitee.apim.core.promotion.model.PromotionRequest;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.model.promotion.PromotionRequestEntity;
import io.gravitee.rest.api.service.cockpit.services.CockpitPromotionService;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.promotion.PromotionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CockpitPromotionServiceProviderImplTest {

    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final String API_ID = "api-id";
    private static final String TARGET_COCKPIT_ENV_ID = "target-env-id";
    private static final String TARGET_ENV_NAME = "target-env-name";
    private static final String USER_ID = "user-id";
    private static final PromotionAuthor PROMOTION_AUTHOR = PromotionFixtures.aPromotionAuthor().toBuilder().userId(USER_ID).build();
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Promotion PROMOTION = PromotionFixtures.aPromotion()
        .toBuilder()
        .apiId(API_ID)
        .targetEnvCockpitId(TARGET_COCKPIT_ENV_ID)
        .targetEnvName(TARGET_ENV_NAME)
        .author(PROMOTION_AUTHOR)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
    private final CockpitPromotionService cockpitPromotionService = mock(CockpitPromotionService.class);
    private final PromotionService promotionService = mock(PromotionService.class);

    CockpitPromotionServiceProvider service;

    @BeforeEach
    void setUp() {
        service = new CockpitPromotionServiceProviderImpl(cockpitPromotionService, promotionService);
    }

    @Nested
    class RequestPromotion {

        @Test
        void should_succeed() {
            when(cockpitPromotionService.requestPromotion(any(), any())).thenReturn(new CockpitReply<>(null, CockpitReplyStatus.SUCCEEDED));
            assertThat(service.requestPromotion(ORGANIZATION_ID, ENVIRONMENT_ID, PROMOTION)).isEqualTo(
                io.gravitee.apim.core.cockpit.model.CockpitReplyStatus.SUCCEEDED
            );
        }

        @Test
        void should_fail() {
            when(cockpitPromotionService.requestPromotion(any(), any())).thenReturn(new CockpitReply<>(null, CockpitReplyStatus.ERROR));
            assertThat(service.requestPromotion(ORGANIZATION_ID, ENVIRONMENT_ID, PROMOTION)).isEqualTo(
                io.gravitee.apim.core.cockpit.model.CockpitReplyStatus.ERROR
            );
        }
    }

    @Nested
    class CreatePromotion {

        @Test
        void should_create_api_v2_promotion() {
            PromotionEntityAuthor promotionEntityAuthor = new PromotionEntityAuthor();
            promotionEntityAuthor.setUserId(USER_ID);
            promotionEntityAuthor.setDisplayName(PROMOTION_AUTHOR.getDisplayName());
            promotionEntityAuthor.setEmail(PROMOTION_AUTHOR.getEmail());
            promotionEntityAuthor.setSource(PROMOTION_AUTHOR.getSource());
            promotionEntityAuthor.setSourceId(PROMOTION_AUTHOR.getSourceId());

            PromotionEntity createdPromotion = new PromotionEntity();
            createdPromotion.setId(PROMOTION.getId());
            createdPromotion.setApiId(PROMOTION.getApiId());
            createdPromotion.setTargetEnvCockpitId(PROMOTION.getTargetEnvCockpitId());
            createdPromotion.setTargetEnvName(PROMOTION.getTargetEnvName());
            createdPromotion.setApiDefinition(PROMOTION.getApiDefinition());
            createdPromotion.setCreatedAt(Date.from(PROMOTION.getCreatedAt()));
            createdPromotion.setUpdatedAt(Date.from(PROMOTION.getUpdatedAt()));
            createdPromotion.setAuthor(promotionEntityAuthor);

            when(promotionService.create(any(), eq(ENVIRONMENT_ID), eq(API_ID), any(PromotionRequestEntity.class), eq(USER_ID))).thenReturn(
                createdPromotion
            );

            var result = service.createPromotion(
                API_ID,
                PromotionRequest.builder().targetEnvName(TARGET_ENV_NAME).targetEnvCockpitId(TARGET_COCKPIT_ENV_ID).build(),
                USER_ID
            );

            assertThat(result)
                .isNotNull()
                .satisfies(promotion -> {
                    assertThat(promotion.getId()).isEqualTo(PROMOTION.getId());
                    assertThat(promotion.getApiId()).isEqualTo(PROMOTION.getApiId());
                    assertThat(promotion.getTargetEnvCockpitId()).isEqualTo(PROMOTION.getTargetEnvCockpitId());
                    assertThat(promotion.getTargetEnvName()).isEqualTo(PROMOTION.getTargetEnvName());
                    assertThat(promotion.getApiDefinition()).isEqualTo(PROMOTION.getApiDefinition());
                    assertThat(promotion.getCreatedAt()).isEqualTo(PROMOTION.getCreatedAt());
                    assertThat(promotion.getUpdatedAt()).isEqualTo(PROMOTION.getUpdatedAt());
                    assertThat(promotion.getAuthor()).isEqualTo(PROMOTION_AUTHOR);
                });
        }
    }
}
