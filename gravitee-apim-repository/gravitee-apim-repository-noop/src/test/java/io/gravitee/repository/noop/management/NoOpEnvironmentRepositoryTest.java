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
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.HashSet;
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
    public void should_return_empty_when_find_by_an_organization_other_than_default() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganization("test_org");

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
    }

    @Test
    public void should_return_default_environment_when_finding_by_default_organization() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganization(Organization.DEFAULT.getId());

        assertNotNull(environments);
        assertEquals(Set.of(Environment.DEFAULT), environments);
    }

    @Test
    public void should_return_empty_when_finding_by_an_organization_hrid_other_than_default() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganizationsAndHrids(Set.of("test_org"), Set.of("test_hrid"));

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
    }

    @Test
    public void should_return_empty_when_finding_by_an_organization_other_than_default_and_default_environment() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganizationsAndHrids(Set.of("test_org"), new HashSet<>(Environment.DEFAULT.getHrids()));

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
    }

    @Test
    public void should_return_empty_when_finding_by_default_organization_but_an_environment_other_than_default() throws TechnicalException {
        Set<Environment> environments = cut.findByOrganizationsAndHrids(
            new HashSet<>(Organization.DEFAULT.getHrids()),
            Set.of("test_hrid")
        );

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
    }

    @Test
    public void should_return_default_environment_when_finding_by_default_organization_and_default_environment_hrids()
        throws TechnicalException {
        Set<Environment> environments = cut.findByOrganizationsAndHrids(
            new HashSet<>(Organization.DEFAULT.getHrids()),
            new HashSet<>(Environment.DEFAULT.getHrids())
        );

        assertNotNull(environments);
        assertEquals(Set.of(Environment.DEFAULT), environments);
    }

    @Test
    public void should_return_default_environment_when_finding_all() throws TechnicalException {
        Set<Environment> environments = cut.findAll();

        assertNotNull(environments);
        assertEquals(Set.of(Environment.DEFAULT), environments);
    }

    @Test
    public void should_return_default_environment_when_finding_by_default_environment() throws TechnicalException {
        Optional<Environment> environmentOpt = cut.findById(Environment.DEFAULT.getId());

        assertNotNull(environmentOpt);
        assertTrue(environmentOpt.isPresent());
        assertEquals(Environment.DEFAULT, environmentOpt.get());
    }

    @Test
    public void should_return_empty_when_finding_by_an_environment() throws TechnicalException {
        Optional<Environment> environmentOpt = cut.findById("test_id");

        assertNotNull(environmentOpt);
        assertFalse(environmentOpt.isPresent());
    }

    @Test
    public void should_return_empty_when_finding_cockpit_id() throws TechnicalException {
        Optional<Environment> environment = cut.findByCockpitId("test_id");

        assertNotNull(environment);
        assertTrue(environment.isEmpty());
    }
}
