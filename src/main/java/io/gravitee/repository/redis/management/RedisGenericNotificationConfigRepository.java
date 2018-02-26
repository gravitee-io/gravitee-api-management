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
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.redis.management.internal.GenericNotificationConfigRedisRepository;
import io.gravitee.repository.redis.management.model.RedisGenericNotificationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class RedisGenericNotificationConfigRepository implements GenericNotificationConfigRepository {

    @Autowired
    private GenericNotificationConfigRedisRepository genericNotificationConfigRedisRepository;

    @Override
    public GenericNotificationConfig create(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        return convert(genericNotificationConfigRedisRepository.create(convert(genericNotificationConfig)));
    }

    @Override
    public GenericNotificationConfig update(GenericNotificationConfig genericNotificationConfig) throws TechnicalException {
        return convert(genericNotificationConfigRedisRepository.update(convert(genericNotificationConfig)));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        genericNotificationConfigRedisRepository.delete(id);
    }

    @Override
    public Optional<GenericNotificationConfig> findById(String id) throws TechnicalException {
        RedisGenericNotificationConfig genericNotificationConfig = genericNotificationConfigRedisRepository.findById(id);
        return Optional.ofNullable(convert(genericNotificationConfig));
    }

    @Override
    public List<GenericNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        return findByReference(referenceType, referenceId)
                .stream()
                .filter(genericNotificationConfig ->
                        genericNotificationConfig.getHooks().contains(hook))
                .collect(Collectors.toList());
    }

    @Override
    public List<GenericNotificationConfig> findByReference(NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        return genericNotificationConfigRedisRepository.findByReference(referenceType, referenceId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private GenericNotificationConfig convert(RedisGenericNotificationConfig redisGenericNotificationConfig) {
        if (redisGenericNotificationConfig == null) {
            return null;
        }

        GenericNotificationConfig genericNotificationConfig = new GenericNotificationConfig();

        genericNotificationConfig.setId(redisGenericNotificationConfig.getId());
        genericNotificationConfig.setName(redisGenericNotificationConfig.getName());
        genericNotificationConfig.setNotifier(redisGenericNotificationConfig.getNotifier());
        genericNotificationConfig.setConfig(redisGenericNotificationConfig.getConfig());
        genericNotificationConfig.setReferenceType(redisGenericNotificationConfig.getReferenceType());
        genericNotificationConfig.setReferenceId(redisGenericNotificationConfig.getReferenceId());
        genericNotificationConfig.setHooks(redisGenericNotificationConfig.getHooks());
        genericNotificationConfig.setCreatedAt(new Date(redisGenericNotificationConfig.getCreatedAt()));
        genericNotificationConfig.setUpdatedAt(new Date(redisGenericNotificationConfig.getUpdatedAt()));

        return genericNotificationConfig;
    }

    private RedisGenericNotificationConfig convert(GenericNotificationConfig genericNotificationConfig) {
        if (genericNotificationConfig == null) {
            return null;
        }

        RedisGenericNotificationConfig redis = new RedisGenericNotificationConfig();

        redis.setId(genericNotificationConfig.getId());
        redis.setName(genericNotificationConfig.getName());
        redis.setNotifier(genericNotificationConfig.getNotifier());
        redis.setConfig(genericNotificationConfig.getConfig());
        redis.setReferenceType(genericNotificationConfig.getReferenceType());
        redis.setReferenceId(genericNotificationConfig.getReferenceId());
        redis.setHooks(genericNotificationConfig.getHooks());
        redis.setCreatedAt(genericNotificationConfig.getCreatedAt().getTime());
        redis.setUpdatedAt(genericNotificationConfig.getUpdatedAt().getTime());

        return redis;
    }
}
