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
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.model.ApiHeader;
import io.gravitee.repository.redis.management.internal.ApiHeaderRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiHeaderRepository implements ApiHeaderRepository {

    @Autowired
    private ApiHeaderRedisRepository apiHeaderRedisRepository;

    @Override
    public Optional<ApiHeader> findById(final String id) throws TechnicalException {
        final RedisApiHeader redis = apiHeaderRedisRepository.findById(id);
        return Optional.ofNullable(convert(redis));
    }

    @Override
    public ApiHeader create(final ApiHeader apiHeader) throws TechnicalException {
        final RedisApiHeader redis = apiHeaderRedisRepository.saveOrUpdate(convert(apiHeader));
        return convert(redis);
    }

    @Override
    public ApiHeader update(final ApiHeader apiHeader) throws TechnicalException {
        final RedisApiHeader apiHeaderRedis = apiHeaderRedisRepository.findById(apiHeader.getId());

        if (apiHeaderRedis == null) {
            throw new IllegalStateException(String.format("No apiHeader found with name [%s]", apiHeader.getId()));
        }
        
        final RedisApiHeader redisView = apiHeaderRedisRepository.saveOrUpdate(convert(apiHeader));
        return convert(redisView);
    }

    @Override
    public Set<ApiHeader> findAll() throws TechnicalException {
        final Set<RedisApiHeader> apiHeaders = apiHeaderRedisRepository.findAll();

        return apiHeaders.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        apiHeaderRedisRepository.delete(id);
    }

    private ApiHeader convert(final RedisApiHeader redis) {
        if (redis == null) {
            return null;
        }
        final ApiHeader apiHeader = new ApiHeader();
        apiHeader.setId(redis.getId());
        apiHeader.setName(redis.getName());
        apiHeader.setValue(redis.getValue());
        apiHeader.setOrder(redis.getOrder());
        if (redis.getCreatedAt() > 0) {
            apiHeader.setCreatedAt(new Date(redis.getCreatedAt()));
        }
        if (redis.getUpdatedAt() > 0) {
            apiHeader.setUpdatedAt(new Date(redis.getUpdatedAt()));
        }
        return apiHeader;
    }

    private RedisApiHeader convert(final ApiHeader apiHeader) {
        if (apiHeader == null) {
            return null;
        }
        final RedisApiHeader redis = new RedisApiHeader();
        redis.setId(apiHeader.getId());
        redis.setName(apiHeader.getName());
        redis.setValue(apiHeader.getValue());
        redis.setOrder(apiHeader.getOrder());
        if (apiHeader.getCreatedAt() != null) {
            redis.setCreatedAt(apiHeader.getCreatedAt().getTime());
        }
        if (apiHeader.getUpdatedAt() != null) {
            redis.setUpdatedAt(apiHeader.getUpdatedAt().getTime());
        }
        return redis;
    }
}
