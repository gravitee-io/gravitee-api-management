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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.management.model.Environment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/environment-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Environment environment = new Environment();
        environment.setId("DEFAULT-create");
        environment.setCockpitId("cockpit-create");
        environment.setHrids(Arrays.asList("hrid1", "hrid2"));
        environment.setName("Default env for create");
        environment.setDescription("Default env description for create");
        environment.setOrganizationId("DEFAULT-ORG");

        final Environment createdEnv = environmentRepository.create(environment);

        assertEquals(environment.getId(), createdEnv.getId());
        assertEquals(environment.getCockpitId(), createdEnv.getCockpitId());
        assertEquals(environment.getHrids(), createdEnv.getHrids());
        assertEquals(environment.getName(), createdEnv.getName());
        assertEquals(environment.getDescription(), createdEnv.getDescription());
        assertEquals(environment.getOrganizationId(), createdEnv.getOrganizationId());

        Optional<Environment> optionalEnv = environmentRepository.findById("DEFAULT-create");
        Assert.assertTrue("Environment to create not found", optionalEnv.isPresent());
        assertEquals(environment.getId(), optionalEnv.get().getId());
        assertEquals(environment.getCockpitId(), optionalEnv.get().getCockpitId());
        assertEquals(environment.getHrids(), optionalEnv.get().getHrids());
        assertEquals(environment.getName(), optionalEnv.get().getName());
        assertEquals(environment.getDescription(), optionalEnv.get().getDescription());
        assertEquals(environment.getOrganizationId(), optionalEnv.get().getOrganizationId());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-update");
        Assert.assertTrue("Environment to update not found", optional.isPresent());
        assertEquals("Invalid saved Environment name.", "Default env for update", optional.get().getName());

        final Environment env = optional.get();
        env.setName("New name");
        env.setCockpitId("env#cockpit-new");

        final Environment fetchedEnvironment = environmentRepository.update(env);
        assertEquals(env.getName(), fetchedEnvironment.getName());
        assertEquals(env.getCockpitId(), fetchedEnvironment.getCockpitId());

        optional = environmentRepository.findById("DEFAULT-update");
        Assert.assertTrue("Environment to update not found", optional.isPresent());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-delete");
        Assert.assertTrue("Environment to delete not found", optional.isPresent());
        environmentRepository.delete("DEFAULT-delete");
        optional = environmentRepository.findById("DEFAULT-delete");
        Assert.assertFalse("Environment to delete has not been deleted", optional.isPresent());
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<Environment> optional = environmentRepository.findById("DEFAULT-findById");
        Assert.assertTrue("Environment to find not found", optional.isPresent());
    }

    @Test
    public void shouldFindAll() throws Exception {
        Set<Environment> allEnvironments = environmentRepository.findAll();
        Assert.assertTrue("No environment found", !allEnvironments.isEmpty());
    }

    @Test
    public void shouldFindByOrganization() throws Exception {
        Set<Environment> orgEnvironments = environmentRepository.findByOrganization("DEFAULT-ORG");
        Assert.assertTrue("No environment found", !orgEnvironments.isEmpty());
        Assert.assertEquals(1, orgEnvironments.size());
    }

    @Test
    public void shouldFindByOrganizationsAndHrids() throws Exception {
        Set<String> organizations = new HashSet<>();
        organizations.add("DEFAULT-ORG");
        organizations.add("ANOTHER-ORG");
        Set<String> hrids = new HashSet<>();
        hrids.add("def");
        hrids.add("find");
        Set<Environment> environments = environmentRepository.findByOrganizationsAndHrids(organizations, hrids);
        Assert.assertTrue("No environment found", !environments.isEmpty());
        Assert.assertEquals(2, environments.size());
    }

    @Test
    public void shouldFindByEmptyOrganizationsAndHrids() throws Exception {
        Set<String> organizations = new HashSet<>();
        Set<String> hrids = new HashSet<>();
        hrids.add("def");
        hrids.add("find");
        Set<Environment> environments = environmentRepository.findByOrganizationsAndHrids(organizations, hrids);
        Assert.assertTrue("No environment found", !environments.isEmpty());
        Assert.assertEquals(2, environments.size());
    }

    @Test
    public void shouldFindByOrganizationsAndEmptyHrids() throws Exception {
        Set<String> organizations = new HashSet<>();
        organizations.add("DEFAULT-ORG");
        Set<String> hrids = new HashSet<>();
        Set<Environment> environments = environmentRepository.findByOrganizationsAndHrids(organizations, hrids);
        Assert.assertTrue("No environment found", !environments.isEmpty());
        Assert.assertEquals(1, environments.size());
    }

    @Test
    public void shouldFindByEmptyOrganizationsAndEmptyHrids() throws Exception {
        Set<String> organizations = new HashSet<>();
        Set<String> hrids = new HashSet<>();
        Set<Environment> environments = environmentRepository.findByOrganizationsAndHrids(organizations, hrids);
        Assert.assertTrue("Environment found", environments.isEmpty());
    }

    @Test
    public void shouldFindByCockpitId() throws Exception {
        Optional<Environment> orgEnvironments = environmentRepository.findByCockpitId("cockpitId-findById");
        Assert.assertTrue("No environment found for cockpitId: cockpitId-findById", orgEnvironments.isPresent());
    }

    @Test
    public void shouldFindOrganizationIdsByEmptyEnvironments() throws Exception {
        Set<String> organizationIds = environmentRepository.findOrganizationIdsByEnvironments(Set.of());
        assertTrue("No organization ids found", organizationIds.isEmpty());
    }

    @Test
    public void shouldFindOrganizationIdsByEnvironments() throws Exception {
        Set<String> organizationIds = environmentRepository.findOrganizationIdsByEnvironments(Set.of("DEFAULT-update", "DEFAULT-findById"));
        assertEquals(2, organizationIds.size());
        assertTrue(organizationIds.stream().anyMatch(s -> s.equals("ANOTHER-ORG")));
        assertTrue(organizationIds.stream().anyMatch(s -> s.equals("DEFAULT-ORG")));
    }

    @Test
    public void shouldNotFindOrganizationIdsByWrongEnvironments() throws Exception {
        Set<String> organizationIds = environmentRepository.findOrganizationIdsByEnvironments(Set.of("wrong"));
        assertTrue("No organization ids found", organizationIds.isEmpty());
    }
}
