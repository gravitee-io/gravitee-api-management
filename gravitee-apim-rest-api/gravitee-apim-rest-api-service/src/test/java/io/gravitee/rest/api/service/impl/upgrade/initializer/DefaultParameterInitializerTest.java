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
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DefaultParameterInitializerTest {

    private static final String PORTAL_URL = "apis.example.com";

    @Mock
    private ParameterRepository parameterRepository;

    private DefaultParameterInitializer initializer;

    @BeforeEach
    public void setUp() throws Exception {
        initializer = new DefaultParameterInitializer(parameterRepository, new MockEnvironment().withProperty("portalURL", PORTAL_URL));
    }

    @Test
    public void shouldSetPortalURLWithNoExistingParameter() throws TechnicalException {
        when(parameterRepository.findById(any(), any(), any())).thenReturn(Optional.empty());

        initializer.initialize();

        verify(parameterRepository, times(1)).create(argThat(param -> param.getValue().equals(PORTAL_URL)));
    }

    @Test
    public void shouldSetPortalURLWithExistingParameter() throws TechnicalException {
        when(parameterRepository.findById(any(), any(), any())).thenReturn(Optional.of(new Parameter()));

        initializer.initialize();

        verify(parameterRepository, times(1)).update(argThat(param -> param.getValue().equals(PORTAL_URL)));
    }

    @Test
    public void testOrder() {
        Assertions.assertEquals(InitializerOrder.DEFAULT_PARAMETER_INITIALIZER, initializer.getOrder());
    }
}
