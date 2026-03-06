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

import static io.gravitee.repository.management.model.Audit.AuditProperties.TENANT;
import static io.gravitee.repository.management.model.Tenant.AuditEvent.TENANT_CREATED;
import static io.gravitee.repository.management.model.Tenant.AuditEvent.TENANT_DELETED;
import static io.gravitee.repository.management.model.Tenant.AuditEvent.TENANT_UPDATED;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.rest.api.model.NewTenantEntity;
import io.gravitee.rest.api.model.TenantEntity;
import io.gravitee.rest.api.model.TenantReferenceType;
import io.gravitee.rest.api.model.UpdateTenantEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.TenantService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DuplicateTenantKeyException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TenantNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class TenantServiceImpl extends TransactionalService implements TenantService {

    @Lazy
    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public TenantEntity findByKeyAndReference(String tenant, String referenceId, TenantReferenceType tenantReferenceType) {
        try {
            log.debug("Find tenant by key: {}", tenant);
            Optional<Tenant> optTenant = tenantRepository.findByKeyAndReference(
                tenant,
                referenceId,
                repoTenantReferenceType(tenantReferenceType)
            );

            if (!optTenant.isPresent()) {
                throw new TenantNotFoundException(tenant);
            }

            return convert(optTenant.get());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find tenant by ID", ex);
        }
    }

    @Override
    public List<TenantEntity> findByReference(String referenceId, TenantReferenceType referenceType) {
        try {
            log.debug("Find all tenants");
            return tenantRepository
                .findByReference(referenceId, repoTenantReferenceType(referenceType))
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find all tenants", ex);
        }
    }

    @Override
    public List<TenantEntity> create(
        ExecutionContext executionContext,
        final List<NewTenantEntity> tenantEntities,
        String referenceId,
        TenantReferenceType referenceType
    ) {
        // First we prevent the duplicate tenant name
        final List<String> tenantKeys = tenantEntities.stream().map(NewTenantEntity::getKey).collect(Collectors.toList());

        final Optional<TenantEntity> optionalTenant = findByReference(referenceId, referenceType)
            .stream()
            .filter(tenant -> tenantKeys.contains(tenant.getKey()))
            .findAny();

        if (optionalTenant.isPresent()) {
            throw new DuplicateTenantKeyException(optionalTenant.get().getName());
        }

        final List<TenantEntity> savedTenants = new ArrayList<>(tenantEntities.size());
        tenantEntities.forEach(tenantEntity -> {
            try {
                Tenant tenant = convert(tenantEntity, referenceId, referenceType);
                savedTenants.add(convert(tenantRepository.create(tenant)));
                auditService.createAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Collections.singletonMap(TENANT, tenant.getKey()))
                        .event(TENANT_CREATED)
                        .createdAt(new Date())
                        .oldValue(null)
                        .newValue(tenant)
                        .build()
                );
            } catch (TechnicalException ex) {
                throw new TechnicalManagementException("An error occurs while trying to create tenant " + tenantEntity.getName(), ex);
            }
        });
        return savedTenants;
    }

    @Override
    public List<TenantEntity> update(
        ExecutionContext executionContext,
        final List<UpdateTenantEntity> tenantEntities,
        String referenceId,
        TenantReferenceType referenceType
    ) {
        final List<TenantEntity> savedTenants = new ArrayList<>(tenantEntities.size());
        tenantEntities.forEach(tenantEntity -> {
            try {
                Tenant tenant = convert(tenantEntity);
                Optional<Tenant> tenantOptional = tenantRepository.findByKeyAndReference(
                    tenant.getKey(),
                    referenceId,
                    repoTenantReferenceType(referenceType)
                );
                if (tenantOptional.isPresent()) {
                    Tenant existingTenant = tenantOptional.get();
                    tenant.setId(existingTenant.getId());
                    tenant.setReferenceId(existingTenant.getReferenceId());
                    tenant.setReferenceType(existingTenant.getReferenceType());
                    savedTenants.add(convert(tenantRepository.update(tenant)));
                    auditService.createAuditLog(
                        executionContext,
                        AuditService.AuditLogData.builder()
                            .properties(Collections.singletonMap(TENANT, tenant.getKey()))
                            .event(TENANT_UPDATED)
                            .createdAt(new Date())
                            .oldValue(tenantOptional.get())
                            .newValue(tenant)
                            .build()
                    );
                }
            } catch (TechnicalException ex) {
                throw new TechnicalManagementException("An error occurs while trying to update tenant " + tenantEntity.getName(), ex);
            }
        });
        return savedTenants;
    }

    @Override
    public void delete(ExecutionContext executionContext, final String tenantKey, String referenceId, TenantReferenceType referenceType) {
        try {
            Optional<Tenant> tenantOptional = tenantRepository.findByKeyAndReference(
                tenantKey,
                referenceId,
                repoTenantReferenceType(referenceType)
            );
            if (tenantOptional.isPresent()) {
                tenantRepository.delete(tenantOptional.get().getId());
                auditService.createAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Collections.singletonMap(TENANT, tenantKey))
                        .event(TENANT_DELETED)
                        .createdAt(new Date())
                        .oldValue(null)
                        .newValue(tenantOptional.get())
                        .build()
                );
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete tenant " + tenantKey, ex);
        }
    }

    private Tenant convert(final NewTenantEntity tenantEntity, String referenceId, TenantReferenceType referenceType) {
        final Tenant tenant = new Tenant();
        tenant.setId(UUID.random().toString());
        tenant.setKey(IdGenerator.generate(tenantEntity.getKey()));
        tenant.setName(tenantEntity.getName());
        tenant.setDescription(tenantEntity.getDescription());
        tenant.setReferenceId(referenceId);
        tenant.setReferenceType(io.gravitee.repository.management.model.TenantReferenceType.valueOf(referenceType.name()));
        return tenant;
    }

    private Tenant convert(final UpdateTenantEntity tenantEntity) {
        final Tenant tenant = new Tenant();
        tenant.setKey(tenantEntity.getKey());
        tenant.setName(tenantEntity.getName());
        tenant.setDescription(tenantEntity.getDescription());
        return tenant;
    }

    private TenantEntity convert(final Tenant tenant) {
        final TenantEntity tenantEntity = new TenantEntity();
        tenantEntity.setId(tenant.getId());
        tenantEntity.setKey(tenant.getKey());
        tenantEntity.setName(tenant.getName());
        tenantEntity.setDescription(tenant.getDescription());
        return tenantEntity;
    }

    private io.gravitee.repository.management.model.TenantReferenceType repoTenantReferenceType(TenantReferenceType referenceType) {
        return io.gravitee.repository.management.model.TenantReferenceType.valueOf(referenceType.name());
    }
}
