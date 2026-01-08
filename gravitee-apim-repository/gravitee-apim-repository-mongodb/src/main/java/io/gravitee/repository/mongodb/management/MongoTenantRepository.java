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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.management.model.TenantReferenceType;
import io.gravitee.repository.mongodb.management.internal.api.TenantMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.TenantMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoTenantRepository implements TenantRepository {

    @Autowired
    private TenantMongoRepository internalTenantRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Tenant> findById(String tenantId) throws TechnicalException {
        log.debug("Find tenant by ID [{}]", tenantId);

        final TenantMongo tenant = internalTenantRepo.findById(tenantId).orElse(null);

        log.debug("Find tenant by ID [{}] - Done", tenantId);
        return Optional.ofNullable(mapper.map(tenant));
    }

    @Override
    public Optional<Tenant> findByIdAndReference(String tenantId, String referenceId, TenantReferenceType referenceType) {
        log.debug("Find tenant by ID [{}]", tenantId);

        final TenantMongo tenant = internalTenantRepo
            .findByIdAndReferenceIdAndReferenceType(tenantId, referenceId, referenceType)
            .orElse(null);

        log.debug("Find tenant by ID [{}] - Done", tenantId);
        return Optional.ofNullable(mapper.map(tenant));
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, TenantReferenceType referenceType)
        throws TechnicalException {
        log.debug("Delete tenants by refId: {}/{}", referenceId, referenceType);
        try {
            final List<String> tenants = internalTenantRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(TenantMongo::getId)
                .toList();
            log.debug("Delete tenants by refId: {}/{}", referenceId, referenceType);
            return tenants;
        } catch (Exception ex) {
            log.error("Failed to delete tenants by refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete tenants by reference");
        }
    }

    @Override
    public Tenant create(Tenant tenant) throws TechnicalException {
        log.debug("Create tenant [{}]", tenant.getName());

        TenantMongo tenantMongo = mapper.map(tenant);
        TenantMongo createdTenantMongo = internalTenantRepo.insert(tenantMongo);

        Tenant res = mapper.map(createdTenantMongo);

        log.debug("Create tenant [{}] - Done", tenant.getName());

        return res;
    }

    @Override
    public Tenant update(Tenant tenant) throws TechnicalException {
        if (tenant == null || tenant.getName() == null) {
            throw new IllegalStateException("Tenant to update must have a name");
        }

        final TenantMongo tenantMongo = internalTenantRepo.findById(tenant.getId()).orElse(null);

        if (tenantMongo == null) {
            throw new IllegalStateException(String.format("No tenant found with name [%s]", tenant.getId()));
        }

        try {
            //Update
            tenantMongo.setName(tenant.getName());
            tenantMongo.setDescription(tenant.getDescription());
            tenantMongo.setReferenceId(tenant.getReferenceId());
            tenantMongo.setReferenceType(tenant.getReferenceType());

            TenantMongo tenantMongoUpdated = internalTenantRepo.save(tenantMongo);
            return mapper.map(tenantMongoUpdated);
        } catch (Exception e) {
            log.error("An error occurred when updating tenant", e);
            throw new TechnicalException("An error occurred when updating tenant");
        }
    }

    @Override
    public void delete(String tenantId) throws TechnicalException {
        try {
            internalTenantRepo.deleteById(tenantId);
        } catch (Exception e) {
            log.error("An error occurred when deleting tenant [{}]", tenantId, e);
            throw new TechnicalException("An error occurred when deleting tenant");
        }
    }

    @Override
    public Set<Tenant> findByReference(String referenceId, TenantReferenceType referenceType) throws TechnicalException {
        final List<TenantMongo> tenants = internalTenantRepo.findByReferenceIdAndReferenceType(referenceId, referenceType);
        return tenants
            .stream()
            .map(tenantMongo -> {
                final Tenant tenant = new Tenant();
                tenant.setId(tenantMongo.getId());
                tenant.setName(tenantMongo.getName());
                tenant.setDescription(tenantMongo.getDescription());
                tenant.setReferenceId(tenantMongo.getReferenceId());
                tenant.setReferenceType(tenantMongo.getReferenceType());
                return tenant;
            })
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Tenant> findAll() throws TechnicalException {
        return internalTenantRepo
            .findAll()
            .stream()
            .map(tenantMongo -> mapper.map(tenantMongo))
            .collect(Collectors.toSet());
    }
}
