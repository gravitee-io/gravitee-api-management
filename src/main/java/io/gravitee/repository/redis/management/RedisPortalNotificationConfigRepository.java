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
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.redis.management.internal.PortalNotificationConfigRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPortalNotificationConfig;
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
public class RedisPortalNotificationConfigRepository implements PortalNotificationConfigRepository {

    @Autowired
    private PortalNotificationConfigRedisRepository portalNotificationConfigRedisRepository;

    @Override
    public PortalNotificationConfig create(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        return convert(portalNotificationConfigRedisRepository.create(convert(portalNotificationConfig)));
    }

    @Override
    public PortalNotificationConfig update(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        return convert(portalNotificationConfigRedisRepository.update(convert(portalNotificationConfig)));
    }

    @Override
    public void delete(PortalNotificationConfig portalNotificationConfig) throws TechnicalException {
        portalNotificationConfigRedisRepository.delete(
                portalNotificationConfig.getUser(),
                portalNotificationConfig.getReferenceType().name(),
                portalNotificationConfig.getReferenceId());
    }

    @Override
    public Optional<PortalNotificationConfig> findById(String user, NotificationReferenceType referenceType, String referenceId) throws TechnicalException {
        RedisPortalNotificationConfig redis = portalNotificationConfigRedisRepository.find(user, referenceType.name(), referenceId);
        return Optional.ofNullable(convert(redis));
    }

    @Override
    public List<PortalNotificationConfig> findByReferenceAndHook(final String hook, final NotificationReferenceType referenceType, final String referenceId) throws TechnicalException {
        return portalNotificationConfigRedisRepository.findByReference(referenceType, referenceId)
                .stream()
                .filter(redisPortalNotificationConfig -> redisPortalNotificationConfig.getHooks().contains(hook))
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private PortalNotificationConfig convert(RedisPortalNotificationConfig redisPortalNotificationConfig) {
        if (redisPortalNotificationConfig == null) {
            return null;
        }

        PortalNotificationConfig portalNotificationConfig = new PortalNotificationConfig();

        portalNotificationConfig.setReferenceType(redisPortalNotificationConfig.getReferenceType());
        portalNotificationConfig.setReferenceId(redisPortalNotificationConfig.getReferenceId());
        portalNotificationConfig.setUser(redisPortalNotificationConfig.getUser());
        portalNotificationConfig.setHooks(redisPortalNotificationConfig.getHooks());
        portalNotificationConfig.setCreatedAt(new Date(redisPortalNotificationConfig.getCreatedAt()));
        portalNotificationConfig.setUpdatedAt(new Date(redisPortalNotificationConfig.getUpdatedAt()));

        return portalNotificationConfig;
    }

    private RedisPortalNotificationConfig convert(PortalNotificationConfig portalNotificationConfig) {
        if (portalNotificationConfig == null) {
            return null;
        }

        RedisPortalNotificationConfig redisPortalNotificationConfig = new RedisPortalNotificationConfig();

        redisPortalNotificationConfig.setReferenceType(portalNotificationConfig.getReferenceType());
        redisPortalNotificationConfig.setReferenceId(portalNotificationConfig.getReferenceId());
        redisPortalNotificationConfig.setUser(portalNotificationConfig.getUser());
        redisPortalNotificationConfig.setHooks(portalNotificationConfig.getHooks());
        redisPortalNotificationConfig.setCreatedAt(portalNotificationConfig.getCreatedAt().getTime());
        redisPortalNotificationConfig.setUpdatedAt(portalNotificationConfig.getUpdatedAt().getTime());

        return redisPortalNotificationConfig;
    }

}
