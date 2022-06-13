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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentService_GetDefaultOrInitializeTest {

    @InjectMocks
    private EnvironmentServiceImpl environmentService = new EnvironmentServiceImpl();

    @Mock
    private EnvironmentRepository mockEnvironmentRepository;

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnDefaultEnvironment() throws TechnicalException {
        Environment existingDefault = new Environment();
        existingDefault.setId(GraviteeContext.getDefaultEnvironment());
        when(mockEnvironmentRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenReturn(Optional.of(existingDefault));

        EnvironmentEntity environment = environmentService.getDefaultOrInitialize();

        assertThat(environment.getId()).isEqualTo(GraviteeContext.getDefaultEnvironment());
    }

    @Test
    public void shouldCreateDefaultEnvironment() throws TechnicalException {
        when(mockEnvironmentRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenReturn(Optional.empty());

        EnvironmentEntity environment = environmentService.getDefaultOrInitialize();

        assertThat(environment.getId()).isEqualTo(GraviteeContext.getDefaultEnvironment());
        assertThat(environment.getName()).isEqualTo("Default environment");
        assertThat(environment.getHrids()).isEqualTo(Collections.singletonList("default"));
        assertThat(environment.getDescription()).isEqualTo("Default environment");
        assertThat(environment.getOrganizationId()).isEqualTo(GraviteeContext.getDefaultOrganization());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldCatchExceptionIfThrow() throws TechnicalException {
        when(mockEnvironmentRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenThrow(new TechnicalException(""));

        environmentService.getDefaultOrInitialize();
    }
}
