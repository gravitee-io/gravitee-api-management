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
package io.gravitee.apim.infra.domain_service.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ValidateResourceDomainServiceLegacyWrapperTest {

    @Mock
    ResourcesValidationService resourcesValidationService;

    @InjectMocks
    ValidateResourceDomainServiceLegacyWrapper service;

    private static final String ENVIRONMENT_ID = "environment-id";

    @Test
    void should_call_legacy_service_for_validate_and_sanitizing() {
        Resource resource = new Resource("oauth2-am-resource", "oauth2-am-resource", "configuration", true);
        service.validateAndSanitize(new ValidateResourceDomainService.Input(ENVIRONMENT_ID, List.of(resource)));

        verify(resourcesValidationService).validateAndSanitize(List.of(resource));
    }

    @Test
    void should_answer_the_configuration_the_legacy_service_accepts_for_one_resource() {
        doAnswer(invocation -> {
            List<Resource> resources = invocation.getArgument(0);
            resources.getFirst().setConfiguration("{\"accepted\":true}");
            return resources;
        })
            .when(resourcesValidationService)
            .validateAndSanitize(anyList());

        var accepted = service.validateAndSanitizeConfiguration("oauth2-am-resource", "{}");

        assertThat(accepted).isEqualTo("{\"accepted\":true}");
    }

    @Test
    void should_hand_the_legacy_service_an_enabled_resource_of_that_type() {
        service.validateAndSanitizeConfiguration("oauth2-am-resource", "{\"a\":1}");

        var handed = ArgumentCaptor.forClass(List.class);
        verify(resourcesValidationService).validateAndSanitize(handed.capture());
        assertThat((List<Resource>) handed.getValue())
            .singleElement()
            .satisfies(resource -> {
                assertThat(resource.getType()).isEqualTo("oauth2-am-resource");
                assertThat(resource.getConfiguration()).isEqualTo("{\"a\":1}");
                assertThat(resource.isEnabled()).isTrue();
            });
    }

    @Test
    void should_let_the_schema_error_of_one_resource_through() {
        doThrow(new InvalidDataException("$.url: is missing but it is required"))
            .when(resourcesValidationService)
            .validateAndSanitize(anyList());

        assertThatThrownBy(() -> service.validateAndSanitizeConfiguration("oauth2-am-resource", "{}"))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("$.url: is missing but it is required");
    }
}
