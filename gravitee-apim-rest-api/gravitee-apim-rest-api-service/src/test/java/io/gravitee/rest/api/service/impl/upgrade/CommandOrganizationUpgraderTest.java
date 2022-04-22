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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.Environment;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandOrganizationUpgraderTest {

    @Mock
    CommandRepository commandRepository;

    @Mock
    EnvironmentRepository environmentRepository;

    @InjectMocks
    CommandOrganizationUpgrader upgrader;

    @Test
    public void shouldSetCommandOrganizationIds() throws Exception {
        final Environment env1 = new Environment();
        env1.setId("env-1");
        env1.setOrganizationId("org-1");

        final Environment env2 = new Environment();
        env2.setOrganizationId("org-2");
        env2.setId("env-2");

        when(environmentRepository.findAll()).thenReturn(Set.of(env1, env2));

        final Command cmd1 = new Command();
        final Command cmd2 = new Command();

        when(commandRepository.search(any()))
            .thenAnswer(
                answer -> {
                    CommandCriteria criteria = answer.getArgument(0);
                    return "env-1".equals(criteria.getEnvironmentId()) ? List.of(cmd1) : List.of(cmd2);
                }
            );

        upgrader.processOneShotUpgrade();

        assertEquals("org-1", cmd1.getOrganizationId());
        assertEquals("org-2", cmd2.getOrganizationId());
        verify(commandRepository, times(2)).update(any());
    }
}
