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
import io.gravitee.rest.api.model.MetadataFormat;
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
                "api-id",
                List.of(
                    Metadata
                        .builder()
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata
                        .builder()
                        .key("homepage")
                        .value("https://gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.URL)
                )
            );

            // when
            var result = service.findApiMetadata("api-id");

            // then
            Assertions
                .assertThat(result)
                .hasSize(2)
                .containsEntry(
                    "team-contact",
                    ApiMetadata.builder().key("team-contact").value("team@gravitee.io").format(MetadataFormat.MAIL).build()
                )
                .containsEntry(
                    "homepage",
                    ApiMetadata.builder().key("homepage").value("https://gravitee.io").format(MetadataFormat.URL).build()
                );
        }

        @Test
        void should_return_api_metadata_with_their_default_value() {
            // given
            givenExistingDefaultMetadata(
                List.of(
                    Metadata
                        .builder()
                        .key("team-contact")
                        .value("admin@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata.builder().key("brand").value("Gravitee").format(io.gravitee.repository.management.model.MetadataFormat.STRING)
                )
            );
            givenExistingApiMetadata(
                "api-id",
                List.of(
                    Metadata
                        .builder()
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.MAIL),
                    Metadata
                        .builder()
                        .key("homepage")
                        .value("https://gravitee.io")
                        .format(io.gravitee.repository.management.model.MetadataFormat.URL)
                )
            );

            // when
            var result = service.findApiMetadata("api-id");

            // then
            Assertions
                .assertThat(result)
                .hasSize(3)
                .containsEntry(
                    "team-contact",
                    ApiMetadata
                        .builder()
                        .key("team-contact")
                        .value("team@gravitee.io")
                        .defaultValue("admin@gravitee.io")
                        .format(MetadataFormat.MAIL)
                        .build()
                )
                .containsEntry(
                    "homepage",
                    ApiMetadata.builder().key("homepage").value("https://gravitee.io").format(MetadataFormat.URL).build()
                )
                .containsEntry("brand", ApiMetadata.builder().key("brand").defaultValue("Gravitee").format(MetadataFormat.STRING).build());
        }

        @Test
        public void should_throw_when_fail_to_fetch_api_metadata() {
            givenApiMetadataFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.findApiMetadata("api-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_default_metadata() {
            givenDefaultMetadataFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.findApiMetadata("api-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @SneakyThrows
    private void givenExistingDefaultMetadata(List<Metadata.MetadataBuilder> metadata) {
        lenient().when(metadataRepository.findByReferenceType(eq(MetadataReferenceType.DEFAULT))).thenReturn(List.of());

        lenient()
            .when(metadataRepository.findByReferenceType(eq(MetadataReferenceType.DEFAULT)))
            .thenReturn(metadata.stream().map(m -> m.referenceType(MetadataReferenceType.API).build()).toList());
    }

    @SneakyThrows
    private void givenExistingApiMetadata(String apiId, List<Metadata.MetadataBuilder> metadata) {
        lenient().when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.API), any())).thenReturn(List.of());

        lenient()
            .when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.API), eq(apiId)))
            .thenReturn(metadata.stream().map(m -> m.referenceType(MetadataReferenceType.API).referenceId(apiId).build()).toList());
    }

    @SneakyThrows
    private void givenApiMetadataFailToBeFetched(String message) {
        when(metadataRepository.findByReferenceTypeAndReferenceId(eq(MetadataReferenceType.API), any()))
            .thenThrow(new TechnicalException(message));
    }

    @SneakyThrows
    private void givenDefaultMetadataFailToBeFetched(String message) {
        when(metadataRepository.findByReferenceType(eq(MetadataReferenceType.DEFAULT))).thenThrow(new TechnicalException(message));
    }
}
