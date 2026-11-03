/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationMongo;
import io.gravitee.repository.mongodb.management.internal.notification.PortalNotificationMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoPortalNotificationRepository implements PortalNotificationRepository {

    @Autowired
    private PortalNotificationMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public List<PortalNotification> findByUser(String user) {
        log.debug("Find notifications by user: {}", user);
        return internalRepo
            .findByUser(user)
            .stream()
            .map(n -> mapper.map(n))
            .collect(Collectors.toList());
    }

    @Override
    public Page<PortalNotification> search(PortalNotificationSearchCriteria criteria, Pageable pageable) {
        log.debug("Search notifications: {}, {}", criteria, pageable);
        var result = internalRepo.search(criteria, pageable);
        return new Page<>(
            result.getContent().stream().map(mapper::map).toList(),
            result.getPageNumber(),
            (int) result.getPageElements(),
            result.getTotalElements()
        );
    }

    @Override
    public PortalNotificationReferences findDistinctReferences(String user, String environmentId) {
        log.debug("Find notification references of user {} in environment {}", user, environmentId);
        return internalRepo.findDistinctReferences(user, environmentId);
    }

    @Override
    public boolean markAsRead(String user, String environmentId, String id, Date readAt) {
        log.debug("Mark notification {} of user {} as read", id, user);
        return internalRepo.markAsRead(user, environmentId, id, readAt);
    }

    @Override
    public void markAllAsRead(String user, String environmentId, Date readAt) {
        log.debug("Mark all notifications of user {} in environment {} as read", user, environmentId);
        internalRepo.markAllAsRead(user, environmentId, readAt);
    }

    @Override
    public boolean delete(String user, String environmentId, String id) {
        log.debug("Delete notification {} of user {}", id, user);
        return internalRepo.delete(user, environmentId, id);
    }

    @Override
    public void deleteAll(String user, String environmentId) {
        log.debug("Delete notifications of user {} in environment {}", user, environmentId);
        internalRepo.deleteAll(user, environmentId);
    }

    @Override
    public Optional<PortalNotification> findById(String id) throws TechnicalException {
        log.debug("Find notification by id: {}", id);
        PortalNotificationMongo portalNotificationMongo = internalRepo.findById(id).orElse(null);
        return Optional.ofNullable(mapPortalNotification(portalNotificationMongo));
    }

    private PortalNotification mapPortalNotification(PortalNotificationMongo portalNotificationMongo) {
        return (portalNotificationMongo == null) ? null : mapper.map(portalNotificationMongo);
    }

    @Override
    public PortalNotification create(PortalNotification item) throws TechnicalException {
        log.debug("Create notification : {}", item);
        return mapper.map(internalRepo.insert(mapper.map(item)));
    }

    @Override
    public void create(List<PortalNotification> notifications) throws TechnicalException {
        log.debug("Create notifications : {}", notifications);
        List<PortalNotificationMongo> notificationMongos = notifications
            .stream()
            .map(n -> mapper.map(n))
            .collect(Collectors.toList());
        internalRepo.insert(notificationMongos);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete notification : {}", id);
        internalRepo.deleteById(id);
    }

    @Override
    public void deleteAll(String user) {
        log.debug("Delete notification for user : {}", user);
        internalRepo.deleteAll(user);
    }

    @Override
    public Set<PortalNotification> findAll() throws TechnicalException {
        return internalRepo.findAll().stream().map(this::mapPortalNotification).collect(Collectors.toSet());
    }
}
