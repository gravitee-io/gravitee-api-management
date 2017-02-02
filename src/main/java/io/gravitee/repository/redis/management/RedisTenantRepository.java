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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.redis.management.internal.TenantRedisRepository;
import io.gravitee.repository.redis.management.model.RedisTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisTenantRepository implements TenantRepository {

    @Autowired
    private TenantRedisRepository tenantRedisRepository;

    @Override
    public Optional<Tenant> findById(final String tenantId) throws TechnicalException {
        final RedisTenant redisTenant = tenantRedisRepository.findById(tenantId);
        return Optional.ofNullable(convert(redisTenant));
    }

    @Override
    public Tenant create(final Tenant tenant) throws TechnicalException {
        final RedisTenant redisTenant = tenantRedisRepository.saveOrUpdate(convert(tenant));
        return convert(redisTenant);
    }

    @Override
    public Tenant update(final Tenant tenant) throws TechnicalException {
        Optional<Tenant> existingTenant = findById(tenant.getId());

        if (!existingTenant.isPresent()) {
            return null;
        }
        final RedisTenant redisTenant = tenantRedisRepository.saveOrUpdate(convert(tenant));
        return convert(redisTenant);
    }

    @Override
    public Set<Tenant> findAll() throws TechnicalException {
        final Set<RedisTenant> tenants = tenantRedisRepository.findAll();

        return tenants.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(final String tenantId) throws TechnicalException {
        tenantRedisRepository.delete(tenantId);
    }

    private Tenant convert(final RedisTenant redisTenant) {
        if (redisTenant == null) {
            return null;
        }
        final Tenant tenant = new Tenant();
        tenant.setId(redisTenant.getId());
        tenant.setName(redisTenant.getName());
        tenant.setDescription(redisTenant.getDescription());
        return tenant;
    }

    private RedisTenant convert(final Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        final RedisTenant redisTenant = new RedisTenant();
        redisTenant.setId(tenant.getId());
        redisTenant.setName(tenant.getName());
        redisTenant.setDescription(tenant.getDescription());
        return redisTenant;
    }
}
