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
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.redis.management.internal.EntrypointRedisRepository;
import io.gravitee.repository.redis.management.model.RedisEntrypoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisEntrypointRepository implements EntrypointRepository {

    @Autowired
    private EntrypointRedisRepository entrypointRedisRepository;

    @Override
    public Optional<Entrypoint> findById(final String entrypointId) throws TechnicalException {
        final RedisEntrypoint redisEntryPoint = entrypointRedisRepository.findById(entrypointId);
        return Optional.ofNullable(convert(redisEntryPoint));
    }

    @Override
    public Entrypoint create(final Entrypoint entrypoint) throws TechnicalException {
        final RedisEntrypoint redisEntryPoint = entrypointRedisRepository.saveOrUpdate(convert(entrypoint));
        return convert(redisEntryPoint);
    }

    @Override
    public Entrypoint update(final Entrypoint entrypoint) throws TechnicalException {
        if (entrypoint == null || entrypoint.getValue() == null) {
            throw new IllegalStateException("EntryPoint to update must have a value");
        }

        final RedisEntrypoint redisEntryPoint = entrypointRedisRepository.findById(entrypoint.getId());

        if (redisEntryPoint == null) {
            throw new IllegalStateException(String.format("No entrypoint found with id [%s]", entrypoint.getId()));
        }

        final RedisEntrypoint redisEntryPointUpdated = entrypointRedisRepository.saveOrUpdate(convert(entrypoint));
        return convert(redisEntryPointUpdated);
    }

    @Override
    public Set<Entrypoint> findAll() throws TechnicalException {
        final Set<RedisEntrypoint> entrypoints = entrypointRedisRepository.findAll();

        return entrypoints.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(final String entrypointId) throws TechnicalException {
        entrypointRedisRepository.delete(entrypointId);
    }

    private Entrypoint convert(final RedisEntrypoint redisEntryPoint) {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId(redisEntryPoint.getId());
        entrypoint.setValue(redisEntryPoint.getValue());
        entrypoint.setTags(redisEntryPoint.getTags());
        return entrypoint;
    }

    private RedisEntrypoint convert(final Entrypoint entrypoint) {
        final RedisEntrypoint redisEntryPoint = new RedisEntrypoint();
        redisEntryPoint.setId(entrypoint.getId());
        redisEntryPoint.setValue(entrypoint.getValue());
        redisEntryPoint.setTags(entrypoint.getTags());
        return redisEntryPoint;
    }
}
