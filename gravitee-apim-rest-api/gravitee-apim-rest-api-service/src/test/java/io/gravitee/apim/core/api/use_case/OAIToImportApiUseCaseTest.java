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

import static io.gravitee.apim.core.api.use_case.OAIToImportApiUseCase.DEFAULT_IMPORT_PAGE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import inmemory.GroupQueryServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.infra.domain_service.api.OAIDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.plugin.EndpointConnectorPluginLegacyWrapper;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OAIToImportApiUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String SHARED_CONFIGURATION = """
        { "description": "this is a dumb shared configuration" }
    """;
    private final PolicyOperationVisitorManagerImpl policyOperationVisitorManager = new PolicyOperationVisitorManagerImpl();
    private final OAIDomainServiceImpl oaiDomainService = new OAIDomainServiceImpl(policyOperationVisitorManager);
    private final EndpointConnectorPluginLegacyWrapper endpointConnectorPluginService = mock(EndpointConnectorPluginLegacyWrapper.class);
    private OAIToImportApiUseCase useCase;

    @BeforeEach
    void setUp() {
        var groupQueryService = new GroupQueryServiceInMemory();
        groupQueryService.initWith(List.of(Group.builder().id("1").name("group1").environmentId(ENVIRONMENT_ID).build()));
        var tagQueryService = new TagQueryServiceInMemory();
        tagQueryService.initWith(
            List.of(
                Tag.builder().id("1").name("tag1").referenceId(ORGANIZATION_ID).referenceType(Tag.TagReferenceType.ORGANIZATION).build()
            )
        );

        when(endpointConnectorPluginService.getSharedConfigurationSchema(anyString())).thenReturn(SHARED_CONFIGURATION);

        useCase = new OAIToImportApiUseCase(oaiDomainService, groupQueryService, tagQueryService, endpointConnectorPluginService);
    }

    @Test
    @SneakyThrows
    void should_map_groups_names_to_ids() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
        importSwaggerDescriptor.setPayload(Resources.toString(resource, Charsets.UTF_8));

        // When
        var output = useCase.execute(
            new OAIToImportApiUseCase.Input(
                importSwaggerDescriptor,
                AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build()
            )
        );

        // Then
        assertThat(output).isNotNull();

        var importDefinition = output.importDefinition();
        assertThat(importDefinition).isNotNull();
        assertThat(importDefinition.getApiExport().getGroups()).containsExactly("1");
    }

    @Test
    @SneakyThrows
    void should_map_tags_names_to_ids() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
        importSwaggerDescriptor.setPayload(Resources.toString(resource, Charsets.UTF_8));

        // When
        var output = useCase.execute(
            new OAIToImportApiUseCase.Input(
                importSwaggerDescriptor,
                AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build()
            )
        );

        // Then
        assertThat(output).isNotNull();

        var importDefinition = output.importDefinition();
        assertThat(importDefinition).isNotNull();
        assertThat(importDefinition.getApiExport().getTags()).containsExactly("1");
    }

    @Test
    @SneakyThrows
    void should_add_shared_configuration_in_endpoint_group() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
        importSwaggerDescriptor.setPayload(Resources.toString(resource, Charsets.UTF_8));

        // When
        var output = useCase.execute(
            new OAIToImportApiUseCase.Input(
                importSwaggerDescriptor,
                AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build()
            )
        );

        // Then
        assertThat(output).isNotNull();

        var importDefinition = output.importDefinition();
        assertThat(importDefinition).isNotNull();
        assertThat(importDefinition.getApiExport().getEndpointGroups())
            .hasSize(1)
            .extracting(EndpointGroup::getSharedConfiguration)
            .containsExactly(SHARED_CONFIGURATION);
    }

    @Nested
    class Documentation {

        @Test
        @SneakyThrows
        void should_add_openapi_documentation_page() {
            // Given
            var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
            var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
            final String openApiAsString = Resources.toString(resource, Charsets.UTF_8);
            importSwaggerDescriptor.setPayload(openApiAsString);
            importSwaggerDescriptor.setWithDocumentation(true);

            // When
            var output = useCase.execute(
                new OAIToImportApiUseCase.Input(
                    importSwaggerDescriptor,
                    AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build()
                )
            );

            // Then
            assertThat(output).isNotNull();

            var importDefinition = output.importDefinition();
            assertThat(importDefinition).isNotNull();
            assertThat(importDefinition.getPages())
                .hasSize(1)
                .first()
                .satisfies(page -> {
                    assertThat(page.getReferenceId()).isEqualTo(output.importDefinition().getApiExport().getId());
                    assertThat(page.getName()).isEqualTo(DEFAULT_IMPORT_PAGE_NAME);
                    assertThat(page.getContent()).isEqualTo(openApiAsString);
                });
        }

        @Test
        @SneakyThrows
        void should_not_add_openapi_documentation_page() {
            // Given
            var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
            var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
            final String openApiAsString = Resources.toString(resource, Charsets.UTF_8);
            importSwaggerDescriptor.setPayload(openApiAsString);
            importSwaggerDescriptor.setWithDocumentation(false);

            // When
            var output = useCase.execute(
                new OAIToImportApiUseCase.Input(
                    importSwaggerDescriptor,
                    AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build()
                )
            );

            // Then
            assertThat(output).isNotNull();

            var importDefinition = output.importDefinition();
            assertThat(importDefinition).isNotNull();
            assertThat(importDefinition.getPages()).isNull();
        }
    }
}
