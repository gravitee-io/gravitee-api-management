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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fixtures.core.model.AuditInfoFixtures;
import initializers.ImportDefinitionCreateDomainServiceTestInitializer;
import inmemory.ApiCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.api.OAIDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.api.WsdlParserDomainServiceImpl;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WsdlToImportApiUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String SHARED_CONFIGURATION = """
            { "description": "shared config" }
        """;
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final EndpointConnectorPluginDomainService endpointConnectorPluginService = mock(EndpointConnectorPluginDomainService.class);
    private final PolicyPluginCrudServiceInMemory policyPluginCrudService = new PolicyPluginCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    private WsdlToImportApiUseCase useCase;
    private ImportDefinitionCreateDomainServiceTestInitializer importDefinitionCreateDomainServiceTestInitializer;

    @BeforeEach
    void setUp() {
        importDefinitionCreateDomainServiceTestInitializer = new ImportDefinitionCreateDomainServiceTestInitializer(apiCrudService);

        when(endpointConnectorPluginService.getDefaultSharedConfiguration(anyString())).thenReturn(SHARED_CONFIGURATION);

        importDefinitionCreateDomainServiceTestInitializer.parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                ),
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        importDefinitionCreateDomainServiceTestInitializer.userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane@gravitee.io").build())
        );
        when(
            importDefinitionCreateDomainServiceTestInitializer.validateApiDomainService.validateAndSanitizeForCreation(
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer(invocation -> invocation.getArgument(0));

        ImportConfiguration importConfiguration = mock(ImportConfiguration.class);
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(true);
        when(importConfiguration.getImportWhitelist()).thenReturn(List.of());

        useCase = new WsdlToImportApiUseCase(
            new WsdlParserDomainServiceImpl(importConfiguration),
            new OAIDomainServiceImpl(
                new PolicyOperationVisitorManagerImpl(),
                new GroupQueryServiceInMemory(),
                new TagQueryServiceInMemory(),
                endpointConnectorPluginService,
                policyPluginCrudService
            ),
            importDefinitionCreateDomainServiceTestInitializer.initialize()
        );
    }

    @Test
    void should_create_api_with_name_from_wsdl_service_element() {
        var output = useCase.execute(buildInput(loadWsdl(), false, false));

        assertThat(output).isNotNull();
        assertThat(output.apiWithFlows().getName()).isEqualTo("CalculatorService");
    }

    @Test
    void should_create_api_with_endpoint_url_from_wsdl_soap_address() {
        var output = useCase.execute(buildInput(loadWsdl(), false, false));
        var apiDefinition = (Api) output.apiWithFlows().getApiDefinitionValue();

        assertThat(output).isNotNull();
        assertThat(apiDefinition.getEndpointGroups())
            .isNotEmpty()
            .first()
            .satisfies(endpointGroup ->
                assertThat(endpointGroup.getEndpoints())
                    .isNotEmpty()
                    .first()
                    .satisfies(endpoint -> assertThat(endpoint.getConfiguration()).contains("http://localhost:8080/calculator"))
            );
    }

    @Test
    void should_not_create_flows_when_no_policies_are_requested() {
        var output = useCase.execute(buildInput(loadWsdl(), false, false));
        var apiDefinition = (Api) output.apiWithFlows().getApiDefinitionValue();

        assertThat(output).isNotNull();
        assertThat(apiDefinition.getFlows()).isEmpty();
    }

    @Test
    void should_throw_when_wsdl_payload_is_invalid() {
        var throwable = catchThrowable(() -> useCase.execute(buildInput("not valid wsdl content", false, false)));

        assertThat(throwable).isInstanceOf(SwaggerDescriptorException.class);
    }

    @Nested
    class WithDocumentation {

        @Test
        void should_add_documentation_page_with_openapi_yaml_content() {
            var output = useCase.execute(buildInput(loadWsdl(), true, false));

            assertThat(output).isNotNull();
            assertThat(importDefinitionCreateDomainServiceTestInitializer.pageCrudService.storage())
                .hasSize(1)
                .first()
                .satisfies(page -> {
                    assertThat(page.getReferenceId()).isEqualTo(output.apiWithFlows().getId());
                    assertThat(page.getName()).isEqualTo("Swagger");
                    // Content must be the converted OpenAPI YAML, not the raw WSDL XML
                    assertThat(page.getContent()).startsWith("openapi:");
                    assertThat(page.getContent()).doesNotStartWith("<");
                });
        }

        @Test
        void should_not_add_documentation_page_when_disabled() {
            useCase.execute(buildInput(loadWsdl(), false, false));

            assertThat(importDefinitionCreateDomainServiceTestInitializer.pageCrudService.storage()).isEmpty();
        }
    }

    @Nested
    @WireMockTest
    class WithUrlImport {

        @Test
        void should_create_api_from_remote_wsdl_url(WireMockRuntimeInfo wm) {
            var wsdlContent = loadWsdl();
            wm.getWireMock().register(get(urlEqualTo("/calculator.wsdl")).willReturn(aResponse().withStatus(200).withBody(wsdlContent)));

            var input = new WsdlToImportApiUseCase.Input.Url(
                "http://localhost:" + wm.getHttpPort() + "/calculator.wsdl",
                false,
                false,
                List.of(),
                AUDIT_INFO
            );
            var output = useCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.apiWithFlows().getName()).isEqualTo("CalculatorService");
        }

        @Test
        void should_throw_when_remote_url_is_unreachable() {
            var input = new WsdlToImportApiUseCase.Input.Url("http://localhost:1/nonexistent.wsdl", false, false, List.of(), AUDIT_INFO);

            var throwable = catchThrowable(() -> useCase.execute(input));

            assertThat(throwable).isInstanceOf(SwaggerDescriptorException.class);
        }

        @Test
        void should_throw_when_remote_url_returns_invalid_wsdl(WireMockRuntimeInfo wm) {
            wm.getWireMock().register(get(urlEqualTo("/bad.wsdl")).willReturn(aResponse().withStatus(200).withBody("not valid wsdl")));

            var input = new WsdlToImportApiUseCase.Input.Url(
                "http://localhost:" + wm.getHttpPort() + "/bad.wsdl",
                false,
                false,
                List.of(),
                AUDIT_INFO
            );
            var throwable = catchThrowable(() -> useCase.execute(input));

            assertThat(throwable).isInstanceOf(SwaggerDescriptorException.class);
        }
    }

    @Test
    void should_create_flows_when_rest_to_soap_policy_is_requested() {
        var input = new WsdlToImportApiUseCase.Input.Inline(loadWsdl(), false, false, List.of("rest-to-soap"), AUDIT_INFO);
        var output = useCase.execute(input);
        var apiDefinition = (Api) output.apiWithFlows().getApiDefinitionValue();

        // Flows are generated (not skipped) when policies are requested
        assertThat(apiDefinition.getFlows()).isNotEmpty();
    }

    @SneakyThrows
    private String loadWsdl() {
        return Resources.toString(Resources.getResource("wsdl/calculator.wsdl"), Charsets.UTF_8);
    }

    private WsdlToImportApiUseCase.Input buildInput(String payload, boolean withDocumentation, boolean withOASValidationPolicy) {
        return new WsdlToImportApiUseCase.Input.Inline(payload, withDocumentation, withOASValidationPolicy, List.of(), AUDIT_INFO);
    }
}
