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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.promotion.domain_service.PromotionContextDomainService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
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
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private final PromotionContextDomainService service = new PromotionContextDomainService(
        promotionCrudService,
        apiCrudService,
        apiQueryServiceInMemory,
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
        Stream.of(promotionCrudService, apiCrudService, apiQueryServiceInMemory, environmentCrudService).forEach(
            InMemoryAlternative::reset
        );
    }

    @Test
    @SneakyThrows
    void should_find_promotion_with_api() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/export_proxy.json"), StandardCharsets.UTF_8))
            .build();
        promotionCrudService.initWith(List.of(promotion));

        Api api = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).environmentId(DEFAULT_ENV_ID).build();
        apiCrudService.initWith(List.of(api));
        apiQueryServiceInMemory.initWith(List.of(api));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isEqualTo(promotion);
        assertThat(result.expectedDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(result.existingPromotedApi()).isNull();
        assertThat(result.targetEnvId()).isEqualTo(TARGET_ENV_ID);
    }

    @Test
    @SneakyThrows
    void should_find_promotion_with_api_v2() {
        Api v2Api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).environmentId(DEFAULT_ENV_ID).build();
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/legacy-v2-export.json"), StandardCharsets.UTF_8))
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiCrudService.initWith(List.of(v2Api));
        apiQueryServiceInMemory.initWith(List.of(v2Api));

        var result = service.getPromotionContext(promotion.getId(), true);

        assertThat(result).isNotNull();
        assertThat(result.promotion()).isEqualTo(promotion);
        assertThat(result.expectedDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
    }

    @Test
    void should_throw_exception_definition_version_not_exist() {
        Api v2Api = ApiFixtures.aFederatedApi().toBuilder().id(API_ID).environmentId(DEFAULT_ENV_ID).build();
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition(
                """
                    {
                        "api": {
                            "definitionVersion": "yolo"
                        }
                    }
                """
            )
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiCrudService.initWith(List.of(v2Api));
        apiQueryServiceInMemory.initWith(List.of(v2Api));

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        assertThat(throwable).hasMessage("An error occurred while try to parse promotion definition version promotion-id");
    }

    @Test
    void should_throw_exception_when_definition_version_not_found() {
        Api v2Api = ApiFixtures.aFederatedApi().toBuilder().id(API_ID).environmentId(DEFAULT_ENV_ID).build();
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .apiDefinition("{}")
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiCrudService.initWith(List.of(v2Api));
        apiQueryServiceInMemory.initWith(List.of(v2Api));

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(throwable).hasMessage("Could not determine definition version for promotion promotion-id");
    }

    @Test
    @SneakyThrows
    void should_throw_exception_when_target_api_is_migrated() {
        Api sourceV2Api = ApiFixtures.aProxyApiV2().toBuilder().id(API_ID).crossId(CROSS_ID).environmentId(DEFAULT_ENV_ID).build();
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/legacy-v2-export.json"), StandardCharsets.UTF_8))
            .targetEnvCockpitId(TARGET_ENV_COCKPIT_ID)
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiCrudService.initWith(List.of(sourceV2Api));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV4().toBuilder().id("target-v4-api").crossId(CROSS_ID).environmentId(TARGET_ENV_ID).build())
        );

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(throwable).hasMessage(
            "An API with the same crossId already exists with a different definition version in the target environment"
        );
    }

    @Test
    @SneakyThrows
    void should_throw_exception_when_source_api_is_migrated() {
        Api sourceV4Api = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).environmentId("DEFAULT").build();
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/export_proxy.json"), StandardCharsets.UTF_8))
            .targetEnvCockpitId("TARGET-ENV")
            .build();
        promotionCrudService.initWith(List.of(promotion));
        apiCrudService.initWith(List.of(sourceV4Api));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV2().toBuilder().id("target-v2-api").crossId(CROSS_ID).environmentId("TARGET-ENV-ID").build())
        );
        environmentCrudService.initWith(
            List.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("TARGET-ENV-ID").cockpitId("TARGET-ENV").build())
        );

        Throwable throwable = catchThrowable(() -> service.getPromotionContext(promotion.getId(), true));
        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(throwable).hasMessage(
            "An API with the same crossId already exists with a different definition version in the target environment"
        );
    }

    @Test
    @SneakyThrows
    void should_allow_promotion_rejection_when_versions_missmatch() {
        Promotion promotion = Promotion.builder()
            .id(PROMOTION_ID)
            .apiId(API_ID)
            .apiDefinition(IOUtils.toString(new FileInputStream("src/test/resources/export/export_proxy.json"), StandardCharsets.UTF_8))
            .targetEnvCockpitId("TARGET-ENV")
            .build();

        promotionCrudService.initWith(List.of(promotion));
        apiQueryServiceInMemory.initWith(
            List.of(ApiFixtures.aProxyApiV2().toBuilder().id("target-v2-api").crossId(CROSS_ID).environmentId("TARGET-ENV-ID").build())
        );
        apiCrudService.initWith(
            List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).crossId(CROSS_ID).environmentId("DEFAULT").build())
        );
        environmentCrudService.initWith(
            List.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("TARGET-ENV-ID").cockpitId("TARGET-ENV").build())
        );

        assertThat(service.getPromotionContext(promotion.getId(), false)).isNotNull();
    }
}
