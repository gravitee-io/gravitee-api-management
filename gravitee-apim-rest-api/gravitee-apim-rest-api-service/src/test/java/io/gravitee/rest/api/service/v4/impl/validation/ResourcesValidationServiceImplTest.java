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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ResourcesValidationServiceImplTest {

    @Mock
    private ResourceService resourceService;

    private ResourcesValidationService resourcesValidationService;

    @BeforeEach
    public void setUp() throws Exception {
        resourceService = Mockito.mock(ResourceService.class);
        resourcesValidationService = new ResourcesValidationServiceImpl(resourceService);
    }

    @Test
    public void shouldValidateResources() {
        Resource resource = new Resource();
        List<Resource> resources = Collections.singletonList(resource);
        List<Resource> sanitizedResources = resourcesValidationService.validateAndSanitize(resources);
        assertSame(resources, sanitizedResources);
    }

    @Test
    public void shouldNotUpdateWithInvalidResourceConfiguration() {
        assertThrows(InvalidDataException.class, () -> {
            Resource resource = new Resource();
            List<Resource> resources = Collections.singletonList(resource);

            doThrow(new InvalidDataException()).when(resourceService).validateResourceConfiguration(resource);
            resourcesValidationService.validateAndSanitize(resources);
        });
    }
}
