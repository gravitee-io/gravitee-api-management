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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DefaultEnvironmentUpgraderTest {

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private final DefaultEnvironmentUpgrader upgrader = new DefaultEnvironmentUpgrader();

    @Test
    public void upgrade_should_failed_because_of_exception() throws TechnicalException {
        assertThrows(UpgraderException.class, () -> {
            when(environmentService.findByOrganization(any())).thenThrow(new RuntimeException());

            upgrader.upgrade();

            verify(environmentService, times(1)).findByOrganization(any());
            verifyNoMoreInteractions(environmentService);
        });
    }

    @Test
    public void shouldCreateDefaultEnvironment() throws UpgraderException {
        when(environmentService.findByOrganization(any())).thenReturn(List.of());

        upgrader.upgrade();

        verify(environmentService, times(1)).initialize();
    }

    @Test
    public void test_order() {
        Assertions.assertEquals(UpgraderOrder.DEFAULT_ENVIRONMENT_UPGRADER, upgrader.getOrder());
    }
}
