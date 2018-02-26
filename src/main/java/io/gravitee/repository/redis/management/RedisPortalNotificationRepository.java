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
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.redis.management.internal.PortalNotificationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPortalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class RedisPortalNotificationRepository implements PortalNotificationRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PortalNotificationRedisRepository portalNotificationRedisRepository;

    @Override
    public List<PortalNotification> findByUser(String user) throws TechnicalException {
        return portalNotificationRedisRepository.findByUser(user)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void create(List<PortalNotification> notifications) throws TechnicalException {
        if (notifications.isEmpty()) {
            return;
        }
        List<RedisPortalNotification> redisPortalNotifications = notifications
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
        portalNotificationRedisRepository.saveOrUpdate(redisPortalNotifications);

    }

    @Override
    public void deleteAll(String user) throws TechnicalException {
        this.findByUser(user)
                .forEach(portalNotification -> {
                    try {
                        this.delete(portalNotification.getId());
                    } catch (TechnicalException e) {
                        logger.error(e.getMessage(), e);
                    }
                });
    }

    @Override
    public PortalNotification create(PortalNotification notif) throws TechnicalException {
        this.create(Collections.singletonList(notif));
        return notif;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        portalNotificationRedisRepository.delete(id);
    }

    private RedisPortalNotification convert(PortalNotification portalNotification) {
        RedisPortalNotification redisPortalNotification = new RedisPortalNotification();

        redisPortalNotification.setId(portalNotification.getId());
        redisPortalNotification.setUser(portalNotification.getUser());
        redisPortalNotification.setTitle(portalNotification.getTitle());
        redisPortalNotification.setMessage(portalNotification.getMessage());
        redisPortalNotification.setCreatedAt(portalNotification.getCreatedAt().getTime());

        return redisPortalNotification;
    }

    private PortalNotification convert(RedisPortalNotification redisPortalNotification) {
        PortalNotification portalNotification = new PortalNotification();

        portalNotification.setId(redisPortalNotification.getId());
        portalNotification.setUser(redisPortalNotification.getUser());
        portalNotification.setTitle(redisPortalNotification.getTitle());
        portalNotification.setMessage(redisPortalNotification.getMessage());
        portalNotification.setCreatedAt(new Date(redisPortalNotification.getCreatedAt()));

        return portalNotification;
    }

}
