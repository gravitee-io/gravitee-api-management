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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inmemory.GroupQueryServiceInMemory;
import inmemory.PolicyPluginCrudServiceInMemory;
import inmemory.TagQueryServiceInMemory;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OAIDomainServiceImplTest {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final String ENVIRONMENT_ID = "environmentId";

    private OAIDomainServiceImpl oaiDomainService;
    private final PolicyOperationVisitorManagerImpl policyOperationVisitorManager = new PolicyOperationVisitorManagerImpl();
    private ImportConfiguration importConfiguration;

    @BeforeEach
    void setUp() {
        importConfiguration = mock(ImportConfiguration.class);
        when(importConfiguration.getImportWhitelist()).thenReturn(List.of());
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
        oaiDomainService = new OAIDomainServiceImpl(policyOperationVisitorManager, null, null, null, null, importConfiguration);
    }

    @Test
    void should_throw_when_remote_url_payload_targets_a_private_address() {
        // Given a payload that is a URL resolving to a private/link-local address (SSRF attempt)
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload("http://169.254.169.254/latest/meta-data/");

        // When / Then the URL is rejected before any fetch happens
        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isInstanceOf(UrlForbiddenException.class);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void should_throw_exception_when_specification_is_null(String payload) {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload(payload);

        // When
        assertThatThrownBy(() -> oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false))
            .isExactlyInstanceOf(SwaggerDescriptorException.class)
            .hasMessage("Payload cannot be null");
    }

    @Test
    void should_throw_exception_when_specification_is_does_not_contains_info_section() {
        // Given
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload("{ \"openapi\": \"3.0.0\" }");

        // When
        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isExactlyInstanceOf(SwaggerDescriptorException.class);
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class DeferredResponseValidation {

        private static final String VALID_OPENAPI = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test", "version": "1.0" },
              "paths": {
                "/test": {
                  "get": {
                    "responses": { "200": { "description": "OK" } }
                  }
                }
              }
            }
            """;

        private OAIDomainServiceImpl service;
        private PolicyPluginCrudServiceInMemory policyPluginCrudService;

        @BeforeEach
        void setUp() {
            policyPluginCrudService = new PolicyPluginCrudServiceInMemory();
            policyPluginCrudService.initWith(List.of(PolicyPlugin.builder().id("oas-validation").name("OAS Validation").build()));
            var endpointConnectorPluginService = mock(EndpointConnectorPluginDomainService.class);
            when(endpointConnectorPluginService.getDefaultSharedConfiguration(anyString())).thenReturn("{}");
            service = new OAIDomainServiceImpl(
                new PolicyOperationVisitorManagerImpl(),
                new GroupQueryServiceInMemory(),
                new TagQueryServiceInMemory(),
                endpointConnectorPluginService,
                policyPluginCrudService,
                mock(ImportConfiguration.class)
            );
        }

        @Test
        void should_place_oas_response_validation_last_when_wsdl_format_with_policies() {
            var descriptor = ImportSwaggerDescriptorEntity.builder()
                .payload(VALID_OPENAPI)
                .format(ImportSwaggerDescriptorEntity.Format.WSDL)
                .withPolicies(List.of("rest-to-soap"))
                .build();

            var result = service.convert(ORGANIZATION_ID, ENVIRONMENT_ID, descriptor, false, true);

            assertThat(result).isNotNull();
            var flows = (List<Flow>) result.getApiExport().getFlows();
            assertThat(flows).hasSizeGreaterThanOrEqualTo(2);

            var firstFlow = flows.getFirst();
            assertThat(firstFlow.getName()).isEqualTo("OpenAPI Specification Validation");
            assertThat(firstFlow.getRequest()).hasSize(1);
            assertThat(firstFlow.getResponse()).isEmpty();

            var lastFlow = flows.getLast();
            assertThat(lastFlow.getName()).isEqualTo("OpenAPI Specification Validation");
            assertThat(lastFlow.getResponse()).hasSize(1);
        }

        @Test
        void should_not_defer_response_validation_for_non_wsdl_format() {
            var descriptor = ImportSwaggerDescriptorEntity.builder().payload(VALID_OPENAPI).build();

            var result = service.convert(ORGANIZATION_ID, ENVIRONMENT_ID, descriptor, false, true);

            assertThat(result).isNotNull();
            var flows = (List<Flow>) result.getApiExport().getFlows();
            var oasFlows = flows
                .stream()
                .filter(f -> "OpenAPI Specification Validation".equals(f.getName()))
                .toList();
            assertThat(oasFlows).hasSize(1);
            assertThat(oasFlows.getFirst().getRequest()).hasSize(1);
            assertThat(oasFlows.getFirst().getResponse()).hasSize(1);
        }
    }
}
