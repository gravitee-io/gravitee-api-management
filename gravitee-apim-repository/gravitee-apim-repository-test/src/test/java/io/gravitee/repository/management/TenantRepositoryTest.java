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
import static org.junit.Assert.fail;

import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.management.model.TenantReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class TenantRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/tenant-tests/";
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final Set<Tenant> tenants = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION);

        assertThat(tenants).isNotNull().hasSize(3);
    }

    @Test
    public void shouldCreate() throws Exception {
        final Tenant tenant = new Tenant();
        tenant.setId("76d85918-0909-4054-9859-180909605419");
        tenant.setKey("new-tenant-key");
        tenant.setName("Tenant name");
        tenant.setDescription("Description for the new tenant");
        tenant.setReferenceId("DEFAULT");
        tenant.setReferenceType(TenantReferenceType.ORGANIZATION);

        int nbTenantsBeforeCreation = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();
        tenantRepository.create(tenant);
        int nbTenantsAfterCreation = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();

        assertThat(nbTenantsAfterCreation).isEqualTo(nbTenantsBeforeCreation + 1);

        Optional<Tenant> optional = tenantRepository.findById("76d85918-0909-4054-9859-180909605419");
        assertThat(optional).isPresent();

        final Tenant tenantSaved = optional.get();
        assertThat(tenantSaved.getName()).isEqualTo(tenant.getName());
        assertThat(tenantSaved.getDescription()).isEqualTo(tenant.getDescription());
        assertThat(tenantSaved.getKey()).isEqualTo(tenant.getKey());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Tenant> optional = tenantRepository.findById("e8a91421-4d32-4720-a914-214d32e720f4");
        assertThat(optional).isPresent();
        assertThat(optional.get().getName()).isEqualTo("Asia");

        final Tenant tenant = optional.get();
        tenant.setName("New tenant");
        tenant.setDescription("New description");
        tenant.setReferenceId("DEFAULT");
        tenant.setReferenceType(TenantReferenceType.ORGANIZATION);

        int nbTenantsBeforeUpdate = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();
        tenantRepository.update(tenant);
        int nbTenantsAfterUpdate = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();

        assertThat(nbTenantsAfterUpdate).isEqualTo(nbTenantsBeforeUpdate);

        Optional<Tenant> optionalUpdated = tenantRepository.findById("e8a91421-4d32-4720-a914-214d32e720f4");
        assertThat(optionalUpdated).isPresent();

        final Tenant tenantUpdated = optionalUpdated.get();
        assertThat(tenantUpdated.getName()).isEqualTo("New tenant");
        assertThat(tenantUpdated.getDescription()).isEqualTo("New description");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTenantsBeforeDeletion = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();
        tenantRepository.delete("89083315-9964-469b-8833-159964069b05");
        int nbTenantsAfterDeletion = tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION).size();

        assertThat(nbTenantsAfterDeletion).isEqualTo(nbTenantsBeforeDeletion - 1);
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
    public void shouldFindByKeyAndReference() throws Exception {
        final Optional<Tenant> tenant = tenantRepository.findByKeyAndReference("other-us", "OTHER", TenantReferenceType.ORGANIZATION);

        assertThat(tenant).isPresent();
        assertThat(tenant.get().getName()).isEqualTo("US");
        assertThat(tenant.get().getDescription()).isEqualTo("Description for other US tenant");
        assertThat(tenant.get().getKey()).isEqualTo("other-us");
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        assertThat(tenantRepository.findByReference("ToBeDeleted", TenantReferenceType.ORGANIZATION)).isNotEmpty();

        List<String> deleted = tenantRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", TenantReferenceType.ORGANIZATION);

        assertThat(deleted).hasSize(2);
        assertThat(tenantRepository.findByReference("ToBeDeleted", TenantReferenceType.ORGANIZATION)).isEmpty();
    }
}
