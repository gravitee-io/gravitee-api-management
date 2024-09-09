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
package io.gravitee.apim.infra.query_service.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditMetadataQueryServiceImplTest {

    private static final String ENV_ID = "env#1";

    @Mock
    ApiRepository apiRepository;

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    MetadataRepository metadataRepository;

    @Mock
    PageRepository pageRepository;

    @Mock
    PlanRepository planRepository;

    @Mock
    UserRepository userRepository;

    AuditMetadataQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);

        service =
            new AuditMetadataQueryServiceImpl(
                apiRepository,
                applicationRepository,
                groupRepository,
                metadataRepository,
                pageRepository,
                planRepository,
                userRepository
            );
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class FetchUserNameMetadata {

        public static final String USER_ID = "user-id";

        @Test
        @SneakyThrows
        void should_return_user_display_name() {
            // Given
            when(userRepository.findById(USER_ID))
                .thenAnswer(invocation ->
                    Optional.of(User.builder().id(invocation.getArgument(0)).firstname("John").lastname("Doe").build())
                );

            // When
            var result = service.fetchUserNameMetadata(USER_ID);

            // Then
            Assertions.assertThat(result).isEqualTo("John Doe");
        }

        @Test
        @SneakyThrows
        void should_return_user_id_when_no_user_is_found() {
            // Given
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            // When
            var result = service.fetchUserNameMetadata(USER_ID);

            // Then
            Assertions.assertThat(result).isEqualTo(USER_ID);
        }

        @Test
        @SneakyThrows
        void should_return_user_id_when_technical_exception_occurs() {
            // Given
            when(userRepository.findById(any())).thenThrow(new TechnicalException());

            // When
            var result = service.fetchUserNameMetadata(USER_ID);

            // Then
            Assertions.assertThat(result).isEqualTo("user-id");
        }
    }

    @Nested
    class FetchApiNameMetadata {

        public static final String API_ID = "api-id";

        @Test
        @SneakyThrows
        void should_return_api_name() {
            // Given
            when(apiRepository.findById(API_ID))
                .thenAnswer(invocation -> Optional.of(Api.builder().id(invocation.getArgument(0)).name("My API").build()));

            // When
            var result = service.fetchApiNameMetadata(API_ID);

            // Then
            Assertions.assertThat(result).isEqualTo("My API");
        }

        @Test
        @SneakyThrows
        void should_return_api_id_when_no_api_is_found() {
            // Given
            when(apiRepository.findById(any())).thenReturn(Optional.empty());

            // When
            var result = service.fetchApiNameMetadata(API_ID);

            // Then
            Assertions.assertThat(result).isEqualTo(API_ID);
        }

        @Test
        @SneakyThrows
        void should_return_api_id_when_technical_exception_occurs() {
            // Given
            when(apiRepository.findById(any())).thenThrow(new TechnicalException());

            // When
            var result = service.fetchApiNameMetadata(API_ID);

            // Then
            Assertions.assertThat(result).isEqualTo(API_ID);
        }
    }

    @Nested
    class FetchPropertyMetadata {

        public static final AuditEntity AUDIT = AuditEntity.builder().build();
        public static final String USER_ID = "user-id";

        @Nested
        class ApiProperty {

            public static final String API_ID = "api-id";

            @Test
            @SneakyThrows
            void should_return_api_name() {
                // Given
                when(apiRepository.findById(API_ID))
                    .thenAnswer(invocation -> Optional.of(Api.builder().id(invocation.getArgument(0)).name("My API").build()));

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.API.name(), API_ID);

                // Then
                Assertions.assertThat(result).isEqualTo("My API");
            }

            @Test
            @SneakyThrows
            void should_return_api_id_when_no_api_is_found() {
                // Given
                when(apiRepository.findById(any())).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.API.name(), API_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(API_ID);
            }

            @Test
            @SneakyThrows
            void should_return_api_id_when_technical_exception_occurs() {
                // Given
                when(apiRepository.findById(any())).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.API.name(), API_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(API_ID);
            }
        }

        @Nested
        class ApplicationProperty {

            public static final String APPLICATION_ID = "application-id";

            @Test
            @SneakyThrows
            void should_return_application_name() {
                // Given
                when(applicationRepository.findById(APPLICATION_ID))
                    .thenAnswer(invocation ->
                        Optional.of(Application.builder().id(invocation.getArgument(0)).name("My Application").build())
                    );

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.APPLICATION.name(), APPLICATION_ID);

                // Then
                Assertions.assertThat(result).isEqualTo("My Application");
            }

            @Test
            @SneakyThrows
            void should_return_application_id_when_no_application_is_found() {
                // Given
                when(applicationRepository.findById(any())).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.APPLICATION.name(), APPLICATION_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(APPLICATION_ID);
            }

            @Test
            @SneakyThrows
            void should_return_application_id_when_technical_exception_occurs() {
                // Given
                when(applicationRepository.findById(any())).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.APPLICATION.name(), APPLICATION_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(APPLICATION_ID);
            }
        }

        @Nested
        class GroupProperty {

            public static final String GROUP_ID = "group-id";

            @Test
            @SneakyThrows
            void should_return_group_name() {
                // Given
                when(groupRepository.findById(GROUP_ID))
                    .thenAnswer(invocation -> Optional.of(Group.builder().id(invocation.getArgument(0)).name("My Group").build()));

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.GROUP.name(), GROUP_ID);

                // Then
                Assertions.assertThat(result).isEqualTo("My Group");
            }

            @Test
            @SneakyThrows
            void should_return_group_id_when_no_group_is_found() {
                // Given
                when(groupRepository.findById(any())).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.GROUP.name(), GROUP_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(GROUP_ID);
            }

            @Test
            @SneakyThrows
            void should_return_group_id_when_technical_exception_occurs() {
                // Given
                when(groupRepository.findById(any())).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.GROUP.name(), GROUP_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(GROUP_ID);
            }
        }

        @Nested
        class MetadataProperty {

            public static final String API_ID = "api-id";
            public static final String APPLICATION_ID = "application-id";
            public static final String METADATA_KEY = "my-key";

            @Test
            @SneakyThrows
            void should_return_api_metadata_value() {
                // Given
                when(metadataRepository.findById(METADATA_KEY, API_ID, MetadataReferenceType.API))
                    .thenAnswer(invocation -> Optional.of(Metadata.builder().key(invocation.getArgument(0)).name("My API Metadata").build())
                    );

                // When
                var result = service.fetchPropertyMetadata(
                    AUDIT.toBuilder().referenceId(API_ID).referenceType(AuditEntity.AuditReferenceType.API).build(),
                    AuditProperties.METADATA.name(),
                    METADATA_KEY
                );

                // Then
                Assertions.assertThat(result).isEqualTo("My API Metadata");
            }

            @Test
            @SneakyThrows
            void should_return_application_metadata_value() {
                // Given
                when(metadataRepository.findById(METADATA_KEY, APPLICATION_ID, MetadataReferenceType.APPLICATION))
                    .thenAnswer(invocation ->
                        Optional.of(Metadata.builder().key(invocation.getArgument(0)).name("My Application Metadata").build())
                    );

                // When
                var result = service.fetchPropertyMetadata(
                    AUDIT.toBuilder().referenceId(APPLICATION_ID).referenceType(AuditEntity.AuditReferenceType.APPLICATION).build(),
                    AuditProperties.METADATA.name(),
                    METADATA_KEY
                );

                // Then
                Assertions.assertThat(result).isEqualTo("My Application Metadata");
            }

            @Test
            @SneakyThrows
            void should_return_default_metadata_value() {
                // Given
                when(metadataRepository.findById(METADATA_KEY, ENV_ID, MetadataReferenceType.ENVIRONMENT))
                    .thenAnswer(invocation ->
                        Optional.of(Metadata.builder().key(invocation.getArgument(0)).name("A Default Metadata").build())
                    );

                // When
                var result = service.fetchPropertyMetadata(
                    AUDIT.toBuilder().referenceId(ENV_ID).referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT).build(),
                    AuditProperties.METADATA.name(),
                    METADATA_KEY
                );

                // Then
                Assertions.assertThat(result).isEqualTo("A Default Metadata");
            }

            @Test
            @SneakyThrows
            void should_return_metadata_key_when_no_metadata_is_found() {
                // Given
                when(metadataRepository.findById(METADATA_KEY, API_ID, MetadataReferenceType.API)).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(
                    AUDIT.toBuilder().referenceId(API_ID).referenceType(AuditEntity.AuditReferenceType.API).build(),
                    AuditProperties.METADATA.name(),
                    METADATA_KEY
                );

                // Then
                Assertions.assertThat(result).isEqualTo(METADATA_KEY);
            }

            @Test
            @SneakyThrows
            void should_return_metadata_key_when_technical_exception_occurs() {
                // Given
                when(metadataRepository.findById(METADATA_KEY, API_ID, MetadataReferenceType.API)).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(
                    AUDIT.toBuilder().referenceId(API_ID).referenceType(AuditEntity.AuditReferenceType.API).build(),
                    AuditProperties.METADATA.name(),
                    METADATA_KEY
                );

                // Then
                Assertions.assertThat(result).isEqualTo(METADATA_KEY);
            }
        }

        @Nested
        class PageProperty {

            public static final String PAGE_ID = "page-id";

            @Test
            @SneakyThrows
            void should_return_page_name() {
                // Given
                when(pageRepository.findById(PAGE_ID))
                    .thenAnswer(invocation -> Optional.of(Page.builder().id(invocation.getArgument(0)).name("My Page").build()));

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.PAGE.name(), PAGE_ID);

                // Then
                Assertions.assertThat(result).isEqualTo("My Page");
            }

            @Test
            @SneakyThrows
            void should_return_page_id_when_no_page_is_found() {
                // Given
                when(pageRepository.findById(any())).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.PAGE.name(), PAGE_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(PAGE_ID);
            }

            @Test
            @SneakyThrows
            void should_return_page_id_when_technical_exception_occurs() {
                // Given
                when(pageRepository.findById(any())).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.PAGE.name(), PAGE_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(PAGE_ID);
            }
        }

        @Nested
        class PlanProperty {

            public static final String PLAN_ID = "plan-id";

            @Test
            @SneakyThrows
            void should_return_plan_name() {
                // Given
                when(planRepository.findById(PLAN_ID))
                    .thenAnswer(invocation -> Optional.of(Plan.builder().id(invocation.getArgument(0)).name("My Plan").build()));

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.PLAN.name(), PLAN_ID);

                // Then
                Assertions.assertThat(result).isEqualTo("My Plan");
            }

            @Test
            @SneakyThrows
            void should_return_plan_id_when_no_plan_is_found() {
                // Given
                when(planRepository.findById(any())).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.PLAN.name(), PLAN_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(PLAN_ID);
            }

            @Test
            @SneakyThrows
            void should_return_plan_id_when_technical_exception_occurs() {
                // Given
                when(planRepository.findById(any())).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.PLAN.name(), PLAN_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(PLAN_ID);
            }
        }

        @Nested
        class UserProperty {

            public static final String USER_ID = "user-id";

            @Test
            @SneakyThrows
            void should_return_user_name() {
                // Given
                when(userRepository.findById(USER_ID))
                    .thenAnswer(invocation ->
                        Optional.of(User.builder().id(invocation.getArgument(0)).firstname("John").lastname("Doe").build())
                    );

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.USER.name(), USER_ID);

                // Then
                Assertions.assertThat(result).isEqualTo("John Doe");
            }

            @Test
            @SneakyThrows
            void should_return_user_id_when_no_user_is_found() {
                // Given
                when(userRepository.findById(any())).thenReturn(Optional.empty());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.USER.name(), USER_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(USER_ID);
            }

            @Test
            @SneakyThrows
            void should_return_user_id_when_technical_exception_occurs() {
                // Given
                when(userRepository.findById(any())).thenThrow(new TechnicalException());

                // When
                var result = service.fetchPropertyMetadata(AUDIT, AuditProperties.USER.name(), USER_ID);

                // Then
                Assertions.assertThat(result).isEqualTo(USER_ID);
            }
        }
    }
}
