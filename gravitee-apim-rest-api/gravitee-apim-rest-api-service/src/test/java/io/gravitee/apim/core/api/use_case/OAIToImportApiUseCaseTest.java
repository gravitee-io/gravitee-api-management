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
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fixtures.core.model.AuditInfoFixtures;
import initializers.ImportDefinitionCreateDomainServiceTestInitializer;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.exception.InvalidImportWithOASValidationPolicyException;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.api.ApiImportDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.domain_service.api.OAIDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.plugin.EndpointConnectorPluginLegacyWrapper;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
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
    private static final String USER_ID = "user-id";
    private static final String USER_EMAIL = "jane.doe@gravitee.io";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private final PolicyOperationVisitorManagerImpl policyOperationVisitorManager = new PolicyOperationVisitorManagerImpl();
    private final OAIDomainServiceImpl oaiDomainService = new OAIDomainServiceImpl(policyOperationVisitorManager);
    private final EndpointConnectorPluginLegacyWrapper endpointConnectorPluginService = mock(EndpointConnectorPluginLegacyWrapper.class);
    private OAIToImportApiUseCase useCase;
    ImportDefinitionCreateDomainServiceTestInitializer importDefinitionCreateDomainServiceTestInitializer;
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PolicyPluginCrudServiceInMemory policyPluginCrudService = new PolicyPluginCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        importDefinitionCreateDomainServiceTestInitializer = new ImportDefinitionCreateDomainServiceTestInitializer(apiCrudService);
        var groupQueryService = new GroupQueryServiceInMemory();
        groupQueryService.initWith(List.of(Group.builder().id("1").name("group1").environmentId(ENVIRONMENT_ID).build()));
        var tagQueryService = new TagQueryServiceInMemory();
        tagQueryService.initWith(
            List.of(
                Tag.builder().id("1").name("tag1").referenceId(ORGANIZATION_ID).referenceType(Tag.TagReferenceType.ORGANIZATION).build()
            )
        );

        when(endpointConnectorPluginService.getSharedConfigurationSchema(anyString())).thenReturn(SHARED_CONFIGURATION);

        importDefinitionCreateDomainServiceTestInitializer.parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                ),
                new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        importDefinitionCreateDomainServiceTestInitializer.userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email(USER_EMAIL).build())
        );
        when(
            importDefinitionCreateDomainServiceTestInitializer.validateApiDomainService.validateAndSanitizeForCreation(
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenAnswer(invocation -> invocation.getArgument(0));

        useCase =
            new OAIToImportApiUseCase(
                oaiDomainService,
                groupQueryService,
                tagQueryService,
                endpointConnectorPluginService,
                importDefinitionCreateDomainServiceTestInitializer.initialize(),
                policyPluginCrudService
            );
    }

    @Test
    @SneakyThrows
    void should_map_groups_names_to_ids() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
        importSwaggerDescriptor.setPayload(Resources.toString(resource, Charsets.UTF_8));

        // When
        var output = useCase.execute(new OAIToImportApiUseCase.Input(importSwaggerDescriptor, AUDIT_INFO));

        // Then
        assertThat(output).isNotNull();

        var importDefinition = output.apiWithFlows();
        assertThat(importDefinition).isNotNull();
        assertThat(importDefinition.getGroups()).containsExactly("1");
    }

    @Test
    @SneakyThrows
    void should_map_tags_names_to_ids() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
        importSwaggerDescriptor.setPayload(Resources.toString(resource, Charsets.UTF_8));

        // When
        var output = useCase.execute(new OAIToImportApiUseCase.Input(importSwaggerDescriptor, AUDIT_INFO));

        // Then
        assertThat(output).isNotNull();

        var importDefinition = output.apiWithFlows();
        assertThat(importDefinition).isNotNull();
        assertThat(importDefinition.getTags()).containsExactly("1");
    }

    @Test
    @SneakyThrows
    void should_add_shared_configuration_in_endpoint_group() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
        importSwaggerDescriptor.setPayload(Resources.toString(resource, Charsets.UTF_8));

        // When
        var output = useCase.execute(new OAIToImportApiUseCase.Input(importSwaggerDescriptor, AUDIT_INFO));

        // Then
        assertThat(output).isNotNull();

        var importDefinition = output.apiWithFlows();
        assertThat(importDefinition).isNotNull();
        assertThat(importDefinition.getApiDefinitionV4().getEndpointGroups())
            .hasSize(1)
            .extracting(EndpointGroup::getSharedConfiguration)
            .containsExactly(SHARED_CONFIGURATION);
    }

    @Nested
    class WithDocumentation {

        @Test
        @SneakyThrows
        void should_add_openapi_documentation_page() {
            // Given
            var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
            var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
            final String openApiAsString = Resources.toString(resource, Charsets.UTF_8);
            importSwaggerDescriptor.setPayload(openApiAsString);

            // When
            var output = useCase.execute(new OAIToImportApiUseCase.Input(importSwaggerDescriptor, true, false, AUDIT_INFO));

            // Then
            assertThat(output).isNotNull();

            var importDefinition = output.apiWithFlows();
            assertThat(importDefinition).isNotNull();
            assertThat(importDefinitionCreateDomainServiceTestInitializer.pageCrudService.storage())
                .hasSize(1)
                .first()
                .satisfies(page -> {
                    assertThat(page.getReferenceId()).isEqualTo(output.apiWithFlows().getId());
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

            // When
            var output = useCase.execute(new OAIToImportApiUseCase.Input(importSwaggerDescriptor, false, false, AUDIT_INFO));

            // Then
            assertThat(output).isNotNull();

            var importDefinition = output.apiWithFlows();
            assertThat(importDefinition).isNotNull();
            assertThat(importDefinitionCreateDomainServiceTestInitializer.pageCrudService.storage()).isEmpty();
        }
    }

    @Nested
    class WithOASValidationPolicy {

        @Test
        @SneakyThrows
        void should_add_OAS_validation_policy() {
            // Given
            var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
            var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
            final String openApiAsString = Resources.toString(resource, Charsets.UTF_8);
            importSwaggerDescriptor.setPayload(openApiAsString);
            policyPluginCrudService.initWith(
                List.of(PolicyPlugin.builder().id("oas-validation").name("OpenAPI Specification Validation").build())
            );

            // When
            var output = useCase.execute(
                OAIToImportApiUseCase.Input
                    .builder()
                    .importSwaggerDescriptor(importSwaggerDescriptor)
                    .withOASValidationPolicy(true)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            // Then
            assertThat(output).isNotNull();

            var importDefinition = output.apiWithFlows();
            assertThat(importDefinition).isNotNull();
            // Check that the OAS validation policy is added
            assertThat(importDefinition.getApiDefinitionV4().getFlows())
                .first()
                .satisfies(flow -> {
                    assertThat(flow.getName()).isEqualTo("OpenAPI Specification Validation");
                    assertThat(flow.getRequest()).isNotNull();
                    assertThat(((HttpSelector) flow.getSelectors().get(0)).getPath()).isEqualTo("/");
                    assertThat(((HttpSelector) flow.getSelectors().get(0)).getPathOperator()).isEqualTo(Operator.STARTS_WITH);
                    var oasValidationStep = flow.getRequest().get(0);
                    assertThat(oasValidationStep.getPolicy()).isEqualTo("oas-validation");
                    assertThat(oasValidationStep.getConfiguration()).isEqualTo("{\"resourceName\":\"OpenAPI Specification\"}");
                });

            // Check that the Resource is added
            assertThat(importDefinition.getApiDefinitionV4().getResources())
                .hasSize(1)
                .first()
                .satisfies(resource1 -> {
                    assertThat(resource1.getName()).isEqualTo("OpenAPI Specification");
                    assertThat(resource1.getType()).isEqualTo("content-provider-inline-resource");
                    assertThat(resource1.getConfiguration()).contains("\"content\":");
                });
        }

        @Test
        @SneakyThrows
        void should_throw_if_oas_validation_policy_not_found() {
            // Given
            var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
            var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
            final String openApiAsString = Resources.toString(resource, Charsets.UTF_8);
            importSwaggerDescriptor.setPayload(openApiAsString);

            // When
            var throwable = catchThrowable(() ->
                useCase.execute(
                    OAIToImportApiUseCase.Input
                        .builder()
                        .importSwaggerDescriptor(importSwaggerDescriptor)
                        .withOASValidationPolicy(true)
                        .auditInfo(AUDIT_INFO)
                        .build()
                )
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(InvalidImportWithOASValidationPolicyException.class)
                .hasMessage("Invalid import with OAS validation policy: Policy not found");
        }

        @Test
        @SneakyThrows
        void should_not_add_OAS_validation_policy() {
            // Given
            var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
            var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");
            final String openApiAsString = Resources.toString(resource, Charsets.UTF_8);
            importSwaggerDescriptor.setPayload(openApiAsString);

            // When
            var output = useCase.execute(
                OAIToImportApiUseCase.Input
                    .builder()
                    .importSwaggerDescriptor(importSwaggerDescriptor)
                    .withOASValidationPolicy(false)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            // Then
            assertThat(output).isNotNull();

            var importDefinition = output.apiWithFlows();
            assertThat(importDefinition).isNotNull();
            // Check that the OAS validation policy is not added
            assertThat(importDefinition.getApiDefinitionV4().getFlows())
                .noneMatch(flow -> flow.getName().equals("OpenAPI Specification Validation"));
            // Check that the Resource is not added
            assertThat(importDefinition.getApiDefinitionV4().getResources()).isEmpty();
        }
    }
}
