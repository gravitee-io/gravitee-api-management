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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationMongo;
import io.gravitee.repository.mongodb.management.internal.notification.PortalNotificationMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoPortalNotificationRepository implements PortalNotificationRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoPortalNotificationRepository.class);

    @Autowired
    private PortalNotificationMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public List<PortalNotification> findByUser(String user) {
        LOGGER.debug("Find notifications by user: {}", user);
        return internalRepo.findByUser(user).stream().map(n -> mapper.map(n, PortalNotification.class)).collect(Collectors.toList());
    }

    @Override
    public Optional<PortalNotification> findById(String id) throws TechnicalException {
        LOGGER.debug("Find notification by id: {}", id);
        PortalNotificationMongo portalNotificationMongo = internalRepo.findById(id).orElse(null);
        return Optional.ofNullable(mapPortalNotification(portalNotificationMongo));
    }

    private PortalNotification mapPortalNotification(PortalNotificationMongo portalNotificationMongo) {
        return (portalNotificationMongo == null) ? null : mapper.map(portalNotificationMongo, PortalNotification.class);
    }

    @Override
    public PortalNotification create(PortalNotification item) throws TechnicalException {
        LOGGER.debug("Create notification : {}", item);
        return mapper.map(internalRepo.insert(mapper.map(item, PortalNotificationMongo.class)), PortalNotification.class);
    }

    @Override
    public void create(List<PortalNotification> notifications) throws TechnicalException {
        LOGGER.debug("Create notifications : {}", notifications);
        List<PortalNotificationMongo> notificationMongos = notifications
            .stream()
            .map(n -> mapper.map(n, PortalNotificationMongo.class))
            .collect(Collectors.toList());
        internalRepo.insert(notificationMongos);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("Delete notification : {}", id);
        internalRepo.deleteById(id);
    }

    @Override
    public void deleteAll(String user) {
        LOGGER.debug("Delete notification for user : {}", user);
        internalRepo.deleteAll(user);
    }

    @Override
    public Set<PortalNotification> findAll() throws TechnicalException {
        return internalRepo.findAll().stream().map(this::mapPortalNotification).collect(Collectors.toSet());
    }
}
