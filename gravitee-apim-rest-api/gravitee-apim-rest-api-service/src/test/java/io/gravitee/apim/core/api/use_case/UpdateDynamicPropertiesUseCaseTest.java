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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.definition.ApiDefinitionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiEventQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.infra.domain_service.api.CategoryDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateDynamicPropertiesUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String HTTP_DYNAMIC_PROPERTIES = "http-dynamic-properties";
    private static final String USER = HTTP_DYNAMIC_PROPERTIES + "-management-api-service";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";

    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final EnvironmentCrudServiceInMemory environmentCrudServiceInMemory = new EnvironmentCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();
    private final ApiEventQueryServiceInMemory apiEventQueryServiceInMemory = new ApiEventQueryServiceInMemory();
    CategoryMapper categoryMapper = mock(CategoryMapper.class);
    ApiCategoryOrderRepository apiCategoryOrderRepository = mock(ApiCategoryOrderRepository.class);
    CategoryDomainService categoryDomainService = new CategoryDomainServiceImpl(categoryMapper, apiCategoryOrderRepository);

    private ApiStateDomainService apiStateDomainService;

    private UpdateDynamicPropertiesUseCase cut;

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
        apiStateDomainService = mock(ApiStateDomainService.class);
        environmentCrudServiceInMemory.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build()));
        cut =
            new UpdateDynamicPropertiesUseCase(
                apiCrudServiceInMemory,
                apiStateDomainService,
                environmentCrudServiceInMemory,
                new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor()),
                apiEventQueryServiceInMemory,
                categoryDomainService
            );
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudServiceInMemory,
                environmentCrudServiceInMemory,
                auditCrudServiceInMemory,
                userCrudServiceInMemory,
                apiEventQueryServiceInMemory
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_not_update_api_if_same_properties() {
        var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("key").value("value").dynamic(true).build())));

        cut.execute(
            new UpdateDynamicPropertiesUseCase.Input(
                api.getId(),
                HTTP_DYNAMIC_PROPERTIES,
                List.of(Property.builder().key("key").value("value").dynamic(true).build())
            )
        );

        assertThat(auditCrudServiceInMemory.storage()).isEmpty();
        verifyNoInteractions(apiStateDomainService);
    }

    @Nested
    class WhenApiIsSynchronized {

        @BeforeEach
        void setUp() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(true);
        }

        @Test
        void should_update_api_with_new_properties() {
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build())));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("key").value("value").dynamic(true).build())
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties())
                .containsExactlyInAnyOrder(
                    Property.builder().key("user-prop").value("value").dynamic(false).build(),
                    Property.builder().key("key").value("value").dynamic(true).build()
                );
            assertAuditHasBeenCreated();
        }

        @Test
        void should_ignore_dynamic_properties_having_same_key_than_static_properties_added_by_user() {
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build())));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(
                        Property.builder().key("key").value("value").dynamic(true).build(),
                        // trying to set the same property as the user
                        Property.builder().key("user-prop").value("other-value").dynamic(true).build()
                    )
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties())
                .containsExactlyInAnyOrder(
                    Property.builder().key("user-prop").value("value").dynamic(false).build(),
                    Property.builder().key("key").value("value").dynamic(true).build()
                );
            assertAuditHasBeenCreated();
        }

        @Test
        void should_not_deploy() {
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build())));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("key").value("value").dynamic(true).build())
                )
            );

            verify(apiStateDomainService, never()).deploy(any(), any(), any());
        }
    }

    @Nested
    class WhenApiIsNotSynchronized {

        private final ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
        private final ArgumentCaptor<AuditInfo> auditInfoCaptor = ArgumentCaptor.forClass(AuditInfo.class);

        @BeforeEach
        void setUp() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(false);
        }

        @Test
        void should_update_api_with_new_properties() {
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build())));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(
                        Property.builder().key("key").value("value").dynamic(true).build(),
                        // trying to set the same property as the user
                        Property.builder().key("user-prop").value("other-value").dynamic(true).build()
                    )
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties())
                .containsExactlyInAnyOrder(
                    Property.builder().key("user-prop").value("value").dynamic(false).build(),
                    Property.builder().key("key").value("value").dynamic(true).build()
                );
            assertAuditHasBeenCreated();
        }

        @Test
        void should_redeploy_api() {
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build())));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(
                        Property.builder().key("key").value("value").dynamic(true).build(),
                        // trying to set the same property as the user
                        Property.builder().key("user-prop").value("other-value").dynamic(true).build()
                    )
                )
            );

            verify(apiStateDomainService).deploy(apiCaptor.capture(), any(String.class), auditInfoCaptor.capture());
            assertSoftly(softly -> {
                softly
                    .assertThat(apiCaptor.getValue().getApiDefinitionHttpV4().getProperties())
                    .containsExactlyInAnyOrder(
                        Property.builder().key("user-prop").value("value").dynamic(false).build(),
                        Property.builder().key("key").value("value").dynamic(true).build()
                    );
                softly
                    .assertThat(auditInfoCaptor.getValue())
                    .isEqualTo(
                        AuditInfo
                            .builder()
                            .organizationId(ORGANIZATION_ID)
                            .environmentId(ENVIRONMENT_ID)
                            .actor(AuditActor.builder().userId(USER).build())
                            .build()
                    );
            });
        }

        @Test
        void should_redeploy_using_the_last_deployed_api_definition() {
            // Case were a user disable and save the API, but without deploying it
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build())));
            api.getApiDefinitionHttpV4().getServices().getDynamicProperty().setEnabled(false);

            // Last event of the deployed api is with the service enabled
            final Api lastDeployedApi = api.toBuilder().build();
            lastDeployedApi.getApiDefinitionHttpV4().getServices().getDynamicProperty().setEnabled(true);
            apiEventQueryServiceInMemory.initWith(List.of(lastDeployedApi));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(
                        Property.builder().key("key").value("value").dynamic(true).build(),
                        // trying to set the same property as the user
                        Property.builder().key("user-prop").value("other-value").dynamic(true).build()
                    )
                )
            );

            verify(apiStateDomainService).deploy(apiCaptor.capture(), any(String.class), auditInfoCaptor.capture());
            assertSoftly(softly -> {
                var definition = api.getApiDefinitionHttpV4();
                softly.assertThat(definition.getServices().getDynamicProperty().isEnabled()).isTrue();
                softly
                    .assertThat(definition.getProperties())
                    .containsExactlyInAnyOrder(
                        Property.builder().key("user-prop").value("value").dynamic(false).build(),
                        Property.builder().key("key").value("value").dynamic(true).build()
                    );
                softly
                    .assertThat(auditInfoCaptor.getValue())
                    .isEqualTo(
                        AuditInfo
                            .builder()
                            .organizationId(ORGANIZATION_ID)
                            .environmentId(ENVIRONMENT_ID)
                            .actor(AuditActor.builder().userId(USER).build())
                            .build()
                    );
            });
        }
    }

    private Api givenApi(Api api) {
        apiCrudServiceInMemory.initWith(List.of(api));
        apiEventQueryServiceInMemory.initWith(List.of(api.toBuilder().build()));
        return api;
    }

    @NotNull
    private static Api buildApiWithProperties(List<Property> properties) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).apiDefinitionHttpV4(anApiDefinitionWithProperties(properties)).build();
    }

    private static io.gravitee.definition.model.v4.Api anApiDefinitionWithProperties(List<Property> properties) {
        return ApiDefinitionFixtures
            .anApiV4()
            .toBuilder()
            .services(new ApiServices(Service.builder().type(HTTP_DYNAMIC_PROPERTIES).enabled(true).build()))
            .properties(properties)
            .build();
    }

    private void assertAuditHasBeenCreated() {
        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    API_ID,
                    USER,
                    Map.of("API", API_ID),
                    ApiAuditEvent.API_UPDATED.name(),
                    INSTANT_NOW.atZone(ZoneId.systemDefault()),
                    ""
                )
            );
    }
}
