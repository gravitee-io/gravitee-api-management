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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PromotionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PromotionCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProcessPromotionUseCaseTest {

    private static final String API_ID = "api-id";
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final Api API_V2 = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).build();
    private static final Promotion PROMOTION = PromotionFixtures.aPromotion()
        .toBuilder()
        .apiId(API_ID)
        .status(PromotionStatus.TO_BE_VALIDATED)
        .build();
    private static final Environment ENVIRONMENT = Environment.builder()
        .id(ENVIRONMENT_ID)
        .cockpitId(PROMOTION.getTargetEnvCockpitId())
        .organizationId(ORGANIZATION_ID)
        .build();

    private final PromotionCrudServiceInMemory promotionCrudService = new PromotionCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private CockpitPromotionServiceProvider cockpitPromotionServiceProvider;
    private ProcessPromotionUseCase useCase;

    @BeforeEach
    public void setUp() {
        cockpitPromotionServiceProvider = mock(CockpitPromotionServiceProvider.class);
        useCase = new ProcessPromotionUseCase(
            promotionCrudService,
            apiCrudServiceInMemory,
            cockpitPromotionServiceProvider,
            environmentCrudService
        );
    }

    @AfterEach
    public void cleanUp() {
        Stream.of(promotionCrudService, apiCrudServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_process_v2_api_promotion() {
        apiCrudServiceInMemory.initWith(List.of(API_V2));
        promotionCrudService.initWith(List.of(PROMOTION));

        when(cockpitPromotionServiceProvider.process(PROMOTION.getId(), true)).thenReturn(PROMOTION);
        var result = useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION.getId(), true, ORGANIZATION_ID));

        verify(cockpitPromotionServiceProvider).process(eq(PROMOTION.getId()), eq(true));
        assertThat(result.promotion()).isEqualTo(PROMOTION);
    }

    @Test
    void should_throw_exception_when_api_definition_is_not_supported() {
        var federatedApi = ApiFixtures.aFederatedApi().toBuilder().id(API_ID).build();
        apiCrudServiceInMemory.initWith(List.of(federatedApi));
        promotionCrudService.initWith(List.of(PROMOTION));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION.getId(), true, ORGANIZATION_ID))
        );

        assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessage("Only V2 and V4 API definition are supported");
    }

    @Test
    void should_throw_exception_when_v4_api_promotion_is_accepted() {
        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi));
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        Throwable throwable = catchThrowable(() ->
            useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION.getId(), true, ORGANIZATION_ID))
        );

        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("Coming soon - V4 API promotion can only be rejected");
    }

    @Test
    void should_reject_v4_api_promotion() {
        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi));
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        when(cockpitPromotionServiceProvider.requestPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
            CockpitReplyStatus.SUCCEEDED
        );

        var result = useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION.getId(), false, ORGANIZATION_ID));

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isNotNull();
        assertThat(result.promotion().getStatus()).isEqualTo(PromotionStatus.REJECTED);
    }

    @Test
    void should_throw_exception_when_v4_api_promotion_command_fails() {
        var v4proxyApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        apiCrudServiceInMemory.initWith(List.of(v4proxyApi));
        promotionCrudService.initWith(List.of(PROMOTION));
        environmentCrudService.initWith(List.of(ENVIRONMENT));

        when(cockpitPromotionServiceProvider.requestPromotion(eq(ORGANIZATION_ID), eq(ENVIRONMENT_ID), any())).thenReturn(
            CockpitReplyStatus.ERROR
        );

        Throwable throwable = catchThrowable(() ->
            useCase.execute(new ProcessPromotionUseCase.Input(PROMOTION.getId(), false, ORGANIZATION_ID))
        );

        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("An error occurs while sending promotion promotion-id request to cockpit");
    }
}
