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
package io.gravitee.apim.infra.query_service.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import fixtures.repository.PromotionFixtures;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionAuthor;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.query_service.PromotionQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class PromotionQueryServiceImplTest {

    private static final String API_ID = "api#1";
    private static final String COCKPIT_ID = "cockpit#1";
    PageableImpl pageable = new PageableImpl(1, 5);

    @Mock
    PromotionRepository promotionRepository;

    private PromotionQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromotionQueryServiceImpl(promotionRepository);
    }

    @Nested
    class Search {

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            when(promotionRepository.search(any(), isNull(), isNull())).thenThrow(TechnicalException.class);
            var query = new PromotionQueryService.PromotionQuery(API_ID, Set.of(), Set.of(), null);

            // When
            Throwable throwable = catchThrowable(() -> service.search(query));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_no_report() {
            when(promotionRepository.search(any(), isNull(), isNull())).thenReturn(new Page<>(List.of(), 0, 0, 0));
            var query = new PromotionQueryService.PromotionQuery(API_ID, Set.of(), Set.of(), null);

            var result = service.search(query);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_find_promotion_by_target_api() {
            var query = new PromotionQueryService.PromotionQuery(API_ID, Set.of(), Set.of(), null);

            var savedPromotion = PromotionFixtures.aPromotion();
            savedPromotion.setApiId(API_ID);

            var page = new Page<>(List.of(savedPromotion), pageable.getPageNumber(), 1, 1);
            when(
                promotionRepository.search(argThat(criteria -> criteria != null && API_ID.equals(criteria.getApiId())), isNull(), isNull())
            ).thenReturn(page);

            var result = service.search(query);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent()).contains(
                Promotion.builder()
                    .id("promotion-id")
                    .apiDefinition("api-definition")
                    .apiId(API_ID)
                    .status(PromotionStatus.CREATED)
                    .targetEnvCockpitId("target-env-cockpit-id")
                    .targetEnvName("target-env-name")
                    .sourceEnvCockpitId("source-env-cockpit-id")
                    .sourceEnvName("source-env-name")
                    .author(
                        PromotionAuthor.builder()
                            .displayName("John Smith")
                            .userId("user-id")
                            .source("source")
                            .sourceId("source-id")
                            .email("foo@example.com")
                            .build()
                    )
                    .targetApiId("target-api-id")
                    .build()
            );
        }

        @Test
        @SneakyThrows
        void should_find_promotion_by_target_statuses() {
            var query = new PromotionQueryService.PromotionQuery(
                null,
                Set.of(),
                Set.of(PromotionStatus.ACCEPTED, PromotionStatus.REJECTED),
                null
            );

            var savedPromotion = PromotionFixtures.aPromotion();
            savedPromotion.setApiId(API_ID);

            var page = new Page<>(List.of(savedPromotion), pageable.getPageNumber(), 1, 1);
            when(
                promotionRepository.search(
                    argThat(
                        criteria ->
                            criteria != null &&
                            criteria
                                .getStatuses()
                                .containsAll(
                                    List.of(
                                        io.gravitee.repository.management.model.PromotionStatus.ACCEPTED,
                                        io.gravitee.repository.management.model.PromotionStatus.REJECTED
                                    )
                                )
                    ),
                    isNull(),
                    isNull()
                )
            ).thenReturn(page);

            var result = service.search(query);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_find_promotion_by_target_cockpit_ids() {
            var query = new PromotionQueryService.PromotionQuery(null, Set.of(COCKPIT_ID), Set.of(), null);

            var savedPromotion = PromotionFixtures.aPromotion();
            savedPromotion.setApiId(API_ID);

            var page = new Page<>(List.of(savedPromotion), pageable.getPageNumber(), 1, 1);
            when(
                promotionRepository.search(
                    argThat(criteria -> criteria != null && criteria.getTargetEnvCockpitIds().contains(COCKPIT_ID)),
                    isNull(),
                    isNull()
                )
            ).thenReturn(page);

            var result = service.search(query);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_find_promotion_by_target_api_exists() {
            var query = new PromotionQueryService.PromotionQuery(API_ID, Set.of(), Set.of(), true);

            var savedPromotion = PromotionFixtures.aPromotion();
            savedPromotion.setApiId(API_ID);

            var page = new Page<>(List.of(savedPromotion), pageable.getPageNumber(), 1, 1);
            when(
                promotionRepository.search(
                    argThat(
                        criteria -> criteria != null && criteria.getApiId().equals(API_ID) && criteria.getTargetApiExists().equals(true)
                    ),
                    isNull(),
                    isNull()
                )
            ).thenReturn(page);

            var result = service.search(query);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
