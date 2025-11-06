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
package io.gravitee.apim.core.api.domain_service.import_definition;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.MetadataFixtures;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.metadata.model.Metadata;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportDefinitionMetadataDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER = "user";
    private static final String API_ID = "api-id";
    private static final String API_CROSS_ID = "api-id";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER).build())
        .build();
    private final ImportDefinitionMetadataDomainServiceTestInitializer initializer =
        new ImportDefinitionMetadataDomainServiceTestInitializer();
    private ImportDefinitionMetadataDomainService service;

    @BeforeEach
    void setUp() {
        service = initializer.initialize();
    }

    @AfterEach
    void tearDown() {
        initializer.tearDown();
    }

    @ParameterizedTest
    @EmptySource
    void should_do_nothing_when_metadata_list_is_empty(Set<NewApiMetadata> metadataSet) {
        var existingMetadata = MetadataFixtures.anApiMetadata().toBuilder().referenceId(API_ID).build();
        initializer.metadataCrudServiceInMemory.initWith(List.of(existingMetadata));

        service.upsertMetadata(API_ID, metadataSet, null);

        assertThat(initializer.metadataCrudServiceInMemory.storage()).containsOnly(existingMetadata);
    }

    @Test
    public void should_update_api_metadata() {
        var metadataToUpdateKey = "metadata to update";
        initializer.apiMetadataQueryServiceInMemory.initWith(
            List.of(
                Metadata.builder()
                    .referenceId(API_ID)
                    .referenceType(Metadata.ReferenceType.API)
                    .key(metadataToUpdateKey)
                    .value("value")
                    .name("name")
                    .build()
            )
        );

        var existingMetadataKey = "existing metadata key";
        initializer.metadataCrudServiceInMemory.initWith(
            List.of(
                Metadata.builder()
                    .referenceId(API_ID)
                    .referenceType(Metadata.ReferenceType.API)
                    .key(existingMetadataKey)
                    .value("existing value")
                    .name("existing name")
                    .build()
            )
        );

        var metadataToCreate = "metadata to create";
        var importDefinitionMetadata = Set.of(
            NewApiMetadata.builder().apiId(API_ID).key(metadataToUpdateKey).value("updated value").name("updated name").build(),
            NewApiMetadata.builder().apiId(API_ID).key(metadataToCreate).value("true").name("a boolean").build()
        );

        service.upsertMetadata(API_ID, importDefinitionMetadata, AUDIT_INFO);

        var apiMetadata = initializer.metadataCrudServiceInMemory.findByApiId(API_ID);
        assertThat(apiMetadata)
            .hasSize(3)
            .extracting(Metadata::getKey)
            .containsExactlyInAnyOrder(existingMetadataKey, metadataToUpdateKey, metadataToCreate);
        assertThat(
            apiMetadata
                .stream()
                .filter(m -> metadataToUpdateKey.equals(m.getKey()))
                .findFirst()
        )
            .isNotEmpty()
            .hasValueSatisfying(metadata -> {
                assertThat(metadata.getValue()).isEqualTo("updated value");
                assertThat(metadata.getName()).isEqualTo("updated name");
            });
        assertThat(
            apiMetadata
                .stream()
                .filter(m -> metadataToCreate.equals(m.getKey()))
                .findFirst()
        )
            .isNotEmpty()
            .hasValueSatisfying(metadata -> {
                assertThat(metadata.getValue()).isEqualTo("true");
                assertThat(metadata.getName()).isEqualTo("a boolean");
            });
    }
}
