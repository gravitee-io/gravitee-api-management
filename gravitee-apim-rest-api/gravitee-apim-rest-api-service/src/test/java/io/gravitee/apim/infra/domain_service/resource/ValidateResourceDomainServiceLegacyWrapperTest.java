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

import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
