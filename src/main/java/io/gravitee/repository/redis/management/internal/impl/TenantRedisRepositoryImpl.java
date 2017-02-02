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
package io.gravitee.repository.redis.management.internal.impl;

import io.gravitee.repository.redis.management.internal.TenantRedisRepository;
import io.gravitee.repository.redis.management.model.RedisTenant;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TenantRedisRepositoryImpl extends AbstractRedisRepository implements TenantRedisRepository {

    private final static String REDIS_KEY = "tenant";

    @Override
    public Set<RedisTenant> findAll() {
        final Map<Object, Object> tenants = redisTemplate.opsForHash().entries(REDIS_KEY);

        return tenants.values()
                .stream()
                .map(object -> convert(object, RedisTenant.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisTenant findById(final String tenantId) {
        Object tenant = redisTemplate.opsForHash().get(REDIS_KEY, tenantId);
        if (tenant == null) {
            return null;
        }

        return convert(tenant, RedisTenant.class);
    }

    @Override
    public RedisTenant saveOrUpdate(final RedisTenant tenant) {
        redisTemplate.opsForHash().put(REDIS_KEY, tenant.getId(), tenant);
        return tenant;
    }

    @Override
    public void delete(final String tenant) {
        redisTemplate.opsForHash().delete(REDIS_KEY, tenant);
    }
}
