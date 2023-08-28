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
package io.gravitee.repository.noop.management;

import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpEnvironmentRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private EnvironmentRepository cut;

    @Test
    public void findByOrganization() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganization("test_org");

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
    }

    @Test
    public void findByOrganizationsAndHrids() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganizationsAndHrids(Set.of("test_org"), Set.of("test_hrid"));

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
    }

    @Test
    public void findByCockpit() throws TechnicalException {
        Optional<Environment> environment = cut.findByCockpit("test_id");

        assertNotNull(environment);
        assertTrue(environment.isEmpty());
    }
}
