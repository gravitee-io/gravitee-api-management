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
package io.gravitee.apim.infra.crud_service.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PromotionCrudServiceImplTest {

    @Mock
    private PromotionRepository repository;

    private PromotionCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromotionCrudServiceImpl(repository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_promotion() {
            when(repository.create(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

            var aPromotion = fixtures.core.model.PromotionFixtures.aPromotion();
            aPromotion.setId("promotion-id");
            var result = service.create(aPromotion);

            assertThat(result).isEqualTo(aPromotion);
        }

        @Test
        @SneakyThrows
        void should_throw_technical_exception_when_create_promotion() {
            when(repository.create(any())).thenThrow(TechnicalException.class);

            Throwable throwable = catchThrowable(() -> service.create(fixtures.core.model.PromotionFixtures.aPromotion()));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred while trying to create a Promotion with id promotion-id");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_promotion() {
            when(repository.update(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

            var updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            var aPromotion = fixtures.core.model.PromotionFixtures.aPromotion();
            aPromotion.setId("promotion-id");
            aPromotion.setUpdatedAt(updatedAt);
            var result = service.update(aPromotion);

            assertThat(result).isEqualTo(aPromotion);
        }

        @Test
        @SneakyThrows
        void should_throw_a_technical_exception_when_updating_promotion() {
            when(repository.update(any())).thenThrow(TechnicalException.class);

            Throwable throwable = catchThrowable(() -> service.update(fixtures.core.model.PromotionFixtures.aPromotion()));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred while trying to update the Promotion with id promotion-id");
        }
    }

    @Nested
    class GetById {

        @Test
        @SneakyThrows
        void should_get_promotion() {
            var id = "promotion-id";
            var aPromotion = fixtures.repository.PromotionFixtures.aPromotion();
            aPromotion.setId(id);

            when(repository.findById(any())).thenReturn(Optional.of(aPromotion));

            var result = service.getById(id);

            assertThat(result)
                .isNotNull()
                .satisfies(promotion -> {
                    assertThat(promotion.getId()).isEqualTo(id);
                    assertThat(promotion.getApiId()).isEqualTo(aPromotion.getApiId());
                    assertThat(promotion.getStatus().name()).isEqualTo(aPromotion.getStatus().name());
                    assertThat(promotion.getSourceEnvCockpitId()).isEqualTo(aPromotion.getSourceEnvCockpitId());
                    assertThat(promotion.getSourceEnvName()).isEqualTo(aPromotion.getSourceEnvName());
                    assertThat(promotion.getTargetEnvCockpitId()).isEqualTo(aPromotion.getTargetEnvCockpitId());
                    assertThat(promotion.getTargetEnvName()).isEqualTo(aPromotion.getTargetEnvName());
                    assertThat(promotion.getCreatedAt()).isEqualTo(aPromotion.getCreatedAt());
                    assertThat(promotion.getUpdatedAt()).isEqualTo(aPromotion.getUpdatedAt());
                    assertThat(promotion.getAuthor()).satisfies(promotionAuthor -> {
                        assertThat(promotionAuthor.getUserId()).isEqualTo(aPromotion.getAuthor().getUserId());
                        assertThat(promotionAuthor.getEmail()).isEqualTo(aPromotion.getAuthor().getEmail());
                        assertThat(promotionAuthor.getDisplayName()).isEqualTo(aPromotion.getAuthor().getDisplayName());
                        assertThat(promotionAuthor.getSource()).isEqualTo(aPromotion.getAuthor().getSource());
                        assertThat(promotionAuthor.getSourceId()).isEqualTo(aPromotion.getAuthor().getSourceId());
                    });
                });
        }

        @Test
        @SneakyThrows
        void should_throw_not_found_exception() {
            var id = "promotion-id";
            when(repository.findById(any())).thenReturn(Optional.empty());

            Throwable throwable = catchThrowable(() -> service.getById(id));

            assertThat(throwable)
                .isInstanceOf(DbEntityNotFoundException.class)
                .hasMessage("Promotion with id promotion-id cannot be found");
        }

        @Test
        @SneakyThrows
        void should_throw_technical_exception() {
            var id = "promotion-id";
            when(repository.findById(any())).thenThrow(new TechnicalException("Unexpected error"));

            Throwable throwable = catchThrowable(() -> service.getById(id));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred while trying to find a Promotion with id promotion-id");
        }
    }
}
