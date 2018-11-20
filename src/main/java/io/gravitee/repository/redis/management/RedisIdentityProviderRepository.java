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
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import io.gravitee.repository.redis.management.internal.IdentityProviderRedisRepository;
import io.gravitee.repository.redis.management.model.RedisIdentityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisIdentityProviderRepository implements IdentityProviderRepository {

    @Autowired
    private IdentityProviderRedisRepository identityProviderRedisRepository;

    @Override
    public Set<IdentityProvider> findAll() throws TechnicalException {
        final Set<RedisIdentityProvider> identityProviders = identityProviderRedisRepository.findAll();

        return identityProviders.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<IdentityProvider> findById(String identityProviderId) throws TechnicalException {
        final RedisIdentityProvider redisIdentityProvider = identityProviderRedisRepository.findById(identityProviderId);
        return Optional.ofNullable(convert(redisIdentityProvider));
    }

    @Override
    public IdentityProvider create(IdentityProvider identityProvider) throws TechnicalException {
        final RedisIdentityProvider redisIdentityProvider = identityProviderRedisRepository.saveOrUpdate(convert(identityProvider));
        return convert(redisIdentityProvider);
    }

    @Override
    public IdentityProvider update(IdentityProvider identityProvider) throws TechnicalException {
        if (identityProvider == null || identityProvider.getName() == null) {
            throw new IllegalStateException("Identity provider to update must have a name");
        }

        RedisIdentityProvider redisIdentityProvider = identityProviderRedisRepository.findById(identityProvider.getId());

        if (redisIdentityProvider == null) {
            throw new IllegalStateException(String.format("No identity provider found with id [%s]", identityProvider.getId()));
        }

        redisIdentityProvider = identityProviderRedisRepository.saveOrUpdate(convert(identityProvider));
        return convert(redisIdentityProvider);
    }

    @Override
    public void delete(String identityProviderId) throws TechnicalException {
        identityProviderRedisRepository.delete(identityProviderId);
    }

    private IdentityProvider convert(RedisIdentityProvider redisIdentityProvider) {
        if (redisIdentityProvider == null) {
            return null;
        }

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setId(redisIdentityProvider.getId());
        identityProvider.setName(redisIdentityProvider.getName());
        identityProvider.setDescription(redisIdentityProvider.getDescription());
        identityProvider.setEnabled(redisIdentityProvider.isEnabled());
        identityProvider.setType(IdentityProviderType.valueOf(redisIdentityProvider.getType()));
        identityProvider.setCreatedAt(new Date(redisIdentityProvider.getCreatedAt()));
        identityProvider.setUpdatedAt(new Date(redisIdentityProvider.getUpdatedAt()));
        identityProvider.setConfiguration(redisIdentityProvider.getConfiguration());
        identityProvider.setGroupMappings(redisIdentityProvider.getGroupMappings());
        identityProvider.setRoleMappings(redisIdentityProvider.getRoleMappings());
        identityProvider.setUserProfileMapping(redisIdentityProvider.getUserProfileMapping());

        return identityProvider;
    }

    private RedisIdentityProvider convert(IdentityProvider identityProvider) {
        if (identityProvider == null) {
            return null;
        }

        RedisIdentityProvider redisIdentityProvider = new RedisIdentityProvider();

        redisIdentityProvider.setId(identityProvider.getId());
        redisIdentityProvider.setName(identityProvider.getName());
        redisIdentityProvider.setDescription(identityProvider.getDescription());
        redisIdentityProvider.setEnabled(identityProvider.isEnabled());
        redisIdentityProvider.setType(identityProvider.getType().name());

        if (identityProvider.getCreatedAt() != null) {
            redisIdentityProvider.setCreatedAt(identityProvider.getCreatedAt().getTime());
        }

        if (identityProvider.getUpdatedAt() != null) {
            redisIdentityProvider.setUpdatedAt(identityProvider.getUpdatedAt().getTime());
        }

        redisIdentityProvider.setConfiguration(identityProvider.getConfiguration());
        redisIdentityProvider.setGroupMappings(identityProvider.getGroupMappings());
        redisIdentityProvider.setRoleMappings(identityProvider.getRoleMappings());
        redisIdentityProvider.setUserProfileMapping(identityProvider.getUserProfileMapping());

        return redisIdentityProvider;
    }
}
