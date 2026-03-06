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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TenantKeyUpgraderTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantKeyUpgrader upgrader;

    @Test
    public void shouldMigrateTenants() throws Exception {
        var tenant = new Tenant();
        tenant.setId("functional-id");
        tenant.setName("Tenant Name");

        when(tenantRepository.findAll()).thenReturn(Collections.singleton(tenant));
        assertThat(upgrader.upgrade()).isTrue();

        var tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).create(tenantCaptor.capture());
        verify(tenantRepository).delete("functional-id");

        var migratedTenant = tenantCaptor.getValue();
        assertThat(migratedTenant.getKey()).isEqualTo("functional-id");
        assertThat(migratedTenant.getId()).isNotEqualTo("functional-id").matches("^[0-9a-fA-F-]{36}$");
        assertThat(migratedTenant.getName()).isEqualTo("Tenant Name");
    }

    @Test
    public void shouldReturnFalseOnException() throws Exception {
        when(tenantRepository.findAll()).thenThrow(new RuntimeException());
        assertThat(upgrader.upgrade()).isFalse();
    }
}
