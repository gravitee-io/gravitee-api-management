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
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.redis.management.internal.ApiRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiRepository implements ApiRepository {

    @Autowired
    private ApiRedisRepository apiRedisRepository;

    @Override
    public Set<Api> findAll() throws TechnicalException {
        Set<RedisApi> apis = apiRedisRepository.findAll();

        return apis.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Api> findByVisibility(Visibility visibility) throws TechnicalException {
        return apiRedisRepository.findByVisibility(visibility.name()).stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Api> findByIds(List<String> ids) throws TechnicalException {
        return apiRedisRepository.find(ids).stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Api> findByGroups(List<String> groupIds) throws TechnicalException {
        return apiRedisRepository.findByGroups(groupIds)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Api> findById(String apiId) throws TechnicalException {
        RedisApi redisApi = this.apiRedisRepository.find(apiId);
        return Optional.ofNullable(convert(redisApi));
    }

    @Override
    public Api create(Api api) throws TechnicalException {
        RedisApi redisApi = apiRedisRepository.saveOrUpdate(convert(api));
        return convert(redisApi);
    }

    @Override
    public Api update(Api api) throws TechnicalException {
        RedisApi redisApi = apiRedisRepository.saveOrUpdate(convert(api));
        return convert(redisApi);
    }

    @Override
    public void delete(String apiId) throws TechnicalException {
        apiRedisRepository.delete(apiId);
    }

    private Api convert(RedisApi redisApi) {
        if (redisApi == null) {
            return null;
        }

        Api api = new Api();

        api.setId(redisApi.getId());
        api.setName(redisApi.getName());
        api.setCreatedAt(new Date(redisApi.getCreatedAt()));
        api.setUpdatedAt(new Date(redisApi.getUpdatedAt()));
        if (redisApi.getDeployedAt() != 0) {
            api.setDeployedAt(new Date(redisApi.getDeployedAt()));
        }
        api.setDefinition(redisApi.getDefinition());
        api.setDescription(redisApi.getDescription());
        api.setVersion(redisApi.getVersion());
        api.setVisibility(Visibility.valueOf(redisApi.getVisibility()));
        api.setLifecycleState(LifecycleState.valueOf(redisApi.getLifecycleState()));
        api.setPicture(redisApi.getPicture());
        api.setGroups(redisApi.getGroups());
        api.setViews(redisApi.getViews());
        api.setLabels(redisApi.getLabels());

        return api;
    }

    private RedisApi convert(Api api) {
        RedisApi redisApi = new RedisApi();

        redisApi.setId(api.getId());
        redisApi.setName(api.getName());
        redisApi.setCreatedAt(api.getCreatedAt().getTime());
        redisApi.setUpdatedAt(api.getUpdatedAt().getTime());

        if (api.getDeployedAt() != null) {
            redisApi.setDeployedAt(api.getDeployedAt().getTime());
        }

        redisApi.setDefinition(api.getDefinition());
        redisApi.setDescription(api.getDescription());
        redisApi.setVersion(api.getVersion());
        redisApi.setVisibility(api.getVisibility().name());
        redisApi.setLifecycleState(api.getLifecycleState().name());
        redisApi.setPicture(api.getPicture());
        redisApi.setGroups(api.getGroups());
        redisApi.setViews(api.getViews());
        redisApi.setLabels(api.getLabels());

        return redisApi;
    }
}
