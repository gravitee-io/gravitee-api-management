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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.api.domain_service.WsdlParserDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WsdlToUpdateApiUseCaseTest {

    private static final String API_ID = "my-api-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String OPENAPI_YAML = "openapi: 3.0.0\ninfo:\n  title: CalculatorService\n";

    private WsdlParserDomainService wsdlParserDomainService;
    private OAIToUpdateApiUseCase oaiToUpdateApiUseCase;
    private WsdlToUpdateApiUseCase useCase;

    @BeforeEach
    void setUp() {
        wsdlParserDomainService = mock(WsdlParserDomainService.class);
        oaiToUpdateApiUseCase = mock(OAIToUpdateApiUseCase.class);
        useCase = new WsdlToUpdateApiUseCase(wsdlParserDomainService, oaiToUpdateApiUseCase);
    }

    @Test
    void should_throw_when_wsdl_conversion_fails() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenThrow(
            new SwaggerDescriptorException("Failed to convert WSDL to OpenAPI specification")
        );

        assertThatThrownBy(() ->
            useCase.execute(new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", false, false, List.of(), AUDIT_INFO))
        )
            .isInstanceOf(SwaggerDescriptorException.class)
            .hasMessage("Failed to convert WSDL to OpenAPI specification");
    }

    @Test
    void should_pass_openapi_yaml_to_oai_update_use_case() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(new ApiWithFlows(existingApi, List.of())));

        useCase.execute(new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", false, false, List.of(), AUDIT_INFO));

        var captor = ArgumentCaptor.forClass(OAIToUpdateApiUseCase.Input.class);
        verify(oaiToUpdateApiUseCase).execute(captor.capture());
        assertThat(captor.getValue().apiId()).isEqualTo(API_ID);
        assertThat(captor.getValue().importSwaggerDescriptor().getPayload()).isEqualTo(OPENAPI_YAML);
        assertThat(captor.getValue().importSwaggerDescriptor().getFormat()).isEqualTo(ImportSwaggerDescriptorEntity.Format.WSDL);
        assertThat(captor.getValue().withPolicyPaths()).isFalse();
    }

    @Test
    void should_forward_withDocumentation_flag_to_oai_use_case() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(new ApiWithFlows(existingApi, List.of())));

        useCase.execute(new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", true, false, List.of(), AUDIT_INFO));

        var captor = ArgumentCaptor.forClass(OAIToUpdateApiUseCase.Input.class);
        verify(oaiToUpdateApiUseCase).execute(captor.capture());
        assertThat(captor.getValue().withDocumentation()).isTrue();
    }

    @Test
    void should_return_api_from_oai_use_case_output() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        var apiWithFlows = new ApiWithFlows(existingApi, List.of());
        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(apiWithFlows));

        var output = useCase.execute(
            new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", false, false, List.of(), AUDIT_INFO)
        );

        assertThat(output.apiWithFlows().getId()).isEqualTo(API_ID);
    }

    @Test
    void should_forward_withPolicies_to_import_swagger_descriptor() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(new ApiWithFlows(existingApi, List.of())));

        useCase.execute(
            new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", false, false, List.of("rest-to-soap"), AUDIT_INFO)
        );

        var captor = ArgumentCaptor.forClass(OAIToUpdateApiUseCase.Input.class);
        verify(oaiToUpdateApiUseCase).execute(captor.capture());
        assertThat(captor.getValue().importSwaggerDescriptor().getWithPolicies()).containsExactly("rest-to-soap", "xml-json");
        assertThat(captor.getValue().importSwaggerDescriptor().isSkipFlows()).isFalse();
    }

    @Test
    void should_set_skipFlows_when_empty_policies_list_is_provided() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(new ApiWithFlows(existingApi, List.of())));

        useCase.execute(new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", false, false, List.of(), AUDIT_INFO));

        var captor = ArgumentCaptor.forClass(OAIToUpdateApiUseCase.Input.class);
        verify(oaiToUpdateApiUseCase).execute(captor.capture());
        assertThat(captor.getValue().importSwaggerDescriptor().isSkipFlows()).isTrue();
    }

    @Test
    void should_not_skip_flows_when_withPolicies_is_not_provided() {
        when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(new ApiWithFlows(existingApi, List.of())));

        useCase.execute(new WsdlToUpdateApiUseCase.Input.Inline(API_ID, "<definitions/>", false, false, null, AUDIT_INFO));

        var captor = ArgumentCaptor.forClass(OAIToUpdateApiUseCase.Input.class);
        verify(oaiToUpdateApiUseCase).execute(captor.capture());
        assertThat(captor.getValue().importSwaggerDescriptor().isSkipFlows()).isFalse();
    }

    @Nested
    class WithUrlInput {

        @Test
        void should_pass_url_string_to_parser() {
            when(wsdlParserDomainService.toOpenApiYaml(any())).thenReturn(OPENAPI_YAML);
            var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build();
            when(oaiToUpdateApiUseCase.execute(any())).thenReturn(
                new OAIToUpdateApiUseCase.Output(new ApiWithFlows(existingApi, List.of()))
            );

            useCase.execute(
                new WsdlToUpdateApiUseCase.Input.Url(API_ID, "http://example.com/service.wsdl", false, false, List.of(), AUDIT_INFO)
            );

            verify(wsdlParserDomainService).toOpenApiYaml(eq("http://example.com/service.wsdl"));
        }
    }
}
