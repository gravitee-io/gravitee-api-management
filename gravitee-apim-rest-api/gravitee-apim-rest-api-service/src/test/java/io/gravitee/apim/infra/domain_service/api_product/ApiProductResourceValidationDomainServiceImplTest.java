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
package io.gravitee.apim.infra.domain_service.api_product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductResourceValidationDomainServiceImplTest {

    @Mock
    private ResourcesValidationService resourcesValidationService;

    private ApiProductResourceValidationDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiProductResourceValidationDomainServiceImpl(resourcesValidationService);
    }

    @Test
    void should_accept_a_missing_list() {
        service.validate(null);
        service.validate(List.of());

        verify(resourcesValidationService, never()).validateAndSanitize(any());
    }

    @Test
    void should_delegate_schema_validation_for_enabled_resources_only() {
        Resource enabled = resource("cache", "cache", "{\"ttl\":10}", true);
        Resource disabled = resource("legacy", "cache-redis", "{\"ttl\":10}", false);
        List<Resource> resources = List.of(enabled, disabled);
        when(resourcesValidationService.validateAndSanitize(List.of(enabled))).thenReturn(List.of(enabled));

        service.validate(resources);

        verify(resourcesValidationService).validateAndSanitize(List.of(enabled));
        verify(resourcesValidationService, never()).validateAndSanitize(resources);
    }

    @Test
    void should_reject_a_duplicated_name() {
        assertThatThrownBy(() -> service.validate(List.of(resource("cache", "cache", "{}", true), resource("cache", "oauth2", "{}", true))))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("cache");

        verify(resourcesValidationService, never()).validateAndSanitize(any());
    }

    @Test
    void should_reject_a_blank_name() {
        assertThatThrownBy(() -> service.validate(List.of(resource(" ", "cache", "{}", true)))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    @Test
    void should_reject_a_resource_type_that_is_not_allowed() {
        assertThatThrownBy(() -> service.validate(List.of(resource("cloud", "gravitee-resource-cache-cloud-redis", "{}", true))))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("gravitee-resource-cache-cloud-redis");
    }

    @Test
    void should_wrap_a_schema_validation_failure_without_leaking_details() {
        Resource enabled = resource("cache", "cache", "{\"ttl\":10}", true);
        doThrow(new InvalidDataException("configuration does not match the schema"))
            .when(resourcesValidationService)
            .validateAndSanitize(any());

        assertThatThrownBy(() -> service.validate(List.of(enabled)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Resource [cache] configuration is not valid");
    }

    @Test
    void should_not_mask_a_technical_validation_failure_as_a_client_error() {
        Resource enabled = resource("cache", "cache", "{\"ttl\":10}", true);
        doThrow(new TechnicalManagementException("plugin registry unavailable"))
            .when(resourcesValidationService)
            .validateAndSanitize(any());

        assertThatThrownBy(() -> service.validate(List.of(enabled)))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("plugin registry unavailable");
    }

    @Test
    void should_reject_an_enabled_resource_without_configuration() {
        assertThatThrownBy(() -> service.validate(List.of(resource("cache", "cache", null, true)))).isInstanceOf(
            ValidationDomainException.class
        );

        verify(resourcesValidationService, never()).validateAndSanitize(any());
    }

    private static Resource resource(String name, String type, String configuration, boolean enabled) {
        return Resource.builder().name(name).type(type).configuration(configuration).enabled(enabled).build();
    }
}
