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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.management.model.TenantReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class TenantRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/tenant-tests/";
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final Set<Tenant> tenants = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION);

        assertNotNull(tenants);
        assertEquals(3, tenants.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Tenant tenant = new Tenant();
        tenant.setId("new-tenant");
        tenant.setName("Tenant name");
        tenant.setDescription("Description for the new tenant");
        tenant.setReferenceId("DEFAULT");
        tenant.setReferenceType(TenantReferenceType.ORGANIZATION);

        int nbTenantsBeforeCreation = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();
        tenantRepository.create(tenant);
        int nbTenantsAfterCreation = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();

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
        tenant.setReferenceId("DEFAULT");
        tenant.setReferenceType(TenantReferenceType.ORGANIZATION);

        int nbTenantsBeforeUpdate = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();
        tenantRepository.update(tenant);
        int nbTenantsAfterUpdate = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();

        Assert.assertEquals(nbTenantsBeforeUpdate, nbTenantsAfterUpdate);

        Optional<Tenant> optionalUpdated = tenantRepository.findById("asia");
        Assert.assertTrue("Tenant to update not found", optionalUpdated.isPresent());

        final Tenant tenantUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved tenant name.", "New tenant", tenantUpdated.getName());
        Assert.assertEquals("Invalid tenant description.", "New description", tenantUpdated.getDescription());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTenantsBeforeDeletion = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();
        tenantRepository.delete("us");
        int nbTenantsAfterDeletion = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();

        Assert.assertEquals(nbTenantsBeforeDeletion - 1, nbTenantsAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownTenant() throws Exception {
        Tenant unknownTenant = new Tenant();
        unknownTenant.setId("unknown");
        tenantRepository.update(unknownTenant);
        fail("An unknown tenant should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        tenantRepository.update(null);
        fail("A null tenant should not be updated");
    }

    @Test
    public void shouldFindByIdAndReference() throws Exception {
        final Optional<Tenant> tenant = tenantRepository.findByIdAndReference("other-us", "OTHER", TenantReferenceType.ORGANIZATION);

        assertTrue(tenant.isPresent());
        assertEquals("US", tenant.get().getName());
        assertEquals("Description for other US tenant", tenant.get().getDescription());
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        assertThat(tenantRepository.findByReference("ToBeDeleted", TenantReferenceType.ORGANIZATION)).isNotEmpty();

        List<String> deleted = tenantRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", TenantReferenceType.ORGANIZATION);

        assertEquals(2, deleted.size());
        assertThat(tenantRepository.findByReference("ToBeDeleted", TenantReferenceType.ORGANIZATION)).isEmpty();
    }
}
