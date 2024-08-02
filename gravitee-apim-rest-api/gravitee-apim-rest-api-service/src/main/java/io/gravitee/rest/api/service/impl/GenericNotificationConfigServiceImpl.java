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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.BadNotificationConfigException;
import io.gravitee.rest.api.service.exceptions.NotificationConfigNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GenericNotificationConfigServiceImpl extends AbstractService implements GenericNotificationConfigService {

    private final Logger LOGGER = LoggerFactory.getLogger(GenericNotificationConfigServiceImpl.class);

    @Lazy
    @Autowired
    GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Override
    public GenericNotificationConfigEntity create(GenericNotificationConfigEntity entity) {
        if (entity.getNotifier() == null || entity.getNotifier().isEmpty() || entity.getName() == null || entity.getName().isEmpty()) {
            throw new BadNotificationConfigException();
        }
        try {
            GenericNotificationConfig notificationConfig = convert(entity);
            notificationConfig.setId(UuidString.generateRandom());
            notificationConfig.setCreatedAt(new Date());
            notificationConfig.setUpdatedAt(notificationConfig.getCreatedAt());
            return convert(genericNotificationConfigRepository.create(notificationConfig));
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to save the generic notification settings {}", entity, te);
            throw new TechnicalManagementException("An error occurs while trying to save the generic notification settings " + entity, te);
        }
    }

    @Override
    public GenericNotificationConfigEntity update(GenericNotificationConfigEntity entity) {
        try {
            if (entity.getNotifier() == null || entity.getNotifier().isEmpty() || entity.getName() == null || entity.getName().isEmpty()) {
                throw new BadNotificationConfigException();
            }

            if (entity.getId() == null || entity.getId().isEmpty()) {
                throw new NotificationConfigNotFoundException();
            }
            Optional<GenericNotificationConfig> optionalConfig = genericNotificationConfigRepository.findById(entity.getId());
            if (!optionalConfig.isPresent()) {
                throw new NotificationConfigNotFoundException();
            }

            GenericNotificationConfig notificationConfig = convert(entity);
            notificationConfig.setCreatedAt(optionalConfig.get().getCreatedAt());
            notificationConfig.setUpdatedAt(new Date());
            return convert(genericNotificationConfigRepository.update(notificationConfig));
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to save the generic notification settings {}", entity, te);
            throw new TechnicalManagementException("An error occurs while trying to save the generic notification settings " + entity, te);
        }
    }

    @Override
    public void delete(String id) {
        try {
            genericNotificationConfigRepository.delete(id);
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to delete the generic notification {}", id, te);
            throw new TechnicalManagementException("An error occurs while trying to delete the generic notification " + id, te);
        }
    }

    @Override
    public void deleteReference(NotificationReferenceType referenceType, String referenceId) {
        try {
            genericNotificationConfigRepository.deleteByReferenceIdAndReferenceType(referenceId, referenceType);
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to delete the generic notifications {} / {}", referenceType, referenceId, e);
            throw new TechnicalManagementException(
                "An error occurs while trying to delete the generic notifications " + referenceType + " / " + referenceId,
                e
            );
        }
    }

    @Override
    public GenericNotificationConfigEntity findById(String id) {
        try {
            Optional<GenericNotificationConfig> optionalConfig = genericNotificationConfigRepository.findById(id);
            if (optionalConfig.isPresent()) {
                return convert(optionalConfig.get());
            }
            throw new NotificationConfigNotFoundException();
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to get the notification config {}", id, te);
            throw new TechnicalManagementException("An error occurs while trying to get the notification config " + id, te);
        }
    }

    @Override
    public List<GenericNotificationConfigEntity> findByReference(NotificationReferenceType referenceType, String referenceId) {
        try {
            return genericNotificationConfigRepository
                .findByReference(referenceType, referenceId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to get the notification config {}/{}", referenceType, referenceId);
            throw new TechnicalManagementException(
                "An error occurs while trying to get the notification config " + referenceType + "/" + referenceId,
                e
            );
        }
    }

    @Override
    public void deleteByUser(User user) {
        try {
            // currently, we only remove email notification. The configuration of this type of notifications contains only its email
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                genericNotificationConfigRepository.deleteByConfig(user.getEmail());
            }
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to delete the notification config for user {}", user.getId(), e);
            throw new TechnicalManagementException(
                "An error occurs while trying to delete the notification config for user " + user.getId(),
                e
            );
        }
    }

    private GenericNotificationConfig convert(GenericNotificationConfigEntity entity) {
        GenericNotificationConfig model = new GenericNotificationConfig();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setReferenceType(NotificationReferenceType.valueOf(entity.getReferenceType()));
        model.setReferenceId(entity.getReferenceId());
        model.setNotifier(entity.getNotifier());
        model.setConfig(entity.getConfig());
        model.setUseSystemProxy(entity.isUseSystemProxy());
        model.setHooks(entity.getHooks());
        return model;
    }

    private GenericNotificationConfigEntity convert(GenericNotificationConfig genericNotificationConfig) {
        GenericNotificationConfigEntity entity = new GenericNotificationConfigEntity();
        entity.setConfigType(NotificationConfigType.GENERIC);
        entity.setId(genericNotificationConfig.getId());
        entity.setName(genericNotificationConfig.getName());
        entity.setReferenceType(genericNotificationConfig.getReferenceType().name());
        entity.setReferenceId(genericNotificationConfig.getReferenceId());
        entity.setNotifier(genericNotificationConfig.getNotifier());
        entity.setConfig(genericNotificationConfig.getConfig());
        entity.setUseSystemProxy(genericNotificationConfig.isUseSystemProxy());
        entity.setHooks(genericNotificationConfig.getHooks());
        return entity;
    }
}
