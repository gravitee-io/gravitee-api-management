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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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
        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.emptyList());
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
        oaiDomainService = new OAIDomainServiceImpl(policyOperationVisitorManager, null, null, null, null, importConfiguration);
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

    @Test
    void should_validate_url_when_payload_is_a_url() {
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        String privateUrl = "http://localhost:8080/swagger.json";
        importSwaggerDescriptor.setPayload(privateUrl);

        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isExactlyInstanceOf(UrlForbiddenException.class);
    }

    @Test
    void should_allow_private_url_when_configured() {
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(true);
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        String privateUrl = "http://localhost:8080/swagger.json";
        importSwaggerDescriptor.setPayload(privateUrl);

        assertThatThrownBy(() -> oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false))
            .isInstanceOf(SwaggerDescriptorException.class)
            .hasMessage("Malformed descriptor");
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://example.com/swagger.json", "http://example.com/api.yaml", "https://api.example.com/openapi.json" })
    void should_allow_public_urls(String publicUrl) {
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload(publicUrl);
        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isInstanceOf(Exception.class);
    }

    @Test
    void should_allow_whitelisted_url() {
        String whitelistedUrl = "http://localhost:8080/swagger.json";
        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.singletonList("http://localhost:8080"));
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptor.setPayload(whitelistedUrl);

        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isInstanceOf(SwaggerDescriptorException.class);
    }

    @Test
    void should_not_validate_url_when_payload_is_not_a_url() {
        var importSwaggerDescriptor = new ImportSwaggerDescriptorEntity();
        String jsonPayload = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              }
            }
            """;
        importSwaggerDescriptor.setPayload(jsonPayload);
        assertThatThrownBy(() ->
            oaiDomainService.convert(ORGANIZATION_ID, ENVIRONMENT_ID, importSwaggerDescriptor, false, false)
        ).isNotInstanceOf(UrlForbiddenException.class);
    }
}
