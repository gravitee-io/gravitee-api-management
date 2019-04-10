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
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.repository.redis.management.internal.ClientRegistrationProviderRedisRepository;
import io.gravitee.repository.redis.management.model.RedisClientRegistrationProvider;
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
public class RedisClientRegistrationProviderRepository implements ClientRegistrationProviderRepository {

    @Autowired
    private ClientRegistrationProviderRedisRepository clientRegistrationProviderRedisRepository;

    @Override
    public Set<ClientRegistrationProvider> findAll() throws TechnicalException {
        final Set<RedisClientRegistrationProvider> clientRegistrationProviders = clientRegistrationProviderRedisRepository.findAll();

        return clientRegistrationProviders.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ClientRegistrationProvider> findById(String clientRegistrationProvider) throws TechnicalException {
        final RedisClientRegistrationProvider redisClientRegistrationProvider = clientRegistrationProviderRedisRepository.findById(clientRegistrationProvider);
        return Optional.ofNullable(convert(redisClientRegistrationProvider));
    }

    @Override
    public ClientRegistrationProvider create(ClientRegistrationProvider clientRegistrationProvider) throws TechnicalException {
        final RedisClientRegistrationProvider redisClientRegistrationProvider = clientRegistrationProviderRedisRepository.saveOrUpdate(convert(clientRegistrationProvider));
        return convert(redisClientRegistrationProvider);
    }

    @Override
    public ClientRegistrationProvider update(ClientRegistrationProvider clientRegistrationProvider) throws TechnicalException {
        if (clientRegistrationProvider == null || clientRegistrationProvider.getName() == null) {
            throw new IllegalStateException("Client registration provider to update must have a name");
        }

        RedisClientRegistrationProvider redisClientRegistrationProvider = clientRegistrationProviderRedisRepository.findById(clientRegistrationProvider.getId());

        if (redisClientRegistrationProvider == null) {
            throw new IllegalStateException(String.format("No client registration provider found with id [%s]", clientRegistrationProvider.getId()));
        }

        redisClientRegistrationProvider = clientRegistrationProviderRedisRepository.saveOrUpdate(convert(clientRegistrationProvider));
        return convert(redisClientRegistrationProvider);
    }

    @Override
    public void delete(String clientRegistrationProvider) throws TechnicalException {
        clientRegistrationProviderRedisRepository.delete(clientRegistrationProvider);
    }

    private ClientRegistrationProvider convert(RedisClientRegistrationProvider redisClientRegistrationProvider) {
        if (redisClientRegistrationProvider == null) {
            return null;
        }

        ClientRegistrationProvider clientRegistrationProvider = new ClientRegistrationProvider();
        clientRegistrationProvider.setId(redisClientRegistrationProvider.getId());
        clientRegistrationProvider.setName(redisClientRegistrationProvider.getName());
        clientRegistrationProvider.setDescription(redisClientRegistrationProvider.getDescription());
        clientRegistrationProvider.setDiscoveryEndpoint(redisClientRegistrationProvider.getDiscoveryEndpoint());
        clientRegistrationProvider.setClientId(redisClientRegistrationProvider.getClientId());
        clientRegistrationProvider.setClientSecret(redisClientRegistrationProvider.getClientSecret());
        clientRegistrationProvider.setScopes(redisClientRegistrationProvider.getScopes());
        clientRegistrationProvider.setCreatedAt(new Date(redisClientRegistrationProvider.getCreatedAt()));
        clientRegistrationProvider.setUpdatedAt(new Date(redisClientRegistrationProvider.getUpdatedAt()));

        return clientRegistrationProvider;
    }

    private RedisClientRegistrationProvider convert(ClientRegistrationProvider clientRegistrationProvider) {
        if (clientRegistrationProvider == null) {
            return null;
        }

        RedisClientRegistrationProvider redisClientRegistrationProvider = new RedisClientRegistrationProvider();

        redisClientRegistrationProvider.setId(clientRegistrationProvider.getId());
        redisClientRegistrationProvider.setName(clientRegistrationProvider.getName());
        redisClientRegistrationProvider.setDescription(clientRegistrationProvider.getDescription());

        if (clientRegistrationProvider.getCreatedAt() != null) {
            redisClientRegistrationProvider.setCreatedAt(clientRegistrationProvider.getCreatedAt().getTime());
        }

        if (clientRegistrationProvider.getUpdatedAt() != null) {
            redisClientRegistrationProvider.setUpdatedAt(clientRegistrationProvider.getUpdatedAt().getTime());
        }

        redisClientRegistrationProvider.setDiscoveryEndpoint(clientRegistrationProvider.getDiscoveryEndpoint());
        redisClientRegistrationProvider.setClientId(clientRegistrationProvider.getClientId());
        redisClientRegistrationProvider.setClientSecret(clientRegistrationProvider.getClientSecret());
        redisClientRegistrationProvider.setScopes(clientRegistrationProvider.getScopes());

        return redisClientRegistrationProvider;
    }
}
