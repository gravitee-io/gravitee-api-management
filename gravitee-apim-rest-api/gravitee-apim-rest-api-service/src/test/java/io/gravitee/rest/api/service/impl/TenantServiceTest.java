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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.rest.api.model.NewTenantEntity;
import io.gravitee.rest.api.model.TenantReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DuplicateTenantKeyException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {

    private static final String REFERENCE_ID = "DEFAULT";
    private static final TenantReferenceType REFERENCE_TYPE = TenantReferenceType.ORGANIZATION;

    @InjectMocks
    private TenantServiceImpl tenantService = new TenantServiceImpl();

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuditService auditService;

    @Test
    public void should_create_tenant_when_key_does_not_exist() throws TechnicalException {
        var newKey = "new-tenant-key";
        var newTenant = new NewTenantEntity();
        newTenant.setKey(newKey);
        newTenant.setName("New Tenant Name");
        newTenant.setDescription("New Tenant Description");

        var executionContext = new ExecutionContext(REFERENCE_ID, null);

        when(
            tenantRepository.findByReference(REFERENCE_ID, io.gravitee.repository.management.model.TenantReferenceType.ORGANIZATION)
        ).thenReturn(Collections.emptySet());

        var savedTenant = new Tenant();
        savedTenant.setId("new-id");
        savedTenant.setKey(newKey);
        savedTenant.setName(newTenant.getName());
        savedTenant.setReferenceId(REFERENCE_ID);
        savedTenant.setReferenceType(io.gravitee.repository.management.model.TenantReferenceType.ORGANIZATION);

        when(tenantRepository.create(any())).thenReturn(savedTenant);

        var result = tenantService.create(executionContext, List.of(newTenant), REFERENCE_ID, REFERENCE_TYPE);

        assertThat(result).isNotNull().hasSize(1);
        var resultTenant = result.getFirst();
        assertThat(resultTenant.getKey()).isEqualTo(newKey);
        assertThat(resultTenant.getName()).isEqualTo(newTenant.getName());

        var tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).create(tenantCaptor.capture());
        var tenantToCreate = tenantCaptor.getValue();
        assertThat(tenantToCreate.getKey()).isEqualTo(newKey);
        assertThat(tenantToCreate.getName()).isEqualTo(newTenant.getName());
        assertThat(tenantToCreate.getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(tenantToCreate.getReferenceType()).isEqualTo(io.gravitee.repository.management.model.TenantReferenceType.ORGANIZATION);

        verify(auditService).createAuditLog(eq(executionContext), any());
    }

    @Test
    public void should_throw_DuplicateTenantKeyException_when_key_already_exists() throws TechnicalException {
        var existingKey = "existing-tenant-key";
        var newTenant = new NewTenantEntity();
        newTenant.setKey(existingKey);
        newTenant.setName("New Tenant Name");

        var existingTenant = new Tenant();
        existingTenant.setId("existing-id");
        existingTenant.setKey(existingKey);
        existingTenant.setName("Existing Tenant Name");
        existingTenant.setReferenceId(REFERENCE_ID);
        existingTenant.setReferenceType(io.gravitee.repository.management.model.TenantReferenceType.ORGANIZATION);

        when(
            tenantRepository.findByReference(REFERENCE_ID, io.gravitee.repository.management.model.TenantReferenceType.ORGANIZATION)
        ).thenReturn(Set.of(existingTenant));

        assertThatThrownBy(() ->
            tenantService.create(new ExecutionContext(REFERENCE_ID, null), List.of(newTenant), REFERENCE_ID, REFERENCE_TYPE)
        ).isInstanceOf(DuplicateTenantKeyException.class);

        verify(tenantRepository, never()).create(any());
        verifyNoInteractions(auditService);
    }
}
