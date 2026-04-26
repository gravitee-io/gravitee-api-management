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
package io.gravitee.apim.infra.query_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiMetadataQueryServiceImplTest {

    private static final String API_ID = "api-id";
    private static final String ENV_ID = "env#1";

    @Mock
    MetadataRepository metadataRepository;

    ApiMetadataQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiMetadataQueryServiceImpl(metadataRepository);
    }

    @Nested
    class FindApiMetadata {

        @Test
        void should_return_api_metadata() {
            // given
            givenExistingApiMetadata(
                API_ID,
                List.of(
                    Metadata.builder()
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata.builder()
                        .key("homepage")
                        .value("https://gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.URL)
                )
            );

            // when
            var result = service.findApiMetadata(ENV_ID, API_ID);

            // then
            Assertions.assertThat(result)
                .hasSize(2)
                .containsEntry(
                    "team-contact",
                    ApiMetadata.builder()
                        .apiId(API_ID)
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.MAIL)
                        .build()
                )
                .containsEntry(
                    "homepage",
                    ApiMetadata.builder()
                        .apiId(API_ID)
                        .key("homepage")
                        .value("https://gravitee.io")
                        .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.URL)
                        .build()
                );
        }

        @Test
        void should_return_api_metadata_with_their_default_value() {
            // given
            givenExistingEnvironmentMetadata(
                ENV_ID,
                List.of(
                    Metadata.builder()
                        .key("team-contact")
                        .value("admin@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata.builder().key("brand").value("Gravitee").format(io.gravitee.repository.management.model.MetadataFormat.STRING)
                )
            );
            givenExistingApiMetadata(
                API_ID,
                List.of(
                    Metadata.builder()
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata.builder()
                        .key("homepage")
                        .value("https://gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.URL)
                )
            );

            // when
            var result = service.findApiMetadata(ENV_ID, API_ID);

            // then
            Assertions.assertThat(result)
                .hasSize(3)
                .containsEntry(
                    "team-contact",
                    ApiMetadata.builder()
                        .apiId(API_ID)
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .defaultValue("admin@gravitee.io")
                        .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.MAIL)
                        .build()
                )
                .containsEntry(
                    "homepage",
                    ApiMetadata.builder()
                        .apiId(API_ID)
                        .key("homepage")
                        .value("https://gravitee.io")
                        .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.URL)
                        .build()
                )
                .containsEntry(
                    "brand",
                    ApiMetadata.builder()
                        .key("brand")
                        .defaultValue("Gravitee")
                        .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.STRING)
                        .build()
                );
        }

        @Test
        public void should_throw_when_fail_to_fetch_api_metadata() {
            givenExistingEnvironmentMetadata(
                ENV_ID,
                List.of(
                    Metadata.builder()
                        .key("team-contact")
                        .value("admin@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata.builder().key("brand").value("Gravitee").format(io.gravitee.repository.management.model.MetadataFormat.STRING)
                )
            );
            givenApiMetadataFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.findApiMetadata(ENV_ID, API_ID));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_default_metadata() {
            givenEnvironmentMetadataFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.findApiMetadata(ENV_ID, API_ID));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @SneakyThrows
    private void givenExistingEnvironmentMetadata(String envId, List<Metadata.MetadataBuilder> metadata) {
        lenient()
            .when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.ENVIRONMENT), eq(envId)))
            .thenReturn(List.of());

        lenient()
            .when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.ENVIRONMENT), eq(envId)))
            .thenReturn(
                metadata
                    .stream()
                    .map(m -> m.referenceType(MetadataReferenceType.API).build())
                    .toList()
            );
    }

    @SneakyThrows
    private void givenExistingApiMetadata(String apiId, List<Metadata.MetadataBuilder> metadata) {
        lenient().when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.API), any())).thenReturn(List.of());

        lenient()
            .when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.API), eq(apiId)))
            .thenReturn(
                metadata
                    .stream()
                    .map(m -> m.referenceType(MetadataReferenceType.API).referenceId(apiId).build())
                    .toList()
            );
    }

    @SneakyThrows
    private void givenApiMetadataFailToBeFetched(String message) {
        when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.API), any())).thenThrow(
            new TechnicalException(message)
        );
    }

    @SneakyThrows
    private void givenEnvironmentMetadataFailToBeFetched(String message) {
        when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.ENVIRONMENT), any())).thenThrow(
            new TechnicalException(message)
        );
    }

    @Nested
    class FindApiMetadataForApis {

        private static final String API_ID_2 = "api-id-2";

        @SneakyThrows
        @Test
        void should_return_metadata_for_multiple_apis() {
            // mock env defaults (called once for both apis)
            lenient()
                .when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.ENVIRONMENT), eq(ENV_ID)))
                .thenReturn(
                    List.of(
                        Metadata.builder()
                            .referenceType(MetadataReferenceType.ENVIRONMENT)
                            .referenceId(ENV_ID)
                            .key("brand")
                            .value("Gravitee")
                            .format(io.gravitee.repository.management.model.MetadataFormat.STRING)
                            .build()
                    )
                );
            // mock batch per-API metadata — single $in call returning both APIs' overrides
            lenient()
                .when(metadataRepository.findByReferenceTypeAndReferenceIdIn(eq(MetadataReferenceType.API), any()))
                .thenReturn(
                    List.of(
                        Metadata.builder()
                            .referenceType(MetadataReferenceType.API)
                            .referenceId(API_ID)
                            .key("team-contact")
                            .value("team@gravitee.io")
                            .format(io.gravitee.repository.management.model.MetadataFormat.MAIL)
                            .build(),
                        Metadata.builder()
                            .referenceType(MetadataReferenceType.API)
                            .referenceId(API_ID_2)
                            .key("team-contact")
                            .value("other@gravitee.io")
                            .format(io.gravitee.repository.management.model.MetadataFormat.MAIL)
                            .build()
                    )
                );

            var result = service.findApiMetadataForApis(ENV_ID, List.of(API_ID, API_ID_2));

            Assertions.assertThat(result).hasSize(2).containsKey(API_ID).containsKey(API_ID_2);
            Assertions.assertThat(result.get(API_ID)).containsKey("brand").containsKey("team-contact");
            Assertions.assertThat(result.get(API_ID).get("team-contact").getValue()).isEqualTo("team@gravitee.io");
            Assertions.assertThat(result.get(API_ID_2).get("team-contact").getValue()).isEqualTo("other@gravitee.io");
        }

        @SneakyThrows
        @Test
        void should_return_only_env_defaults_for_api_with_no_overrides() {
            lenient()
                .when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.ENVIRONMENT), eq(ENV_ID)))
                .thenReturn(List.of());
            lenient()
                .when(metadataRepository.findByReferenceTypeAndReferenceIdIn(eq(MetadataReferenceType.API), any()))
                .thenReturn(List.of());

            var result = service.findApiMetadataForApis(ENV_ID, List.of(API_ID));

            Assertions.assertThat(result).hasSize(1).containsKey(API_ID);
            Assertions.assertThat(result.get(API_ID)).isEmpty();
        }

        @Test
        void should_throw_when_repository_fails() {
            givenEnvironmentMetadataFailToBeFetched("batch fetch error");

            Throwable throwable = catchThrowable(() -> service.findApiMetadataForApis(ENV_ID, List.of(API_ID)));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("batch fetch error");
        }
    }
}
