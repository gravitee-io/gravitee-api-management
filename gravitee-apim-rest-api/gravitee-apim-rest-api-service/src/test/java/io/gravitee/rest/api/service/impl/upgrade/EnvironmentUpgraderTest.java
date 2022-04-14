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
package io.gravitee.rest.api.service.impl.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentUpgraderTest {

    @InjectMocks
    @Spy
    private EnvironmentUpgrader upgrader = new EnvironmentUpgrader() {
        protected void upgradeEnvironment(ExecutionContext executionContext) {}

        public int getOrder() {
            return 0;
        }
    };

    @Mock
    private EnvironmentRepository environmentRepository;

    @Test
    public void upgrade_should_read_all_environments() throws Exception {
        upgrader.upgrade(null);

        verify(environmentRepository, times(1)).findAll();
    }

    @Test
    public void upgrade_should_call_upgradeEnvironment_for_each_environment() throws Exception {
        when(environmentRepository.findAll())
            .thenReturn(
                Set.of(
                    buildTestEnvironment("env1", "org1"),
                    buildTestEnvironment("env2", "org2"),
                    buildTestEnvironment("env3", "org1"),
                    buildTestEnvironment("env4", "org3")
                )
            );

        upgrader.upgrade(null);

        verify(upgrader, times(1))
            .upgradeEnvironment(argThat(e -> e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")));
        verify(upgrader, times(1))
            .upgradeEnvironment(argThat(e -> e.getEnvironmentId().equals("env2") && e.getOrganizationId().equals("org2")));
        verify(upgrader, times(1))
            .upgradeEnvironment(argThat(e -> e.getEnvironmentId().equals("env3") && e.getOrganizationId().equals("org1")));
        verify(upgrader, times(1))
            .upgradeEnvironment(argThat(e -> e.getEnvironmentId().equals("env4") && e.getOrganizationId().equals("org3")));
    }

    @Test
    public void upgrade_should_return_true_when_no_technicalException() throws Exception {
        boolean result = upgrader.upgrade(null);

        assertTrue(result);
    }

    @Test
    public void upgrade_should_return_false_when_technicalException() throws Exception {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        boolean result = upgrader.upgrade(null);

        assertFalse(result);
    }

    private Environment buildTestEnvironment(String environmentId, String organizationId) {
        Environment environment = new Environment();
        environment.setId(environmentId);
        environment.setOrganizationId(organizationId);
        return environment;
    }
}
