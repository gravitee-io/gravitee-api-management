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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.mongodb.management.internal.api.TenantMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.TenantMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoTenantRepository implements TenantRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoTenantRepository.class);

    @Autowired
    private TenantMongoRepository internalTenantRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Tenant> findById(String tenantId) throws TechnicalException {
        LOGGER.debug("Find tenant by ID [{}]", tenantId);

        final TenantMongo tenant = internalTenantRepo.findOne(tenantId);

        LOGGER.debug("Find tenant by ID [{}] - Done", tenantId);
        return Optional.ofNullable(mapper.map(tenant, Tenant.class));
    }

    @Override
    public Tenant create(Tenant tenant) throws TechnicalException {
        LOGGER.debug("Create tenant [{}]", tenant.getName());

        TenantMongo tenantMongo = mapper.map(tenant, TenantMongo.class);
        TenantMongo createdTenantMongo = internalTenantRepo.insert(tenantMongo);

        Tenant res = mapper.map(createdTenantMongo, Tenant.class);

        LOGGER.debug("Create tenant [{}] - Done", tenant.getName());

        return res;
    }

    @Override
    public Tenant update(Tenant tenant) throws TechnicalException {
        if (tenant == null || tenant.getName() == null) {
            throw new IllegalStateException("Tenant to update must have a name");
        }

        final TenantMongo tenantMongo = internalTenantRepo.findOne(tenant.getId());

        if (tenantMongo == null) {
            throw new IllegalStateException(String.format("No tenant found with name [%s]", tenant.getId()));
        }

        try {
            //Update
            tenantMongo.setName(tenant.getName());
            tenantMongo.setDescription(tenant.getDescription());

            TenantMongo tenantMongoUpdated = internalTenantRepo.save(tenantMongo);
            return mapper.map(tenantMongoUpdated, Tenant.class);

        } catch (Exception e) {

            LOGGER.error("An error occured when updating tenant", e);
            throw new TechnicalException("An error occured when updating tenant");
        }
    }

    @Override
    public void delete(String tenantId) throws TechnicalException {
        try {
            internalTenantRepo.delete(tenantId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting tenant [{}]", tenantId, e);
            throw new TechnicalException("An error occured when deleting tenant");
        }
    }

    @Override
    public Set<Tenant> findAll() throws TechnicalException {
        final List<TenantMongo> tenants = internalTenantRepo.findAll();
        return tenants.stream()
                .map(tenantMongo -> {
                    final Tenant tenant = new Tenant();
                    tenant.setId(tenantMongo.getId());
                    tenant.setName(tenantMongo.getName());
                    tenant.setDescription(tenantMongo.getDescription());
                    return tenant;
                })
                .collect(Collectors.toSet());
    }
}
