/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourcesValidationServiceImplTest {

    @Mock
    private ResourceService resourceService;

    private ResourcesValidationService resourcesValidationService;

    @Before
    public void setUp() throws Exception {
        resourceService = Mockito.mock(ResourceService.class);
        resourcesValidationService = new ResourcesValidationServiceImpl(resourceService);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdateWithInvalidResourceConfiguration() {
        Resource resource = new Resource();
        List<Resource> resources = Collections.singletonList(resource);

        doThrow(new InvalidDataException()).when(resourceService).validateResourceConfiguration(resource);
        resourcesValidationService.validateAndSanitize(resources);
    }
}
