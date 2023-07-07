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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultApiHeaderInitializerTest {

    @Mock
    private ApiHeaderService apiHeaderService;

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private final DefaultApiHeaderInitializer initializer = new DefaultApiHeaderInitializer();

    @Test
    public void shouldInitializeDefaultHeaders() throws TechnicalException {
        when(apiHeaderService.findAll(anyString())).thenReturn(List.of());
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        initializer.initialize();
        verify(apiHeaderService, times(1)).initialize(any(ExecutionContext.class));
    }

    @Test
    public void shouldNotInitializeDefaultHeaders() throws TechnicalException {
        when(apiHeaderService.findAll(anyString())).thenReturn(List.of(new ApiHeaderEntity()));
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        initializer.initialize();
        verify(apiHeaderService, never()).initialize(any(ExecutionContext.class));
    }

    private static Environment environment() {
        Environment environment = new Environment();
        environment.setId("DEFAULT");
        return environment;
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.DEFAULT_API_HEADER_INITIALIZER, initializer.getOrder());
    }
}
