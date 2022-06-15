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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Organization;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class OrganizationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/organization-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Organization organization = new Organization();
        organization.setId("DEFAULT-ORG-create");
        organization.setCockpitId("cockpit-org-create");
        organization.setHrids(Arrays.asList("hrid1", "hrid2"));
        organization.setName("Default org for create");
        organization.setDescription("Default org description for create");
        organization.setDomainRestrictions(Arrays.asList("domain", "restriction"));
        organization.setFlowMode("BEST_MATCH");

        final Organization createdOrg = organizationRepository.create(organization);

        assertEquals(organization.getId(), createdOrg.getId());
        assertEquals(organization.getCockpitId(), createdOrg.getCockpitId());
        assertEquals(organization.getName(), createdOrg.getName());
        assertEquals(organization.getDescription(), createdOrg.getDescription());
        assertEquals(organization.getFlowMode(), createdOrg.getFlowMode());
        assertEquals(organization.getHrids(), createdOrg.getHrids());
        List<String> domainRestrictions = createdOrg.getDomainRestrictions();
        assertNotNull(domainRestrictions);
        assertEquals(2, domainRestrictions.size());
        assertTrue(domainRestrictions.contains("domain"));
        assertTrue(domainRestrictions.contains("restriction"));

        Optional<Organization> optionalOrg = organizationRepository.findById("DEFAULT-ORG-create");
        Assert.assertTrue("Organization to create not found", optionalOrg.isPresent());
        assertEquals(organization.getId(), optionalOrg.get().getId());
        assertEquals(organization.getCockpitId(), optionalOrg.get().getCockpitId());
        assertEquals(organization.getName(), optionalOrg.get().getName());
        assertEquals(organization.getDescription(), optionalOrg.get().getDescription());
        assertEquals(organization.getFlowMode(), optionalOrg.get().getFlowMode());
        assertEquals(organization.getHrids(), optionalOrg.get().getHrids());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-update");
        Assert.assertTrue("Organization to update not found", optional.isPresent());
        assertEquals("Invalid saved Organization name.", "Default org for update", optional.get().getName());

        final Organization org = optional.get();
        org.setName("New name");
        org.setCockpitId("org#cockpit-new");
        org.setDescription("New description");
        org.setDomainRestrictions(Collections.singletonList("New domain restriction"));
        org.setHrids(Collections.singletonList("New hrid"));
        org.setFlowMode("DEFAULT");

        final Organization fetchedOrganization = organizationRepository.update(org);
        assertEquals(org.getName(), fetchedOrganization.getName());
        assertEquals(org.getCockpitId(), fetchedOrganization.getCockpitId());
        assertEquals(org.getDescription(), fetchedOrganization.getDescription());
        assertEquals(org.getHrids(), fetchedOrganization.getHrids());
        assertEquals(org.getDomainRestrictions(), fetchedOrganization.getDomainRestrictions());
        assertEquals(org.getFlowMode(), fetchedOrganization.getFlowMode());

        optional = organizationRepository.findById("DEFAULT-ORG-update");
        Assert.assertTrue("Organization to update not found", optional.isPresent());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-delete");
        Assert.assertTrue("Organization to delete not found", optional.isPresent());
        organizationRepository.delete("DEFAULT-ORG-delete");
        optional = organizationRepository.findById("DEFAULT-ORG-delete");
        Assert.assertFalse("Organization to delete has not been deleted", optional.isPresent());
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<Organization> optional = organizationRepository.findById("DEFAULT-ORG-findById");
        Assert.assertTrue("Organization to find not found", optional.isPresent());
    }

    @Test
    public void shouldCount() throws Exception {
        final long count = organizationRepository.count();
        // Should count 4 organizations (DEFAULT-ORG-create, DEFAULT-ORG-update, DEFAULT-ORG-findById and DEFAULT)
        Assert.assertEquals("Organization count should be 4", 4L, count);
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Collection<Organization> organizations = organizationRepository.findAll();
        // Should count 4 organizations (DEFAULT-ORG-create, DEFAULT-ORG-update, DEFAULT-ORG-findById and DEFAULT)
        Assert.assertEquals("Organization count should be 4", 4L, organizations.size());
    }

    @Test
    public void shouldFindByHrids() throws Exception {
        final HashSet<String> hrids = new HashSet<>();
        hrids.add("def");
        hrids.add("ate");
        final Set<Organization> organizations = organizationRepository.findByHrids(hrids);
        Assert.assertTrue("No organization found", !organizations.isEmpty());
        Assert.assertEquals(2, organizations.size());
    }
}
