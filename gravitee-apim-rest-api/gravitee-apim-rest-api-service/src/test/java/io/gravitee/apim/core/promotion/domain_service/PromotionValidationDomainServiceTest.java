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
package io.gravitee.apim.core.promotion.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.PromotionFixtures;
import inmemory.PromotionQueryServiceInMemory;
import io.gravitee.apim.core.promotion.exception.PromotionAlreadyInProgressException;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PromotionValidationDomainServiceTest {

    private static final String API_ID = "api-id";
    private static final String TARGET_COCKPIT_ENV_ID = "cockpit-env-id";
    private static final Promotion.PromotionBuilder PROMOTION = PromotionFixtures.aPromotion()
        .toBuilder()
        .apiId(API_ID)
        .targetEnvCockpitId(TARGET_COCKPIT_ENV_ID)
        .status(PromotionStatus.CREATED);

    private PromotionValidationDomainService service;

    private PromotionQueryServiceInMemory promotionQueryService;

    @BeforeEach
    void setUp() {
        promotionQueryService = new PromotionQueryServiceInMemory();
        service = new PromotionValidationDomainService(promotionQueryService);
    }

    @AfterEach
    void tearDown() {
        promotionQueryService.reset();
    }

    @ParameterizedTest
    @EnumSource(value = PromotionStatus.class, names = { "CREATED", "TO_BE_VALIDATED" })
    @SneakyThrows
    void should_throw_promotion_already_in_progress_exception(PromotionStatus status) {
        promotionQueryService.initWith(List.of(PROMOTION.status(status).build()));

        Throwable throwable = catchThrowable(() -> service.validate(API_ID, TARGET_COCKPIT_ENV_ID));

        assertThat(throwable).isInstanceOf(PromotionAlreadyInProgressException.class);
    }

    @ParameterizedTest
    @EnumSource(value = PromotionStatus.class, names = { "ACCEPTED", "REJECTED", "ERROR" })
    @SneakyThrows
    void should_not_throw_exception_for_status(PromotionStatus status) {
        promotionQueryService.initWith(List.of(PROMOTION.status(status).build()));

        Throwable throwable = catchThrowable(() -> service.validate(API_ID, TARGET_COCKPIT_ENV_ID));

        assertThat(throwable).isNull();
    }

    @Test
    void should_throw_exception_for_another_api() {
        promotionQueryService.initWith(List.of(PROMOTION.build()));

        Throwable throwable = catchThrowable(() -> service.validate("another-api", TARGET_COCKPIT_ENV_ID));

        assertThat(throwable).isNull();
    }

    @Test
    void should_throw_exception_for_another_cockpit_env() {
        promotionQueryService.initWith(List.of(PROMOTION.build()));

        Throwable throwable = catchThrowable(() -> service.validate(API_ID, "another-cockpit-env-id"));

        assertThat(throwable).isNull();
    }
}
