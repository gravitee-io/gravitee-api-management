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
package io.gravitee.repository.mock.management;

import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.management.model.TenantReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TenantRepositoryMock extends AbstractRepositoryMock<TenantRepository> {

    public TenantRepositoryMock() {
        super(TenantRepository.class);
    }

    @Override
    protected void prepare(TenantRepository tenantRepository) throws Exception {
        final Tenant tenant = mock(Tenant.class);
        when(tenant.getName()).thenReturn("Tenant name");
        when(tenant.getDescription()).thenReturn("Description for the new tenant");
        when(tenant.getReferenceId()).thenReturn("DEFAULT");
        when(tenant.getReferenceType()).thenReturn(TenantReferenceType.ORGANIZATION);

        final Tenant tenant2 = mock(Tenant.class);
        when(tenant2.getId()).thenReturn("tenant");
        when(tenant2.getName()).thenReturn("Asia");
        when(tenant.getReferenceId()).thenReturn("DEFAULT");
        when(tenant.getReferenceType()).thenReturn(TenantReferenceType.ORGANIZATION);

        final Tenant tenant2Updated = mock(Tenant.class);
        when(tenant2Updated.getName()).thenReturn("New tenant");
        when(tenant2Updated.getDescription()).thenReturn("New description");
        when(tenant.getReferenceId()).thenReturn("DEFAULT");
        when(tenant.getReferenceType()).thenReturn(TenantReferenceType.ORGANIZATION);

        final Set<Tenant> tenants = newSet(tenant, tenant2, mock(Tenant.class));
        final Set<Tenant> tenantsAfterDelete = newSet(tenant, tenant2);
        final Set<Tenant> tenantsAfterAdd = newSet(tenant, tenant2, mock(Tenant.class), mock(Tenant.class));

        when(tenantRepository.findByReference("DEFAULT", TenantReferenceType.ORGANIZATION))
            .thenReturn(tenants, tenantsAfterAdd, tenants, tenantsAfterDelete, tenants);

        when(tenantRepository.create(any(Tenant.class))).thenReturn(tenant);

        when(tenantRepository.findById("new-tenant")).thenReturn(of(tenant));
        when(tenantRepository.findById("asia")).thenReturn(of(tenant2), of(tenant2Updated));

        when(tenantRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        final Tenant tenant3 = mock(Tenant.class);
        when(tenant3.getName()).thenReturn("US");
        when(tenant3.getDescription()).thenReturn("Description for other US tenant");

        when(tenantRepository.findByIdAndReference("other-us", "OTHER", TenantReferenceType.ORGANIZATION)).thenReturn(Optional.of(tenant3));
    }
}
