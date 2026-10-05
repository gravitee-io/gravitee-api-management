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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
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
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.infra.domain_service.api.CategoryDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

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
    private static final DataEncryptor DATA_ENCRYPTOR = new DataEncryptor(
        new MockEnvironment(),
        "api.properties.encryption.secret",
        "vvLJ4Q8Khvv9tm2tIPdkGEdmgKUruAL6"
    );

    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final EnvironmentCrudServiceInMemory environmentCrudServiceInMemory = new EnvironmentCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();
    private final ApiEventQueryServiceInMemory apiEventQueryServiceInMemory = new ApiEventQueryServiceInMemory();
    CategoryMapper categoryMapper = mock(CategoryMapper.class);
    ApiCategoryOrderRepository apiCategoryOrderRepository = mock(ApiCategoryOrderRepository.class);
    CategoryDomainService categoryDomainService = new CategoryDomainServiceImpl(categoryMapper, apiCategoryOrderRepository);

    private ApiStateDomainService apiStateDomainService;
    private AuditDomainService auditDomainService;

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
        auditDomainService = spy(new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor()));
        environmentCrudServiceInMemory.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build()));
        cut = new UpdateDynamicPropertiesUseCase(
            apiCrudServiceInMemory,
            apiStateDomainService,
            environmentCrudServiceInMemory,
            auditDomainService,
            apiEventQueryServiceInMemory,
            categoryDomainService,
            new PropertyDomainService(DATA_ENCRYPTOR)
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            apiCrudServiceInMemory,
            environmentCrudServiceInMemory,
            auditCrudServiceInMemory,
            userCrudServiceInMemory,
            apiEventQueryServiceInMemory
        ).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_not_update_api_if_same_properties() {
        List<Property> initialPropertiesList = List.of(Property.builder().key("key").value("value").dynamic(true).build());
        var api = givenApi(buildApiWithProperties(initialPropertiesList));

        cut.execute(new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, initialPropertiesList, false));

        assertThat(auditCrudServiceInMemory.storage()).isEmpty();
    }

    @Test
    void should_persist_the_removal_of_a_property_missing_from_the_source_even_if_nothing_else_changed() {
        var kept = Property.builder().key("kept").value("value").dynamic(true).build();
        var removed = Property.builder().key("removed").value("value").dynamic(true).build();
        var api = givenApi(buildApiWithProperties(List.of(kept, removed)));

        cut.execute(new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(kept), false));

        assertAuditHasBeenCreated();
        assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactly(kept);
    }

    @Test
    void should_persist_the_removal_of_the_last_dynamic_property_and_keep_user_defined_ones() {
        var userDefined = Property.builder().key("user-prop").value("value").dynamic(false).build();
        var removed = Property.builder().key("removed").value("value").dynamic(true).build();
        var api = givenApi(buildApiWithProperties(List.of(userDefined, removed)));

        cut.execute(new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(), false));

        assertAuditHasBeenCreated();
        assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactly(userDefined);
    }

    @Test
    void should_determine_sync_state_before_updating_api_properties() {
        List<Property> initialPropertiesList = List.of(Property.builder().key("key").value("value").dynamic(true).build());
        var api = givenApi(buildApiWithProperties(initialPropertiesList));

        var receivedProperties = new ArrayList<>();
        when(apiStateDomainService.isSynchronized(any(), any())).thenAnswer(invocationOnMock -> {
            receivedProperties.addAll(((Api) invocationOnMock.getArgument(0)).getApiDefinitionHttpV4().getProperties().stream().toList());
            return true;
        });

        cut.execute(
            new UpdateDynamicPropertiesUseCase.Input(
                api.getId(),
                HTTP_DYNAMIC_PROPERTIES,
                List.of(Property.builder().key("key").value("value2").dynamic(true).build()),
                false
            )
        );

        assertThat(receivedProperties).isEqualTo(initialPropertiesList);
    }

    @Nested
    class WhenApiIsNotSynchronized {

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
                    List.of(Property.builder().key("key").value("value").dynamic(true).build()),
                    false
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactlyInAnyOrder(
                Property.builder().key("user-prop").value("value").dynamic(false).build(),
                Property.builder().key("key").value("value").dynamic(true).build()
            );
            assertAuditHasBeenCreated();
            assertThat(auditPatch()).contains("/apiDefinitionValue/properties").contains("\"value\":\"key\"");
        }

        @Test
        void should_persist_and_audit_a_sync_that_only_removes_a_dynamic_property() {
            var api = givenApi(
                buildApiWithProperties(
                    List.of(
                        Property.builder().key("user-prop").value("value").dynamic(false).build(),
                        Property.builder().key("gone").value("value").dynamic(true).build()
                    )
                )
            );

            cut.execute(new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of()));

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactly(
                Property.builder().key("user-prop").value("value").dynamic(false).build()
            );
            assertAuditHasBeenCreated();
            assertThat(auditPatch()).contains("{\"op\":\"remove\",\"path\":\"/apiDefinitionValue/properties/");
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
                    ),
                    false
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactlyInAnyOrder(
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
                    List.of(Property.builder().key("key").value("value").dynamic(true).build()),
                    false
                )
            );

            verify(apiStateDomainService, never()).deploy(any(), any(), any());
        }
    }

    @Nested
    class WhenApiIsSynchronized {

        private final ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);
        private final ArgumentCaptor<AuditInfo> auditInfoCaptor = ArgumentCaptor.forClass(AuditInfo.class);

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
                    List.of(
                        Property.builder().key("key").value("value").dynamic(true).build(),
                        // trying to set the same property as the user
                        Property.builder().key("user-prop").value("other-value").dynamic(true).build()
                    ),
                    false
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactlyInAnyOrder(
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
                    ),
                    false
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
                        AuditInfo.builder()
                            .organizationId(ORGANIZATION_ID)
                            .environmentId(ENVIRONMENT_ID)
                            .actor(AuditActor.builder().userId(USER).build())
                            .build()
                    );
            });
        }

        @Test
        void should_redeploy_api_when_a_dynamic_property_is_removed() {
            var userDefined = Property.builder().key("user-prop").value("value").dynamic(false).build();
            var removed = Property.builder().key("removed").value("value").dynamic(true).build();
            var api = givenApi(buildApiWithProperties(List.of(userDefined, removed)));

            cut.execute(new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(), false));

            verify(apiStateDomainService).deploy(apiCaptor.capture(), any(String.class), auditInfoCaptor.capture());
            assertThat(apiCaptor.getValue().getApiDefinitionHttpV4().getProperties()).containsExactly(userDefined);
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
                    ),
                    false
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
                        AuditInfo.builder()
                            .organizationId(ORGANIZATION_ID)
                            .environmentId(ENVIRONMENT_ID)
                            .actor(AuditActor.builder().userId(USER).build())
                            .build()
                    );
            });
        }
    }

    @Nested
    class WhenApiIsNative {

        private final ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);

        @Test
        void should_update_native_api_with_new_properties() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(false);
            var api = givenApi(
                buildNativeApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build()))
            );

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("key").value("value").dynamic(true).build()),
                    false
                )
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionNativeV4().getProperties()).containsExactlyInAnyOrder(
                Property.builder().key("user-prop").value("value").dynamic(false).build(),
                Property.builder().key("key").value("value").dynamic(true).build()
            );
            assertAuditHasBeenCreated();
            assertThat(auditPatch()).contains("/apiDefinitionValue/properties").contains("\"value\":\"key\"");
        }

        @Test
        void should_persist_and_audit_a_native_sync_that_only_removes_a_dynamic_property() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(false);
            var api = givenApi(
                buildNativeApiWithProperties(
                    List.of(
                        Property.builder().key("user-prop").value("value").dynamic(false).build(),
                        Property.builder().key("gone").value("value").dynamic(true).build()
                    )
                )
            );

            cut.execute(new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of()));

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionNativeV4().getProperties()).containsExactly(
                Property.builder().key("user-prop").value("value").dynamic(false).build()
            );
            assertAuditHasBeenCreated();
            assertThat(auditPatch()).contains("{\"op\":\"remove\",\"path\":\"/apiDefinitionValue/properties/");
        }

        @Test
        void should_redeploy_native_api_using_the_last_deployed_api_definition() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(true);
            // Case were a user disable and save the API, but without deploying it
            var api = givenApi(
                buildNativeApiWithProperties(List.of(Property.builder().key("user-prop").value("value").dynamic(false).build()))
            );
            api.getApiDefinitionNativeV4().getServices().getDynamicProperty().setEnabled(false);

            // Last event of the deployed api carries its own, still enabled, service instance. It has to be a distinct
            // instance: toBuilder() copies the definition by reference, so mutating it would mutate the saved api too.
            final Service deployedService = Service.builder().type(HTTP_DYNAMIC_PROPERTIES).enabled(true).build();
            final Api lastDeployedApi = api
                .toBuilder()
                .apiDefinitionNativeV4(
                    ApiDefinitionFixtures.aNativeApiV4().toBuilder().services(new NativeApiServices(deployedService)).build()
                )
                .build();
            apiEventQueryServiceInMemory.initWith(List.of(lastDeployedApi));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("key").value("value").dynamic(true).build()),
                    false
                )
            );

            verify(apiStateDomainService).deploy(apiCaptor.capture(), any(String.class), any());
            assertSoftly(softly -> {
                var definition = apiCaptor.getValue().getApiDefinitionNativeV4();
                // the deployed service instance itself must be carried over, not just an enabled flag
                softly.assertThat(definition.getServices().getDynamicProperty()).isSameAs(deployedService);
                softly
                    .assertThat(definition.getProperties())
                    .containsExactlyInAnyOrder(
                        Property.builder().key("user-prop").value("value").dynamic(false).build(),
                        Property.builder().key("key").value("value").dynamic(true).build()
                    );
            });
        }
    }

    private Api givenApi(Api api) {
        apiCrudServiceInMemory.initWith(List.of(api));
        apiEventQueryServiceInMemory.initWith(List.of(api.toBuilder().build()));
        return api;
    }

    private static Api buildApiWithProperties(List<Property> properties) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).apiDefinitionHttpV4(anApiDefinitionWithProperties(properties)).build();
    }

    private static Api buildNativeApiWithProperties(List<Property> properties) {
        return ApiFixtures.aNativeApi()
            .toBuilder()
            .id(API_ID)
            .apiDefinitionNativeV4(aNativeApiDefinitionWithProperties(properties))
            .build();
    }

    private static NativeApi aNativeApiDefinitionWithProperties(List<Property> properties) {
        return ApiDefinitionFixtures.aNativeApiV4()
            .toBuilder()
            .services(new NativeApiServices(Service.builder().type(HTTP_DYNAMIC_PROPERTIES).enabled(true).build()))
            .properties(properties)
            .build();
    }

    private static io.gravitee.definition.model.v4.Api anApiDefinitionWithProperties(List<Property> properties) {
        return ApiDefinitionFixtures.anApiV4()
            .toBuilder()
            .services(new ApiServices(Service.builder().type(HTTP_DYNAMIC_PROPERTIES).enabled(true).build()))
            .properties(properties)
            .build();
    }

    private String auditPatch() {
        assertThat(auditCrudServiceInMemory.storage()).hasSize(1);
        return auditCrudServiceInMemory.storage().getFirst().getPatch();
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

    @Nested
    class WithEncryptedDynamicProperties {

        private final ArgumentCaptor<Api> apiCaptor = ArgumentCaptor.forClass(Api.class);

        @BeforeEach
        void setUp() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(true);
        }

        @Test
        void should_keep_encrypted_property_when_value_unchanged() throws GeneralSecurityException {
            var stored = encryptedDynamic("secret", "s3cret");
            var api = givenApi(buildApiWithProperties(List.of(stored)));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(fetched("secret", "s3cret")), false)
            );

            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactly(stored);
            assertThat(auditCrudServiceInMemory.storage()).isEmpty();
            verify(apiStateDomainService, never()).deploy(any(), any(), any());
        }

        @Test
        void should_store_a_changed_value_encrypted_and_redeploy_ciphertext_only() throws GeneralSecurityException {
            var api = givenApi(buildApiWithProperties(List.of(encryptedDynamic("secret", "s3cret"))));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(fetched("secret", "n3w")), false)
            );

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties().getFirst();
            assertThat(persisted.isEncrypted()).isTrue();
            assertThat(persisted.isDynamic()).isTrue();
            assertThat(DATA_ENCRYPTOR.decrypt(persisted.getValue())).isEqualTo("n3w");

            verify(apiStateDomainService).deploy(apiCaptor.capture(), any(String.class), any());
            assertThat(apiCaptor.getValue().getApiDefinitionHttpV4().getProperties()).containsExactly(persisted);
            var auditCaptor = ArgumentCaptor.forClass(ApiAuditLogEntity.class);
            verify(auditDomainService).createApiAuditLog(auditCaptor.capture());
            assertThat(((Api) auditCaptor.getValue().newValue()).getApiDefinitionHttpV4().getProperties()).containsExactly(persisted);
        }

        @Test
        void should_purge_an_encrypted_property_missing_from_the_source_and_bring_it_back_plain() throws GeneralSecurityException {
            var api = givenApi(buildApiWithProperties(List.of(encryptedDynamic("secret", "s3cret"), fetched("other", "v1"))));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(fetched("other", "v2")), false)
            );
            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactly(
                fetched("other", "v2")
            );

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(fetched("other", "v2"), fetched("secret", "s3cret")),
                    false
                )
            );
            assertThat(apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties()).containsExactly(
                fetched("other", "v2"),
                fetched("secret", "s3cret")
            );
        }

        @Test
        void should_re_encrypt_a_stored_property_whose_value_cannot_be_decrypted() throws GeneralSecurityException {
            var corrupted = Property.builder().key("secret").value("not-a-ciphertext!").encrypted(true).dynamic(true).build();
            var api = givenApi(buildApiWithProperties(List.of(corrupted, fetched("other", "v1"))));
            var input = new UpdateDynamicPropertiesUseCase.Input(
                api.getId(),
                HTTP_DYNAMIC_PROPERTIES,
                List.of(fetched("secret", "n3w"), fetched("other", "v2")),
                false
            );

            cut.execute(input);

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties();
            var secret = persisted
                .stream()
                .filter(property -> property.getKey().equals("secret"))
                .findFirst()
                .orElseThrow();
            assertThat(secret.isEncrypted()).isTrue();
            assertThat(secret.isDynamic()).isTrue();
            assertThat(DATA_ENCRYPTOR.decrypt(secret.getValue())).isEqualTo("n3w");
            assertThat(persisted).contains(fetched("other", "v2"));

            auditCrudServiceInMemory.reset();
            cut.execute(input);

            assertThat(auditCrudServiceInMemory.storage()).isEmpty();
        }

        @Test
        void should_audit_only_the_ciphertext_of_a_changed_encrypted_value() throws GeneralSecurityException {
            var api = givenApi(buildApiWithProperties(List.of(encryptedDynamic("secret", "s3cret"))));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(fetched("secret", "n3w")), false)
            );

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties().getFirst();
            var audit = auditCrudServiceInMemory.storage().getFirst();
            assertThat(audit.getPatch()).contains(persisted.getValue()).doesNotContain("n3w").doesNotContain("s3cret");
            assertThat(audit.getProperties()).containsEntry("ENCRYPTED", "true");
        }

        @Test
        void should_audit_only_the_ciphertext_of_a_changed_encrypted_value_on_a_native_api() throws GeneralSecurityException {
            var api = givenApi(buildNativeApiWithProperties(List.of(encryptedDynamic("secret", "s3cret"))));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(api.getId(), HTTP_DYNAMIC_PROPERTIES, List.of(fetched("secret", "n3w")), false)
            );

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionNativeV4().getProperties().getFirst();
            var audit = auditCrudServiceInMemory.storage().getFirst();
            assertThat(audit.getPatch()).contains(persisted.getValue()).doesNotContain("n3w").doesNotContain("s3cret");
            assertThat(audit.getProperties()).containsEntry("ENCRYPTED", "true");
        }

        @Test
        void should_not_log_the_fetched_value_when_it_cannot_be_encrypted() {
            Appender<ILoggingEvent> appender = mock(Appender.class);
            Logger logger = (Logger) LoggerFactory.getLogger(PropertyDomainService.class);
            logger.addAppender(appender);
            try {
                var corrupted = Property.builder().key("secret").value("not-a-ciphertext!").encrypted(true).dynamic(true).build();
                var api = givenApi(buildApiWithProperties(List.of(corrupted, fetched("other", "v1"))));

                cut.execute(
                    new UpdateDynamicPropertiesUseCase.Input(
                        api.getId(),
                        HTTP_DYNAMIC_PROPERTIES,
                        List.of(fetched("secret", "super-secret-value"), fetched("other", "v2")),
                        false
                    )
                );

                verify(appender, atLeastOnce()).doAppend(any());
                verify(appender, never()).doAppend(argThat(event -> event.getFormattedMessage().contains("super-secret-value")));
                assertThat(auditPatch()).doesNotContain("super-secret-value");
            } finally {
                logger.detachAppender(appender);
            }
        }

        private static Property encryptedDynamic(String key, String plaintext) throws GeneralSecurityException {
            return Property.builder().key(key).value(DATA_ENCRYPTOR.encrypt(plaintext)).encrypted(true).dynamic(true).build();
        }

        private static Property fetched(String key, String value) {
            return Property.builder().key(key).value(value).dynamic(true).build();
        }
    }

    @Nested
    class WithEncryptOnFetchEnabled {

        @BeforeEach
        void setUp() {
            when(apiStateDomainService.isSynchronized(any(), any())).thenReturn(true);
        }

        @Test
        void should_encrypt_a_newly_fetched_property_when_toggle_is_on() throws GeneralSecurityException {
            var api = givenApi(buildApiWithProperties(List.of()));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build()),
                    true
                )
            );

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties().getFirst();
            assertThat(persisted.isEncrypted()).isTrue();
            assertThat(DATA_ENCRYPTOR.decrypt(persisted.getValue())).isEqualTo("s3cret");
        }

        @Test
        void should_encrypt_an_already_stored_unencrypted_property_on_its_next_fetch() throws GeneralSecurityException {
            var api = givenApi(buildApiWithProperties(List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build())));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build()),
                    true
                )
            );

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties().getFirst();
            assertThat(persisted.isEncrypted()).isTrue();
            assertThat(DATA_ENCRYPTOR.decrypt(persisted.getValue())).isEqualTo("s3cret");
        }

        @Test
        void should_not_encrypt_a_fetched_property_when_toggle_is_off() {
            var api = givenApi(buildApiWithProperties(List.of()));

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build()),
                    false
                )
            );

            var persisted = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties().getFirst();
            assertThat(persisted.isEncrypted()).isFalse();
            assertThat(persisted.getValue()).isEqualTo("s3cret");
        }

        @Test
        void should_reencrypt_a_changed_property_and_encrypt_a_new_one_in_the_same_batch() throws GeneralSecurityException {
            var api = givenApi(
                buildApiWithProperties(
                    List.of(Property.builder().key("existing").value(DATA_ENCRYPTOR.encrypt("old")).encrypted(true).dynamic(true).build())
                )
            );

            cut.execute(
                new UpdateDynamicPropertiesUseCase.Input(
                    api.getId(),
                    HTTP_DYNAMIC_PROPERTIES,
                    List.of(
                        Property.builder().key("existing").value("new-value").dynamic(true).build(),
                        Property.builder().key("brand-new").value("s3cret").dynamic(true).build()
                    ),
                    true
                )
            );

            var properties = apiCrudServiceInMemory.get(api.getId()).getApiDefinitionHttpV4().getProperties();
            assertThat(properties).hasSize(2);
            properties.forEach(p -> assertThat(p.isEncrypted()).isTrue());

            var existing = properties
                .stream()
                .filter(p -> p.getKey().equals("existing"))
                .findFirst()
                .orElseThrow();
            assertThat(DATA_ENCRYPTOR.decrypt(existing.getValue())).isEqualTo("new-value");

            var brandNew = properties
                .stream()
                .filter(p -> p.getKey().equals("brand-new"))
                .findFirst()
                .orElseThrow();
            assertThat(DATA_ENCRYPTOR.decrypt(brandNew.getValue())).isEqualTo("s3cret");
        }
    }
}
