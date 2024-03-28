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
import io.gravitee.repository.management.api.OrganizationRepository;
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
public class NoOpOrganizationRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private OrganizationRepository cut;

    @Test
    public void should_return_default_organization_when_finding_by_default_organization_hrid() throws TechnicalException {
        Set<Organization> organizations = cut.findByHrids(new HashSet<>(Organization.DEFAULT.getHrids()));

        assertNotNull(organizations);
        assertEquals(Set.of(Organization.DEFAULT), organizations);
    }

    @Test
    public void should_return_empty_when_finding_by_an_organization_hrid_other_than_default() throws TechnicalException {
        Set<Organization> organizations = cut.findByHrids(Set.of("test_org"));

        assertNotNull(organizations);
        assertTrue(organizations.isEmpty());
    }

    @Test
    public void should_return_default_organization_when_finding_all() throws TechnicalException {
        Set<Organization> organizations = cut.findAll();

        assertNotNull(organizations);
        assertEquals(Set.of(Organization.DEFAULT), organizations);
    }

    @Test
    public void should_return_default_organization_when_finding_by_default_organization() throws TechnicalException {
        Optional<Organization> organizationOpt = cut.findById(Organization.DEFAULT.getId());

        assertNotNull(organizationOpt);
        assertTrue(organizationOpt.isPresent());
        assertEquals(Organization.DEFAULT, organizationOpt.get());
    }

    @Test
    public void should_return_empty_when_finding_by_an_organization() throws TechnicalException {
        Optional<Organization> organizationOpt = cut.findById("test_id");

        assertNotNull(organizationOpt);
        assertFalse(organizationOpt.isPresent());
    }

    @Test
    public void should_return_empty_when_finding_cockpit_id() throws TechnicalException {
        Optional<Organization> organization = cut.findByCockpitId("test_id");

        assertNotNull(organization);
        assertTrue(organization.isEmpty());
    }

    @Test
    public void should_count_1_organization() throws TechnicalException {
        assertEquals(1L, (long) cut.count());
    }
}
