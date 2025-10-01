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
import io.gravitee.rest.api.service.exceptions.DuplicateTenantNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TenantNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TenantServiceImpl extends TransactionalService implements TenantService {

    private final Logger LOGGER = LoggerFactory.getLogger(TenantServiceImpl.class);

    @Lazy
    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public TenantEntity findByIdAndReference(String tenantId, String referenceId, TenantReferenceType tenantReferenceType) {
        try {
            LOGGER.debug("Find tenant by ID: {}", tenantId);
            Optional<Tenant> optTenant = tenantRepository.findByIdAndReference(
                tenantId,
                referenceId,
                repoTenantReferenceType(tenantReferenceType)
            );

            if (!optTenant.isPresent()) {
                throw new TenantNotFoundException(tenantId);
            }

            return convert(optTenant.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find tenant by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find tenant by ID", ex);
        }
    }

    @Override
    public List<TenantEntity> findByReference(String referenceId, TenantReferenceType referenceType) {
        try {
            LOGGER.debug("Find all tenants");
            return tenantRepository
                .findByReference(referenceId, repoTenantReferenceType(referenceType))
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all tenants", ex);
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
        final List<String> tenantNames = tenantEntities.stream().map(NewTenantEntity::getName).collect(Collectors.toList());

        final Optional<TenantEntity> optionalTenant = findByReference(referenceId, referenceType)
            .stream()
            .filter(tenant -> tenantNames.contains(tenant.getName()))
            .findAny();

        if (optionalTenant.isPresent()) {
            throw new DuplicateTenantNameException(optionalTenant.get().getName());
        }

        final List<TenantEntity> savedTenants = new ArrayList<>(tenantEntities.size());
        tenantEntities.forEach(tenantEntity -> {
            try {
                Tenant tenant = convert(tenantEntity, referenceId, referenceType);
                savedTenants.add(convert(tenantRepository.create(tenant)));
                auditService.createAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Collections.singletonMap(TENANT, tenant.getId()))
                        .event(TENANT_CREATED)
                        .createdAt(new Date())
                        .oldValue(null)
                        .newValue(tenant)
                        .build()
                );
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to create tenant {}", tenantEntity.getName(), ex);
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
                Optional<Tenant> tenantOptional = tenantRepository.findByIdAndReference(
                    tenant.getId(),
                    referenceId,
                    repoTenantReferenceType(referenceType)
                );
                if (tenantOptional.isPresent()) {
                    Tenant existingTenant = tenantOptional.get();
                    tenant.setReferenceId(existingTenant.getReferenceId());
                    tenant.setReferenceType(existingTenant.getReferenceType());
                    savedTenants.add(convert(tenantRepository.update(tenant)));
                    auditService.createAuditLog(
                        executionContext,
                        AuditService.AuditLogData.builder()
                            .properties(Collections.singletonMap(TENANT, tenant.getId()))
                            .event(TENANT_UPDATED)
                            .createdAt(new Date())
                            .oldValue(tenantOptional.get())
                            .newValue(tenant)
                            .build()
                    );
                }
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to update tenant {}", tenantEntity.getName(), ex);
                throw new TechnicalManagementException("An error occurs while trying to update tenant " + tenantEntity.getName(), ex);
            }
        });
        return savedTenants;
    }

    @Override
    public void delete(ExecutionContext executionContext, final String tenantId, String referenceId, TenantReferenceType referenceType) {
        try {
            Optional<Tenant> tenantOptional = tenantRepository.findByIdAndReference(
                tenantId,
                referenceId,
                repoTenantReferenceType(referenceType)
            );
            if (tenantOptional.isPresent()) {
                tenantRepository.delete(tenantId);
                auditService.createAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Collections.singletonMap(TENANT, tenantId))
                        .event(TENANT_DELETED)
                        .createdAt(new Date())
                        .oldValue(null)
                        .newValue(tenantOptional.get())
                        .build()
                );
                tenantRepository.delete(tenantId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete tenant {}", tenantId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete tenant " + tenantId, ex);
        }
    }

    private Tenant convert(final NewTenantEntity tenantEntity, String referenceId, TenantReferenceType referenceType) {
        final Tenant tenant = new Tenant();
        tenant.setId(IdGenerator.generate(tenantEntity.getName()));
        tenant.setName(tenantEntity.getName());
        tenant.setDescription(tenantEntity.getDescription());
        tenant.setReferenceId(referenceId);
        tenant.setReferenceType(io.gravitee.repository.management.model.TenantReferenceType.valueOf(referenceType.name()));
        return tenant;
    }

    private Tenant convert(final UpdateTenantEntity tenantEntity) {
        final Tenant tenant = new Tenant();
        tenant.setId(tenantEntity.getId());
        tenant.setName(tenantEntity.getName());
        tenant.setDescription(tenantEntity.getDescription());
        return tenant;
    }

    private TenantEntity convert(final Tenant tenant) {
        final TenantEntity tenantEntity = new TenantEntity();
        tenantEntity.setId(tenant.getId());
        tenantEntity.setName(tenant.getName());
        tenantEntity.setDescription(tenant.getDescription());
        return tenantEntity;
    }

    private io.gravitee.repository.management.model.TenantReferenceType repoTenantReferenceType(TenantReferenceType referenceType) {
        return io.gravitee.repository.management.model.TenantReferenceType.valueOf(referenceType.name());
    }
}
