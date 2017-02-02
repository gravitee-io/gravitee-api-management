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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Tenant;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TenantRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/tenant-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Tenant> tenants = tenantRepository.findAll();

        assertNotNull(tenants);
        assertEquals(3, tenants.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Tenant tenant = new Tenant();
        tenant.setId("new-tenant");
        tenant.setName("Tenant name");
        tenant.setDescription("Description for the new tenant");

        int nbTenantsBeforeCreation = tenantRepository.findAll().size();
        tenantRepository.create(tenant);
        int nbTenantsAfterCreation = tenantRepository.findAll().size();

        Assert.assertEquals(nbTenantsBeforeCreation + 1, nbTenantsAfterCreation);

        Optional<Tenant> optional = tenantRepository.findById("new-tenant");
        Assert.assertTrue("Tenant saved not found", optional.isPresent());

        final Tenant tenantSaved = optional.get();
        Assert.assertEquals("Invalid saved tenant name.", tenant.getName(), tenantSaved.getName());
        Assert.assertEquals("Invalid tenant description.", tenant.getDescription(), tenantSaved.getDescription());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Tenant> optional = tenantRepository.findById("asia");
        Assert.assertTrue("Tenant to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved tenant name.", "Asia", optional.get().getName());

        final Tenant tenant = optional.get();
        tenant.setName("New tenant");
        tenant.setDescription("New description");

        int nbTenantsBeforeUpdate = tenantRepository.findAll().size();
        tenantRepository.update(tenant);
        int nbTenantsAfterUpdate = tenantRepository.findAll().size();

        Assert.assertEquals(nbTenantsBeforeUpdate, nbTenantsAfterUpdate);

        Optional<Tenant> optionalUpdated = tenantRepository.findById("asia");
        Assert.assertTrue("Tenant to update not found", optionalUpdated.isPresent());

        final Tenant tenantUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved tenant name.", "New tenant", tenantUpdated.getName());
        Assert.assertEquals("Invalid tenant description.", "New description", tenantUpdated.getDescription());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTenantsBeforeDeletion = tenantRepository.findAll().size();
        tenantRepository.delete("us");
        int nbTenantsAfterDeletion = tenantRepository.findAll().size();

        Assert.assertEquals(nbTenantsBeforeDeletion - 1, nbTenantsAfterDeletion);
    }
}
