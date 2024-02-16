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
package io.gravitee.apim.infra.crud_service.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.MetadataFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.MetadataFormat;
import java.time.Instant;
import java.util.Date;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MetadataCrudServiceImplTest {

    MetadataRepository metadataRepository;

    MetadataCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        metadataRepository = mock(MetadataRepository.class);

        service = new MetadataCrudServiceImpl(metadataRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_metadata() {
            var metadata = MetadataFixtures.anApiMetadata("api-id");
            service.create(metadata);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Metadata.class);
            verify(metadataRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Metadata
                        .builder()
                        .referenceType(io.gravitee.repository.management.model.MetadataReferenceType.API)
                        .referenceId("api-id")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .key("my-key")
                        .format(MetadataFormat.MAIL)
                        .value("my-value")
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_created_metadata() {
            when(metadataRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toCreate = MetadataFixtures.anApiMetadata();
            var result = service.create(toCreate);

            assertThat(result).isEqualTo(toCreate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(metadataRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(MetadataFixtures.anApiMetadata()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to create the my-key metadata of [apiId=api-id]");
        }
    }
}
