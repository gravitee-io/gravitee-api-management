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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.mongodb.management.internal.model.GenericNotificationConfigMongo;
import io.gravitee.repository.mongodb.management.internal.notification.GenericNotificationConfigMongoRepository;
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
public class MongoGenericNotificationConfigRepository implements GenericNotificationConfigRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoGenericNotificationConfigRepository.class);

    @Autowired
    private GenericNotificationConfigMongoRepository internalRepo;

    @Override
    public GenericNotificationConfig create(GenericNotificationConfig pnc) throws TechnicalException {
        LOGGER.debug("Create GenericNotificationConfig [{}, {}, {}]", pnc.getNotifier(), pnc.getReferenceType(), pnc.getReferenceId());
        GenericNotificationConfig cfg = map(internalRepo.insert(map(pnc)));
        LOGGER.debug(
            "Create GenericNotificationConfig [{}, {}, {}] - Done",
            cfg.getNotifier(),
            cfg.getReferenceType(),
            cfg.getReferenceId()
        );
        return cfg;
    }

    @Override
    public GenericNotificationConfig update(GenericNotificationConfig pnc) throws TechnicalException {
        LOGGER.debug("Update GenericNotificationConfig [{}, {}, {}]", pnc.getNotifier(), pnc.getReferenceType(), pnc.getReferenceId());
        GenericNotificationConfig cfg = map(internalRepo.save(map(pnc)));
        LOGGER.debug(
            "Update GenericNotificationConfig [{}, {}, {}] - Done",
            cfg.getNotifier(),
            cfg.getReferenceType(),
            cfg.getReferenceId()
        );
        return cfg;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("Delete GenericNotificationConfig [{}]", id);
        internalRepo.deleteById(id);
        LOGGER.debug("Delete GenericNotificationConfig [{}] - Done", id);
    }

    @Override
    public Optional<GenericNotificationConfig> findById(String id) throws TechnicalException {
        LOGGER.debug("Find GenericNotificationConfig [{}]", id);
        GenericNotificationConfigMongo one = internalRepo.findById(id).orElse(null);
        if (one == null) {
            return Optional.empty();
        }
        return Optional.of(map(one));
    }

    @Override
    public List<GenericNotificationConfig> findByReferenceAndHook(
        String hook,
        NotificationReferenceType referenceType,
        String referenceId
    ) {
        LOGGER.debug("Find GenericNotificationConfig [{}, {}, {}]", hook, referenceType, referenceId);
        return internalRepo
            .findByReferenceAndHook(hook, referenceType.name(), referenceId)
            .stream()
            .map(this::map)
            .collect(Collectors.toList());
    }

    @Override
    public List<GenericNotificationConfig> findByReference(NotificationReferenceType referenceType, String referenceId)
        throws TechnicalException {
        LOGGER.debug("Find GenericNotificationConfig [{}, {}]", referenceType, referenceId);
        return internalRepo.findByReference(referenceType.name(), referenceId).stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public void deleteByConfig(String config) {
        LOGGER.debug("Delete GenericNotificationConfig by config [{}]", config);
        internalRepo.deleteByConfig(config);
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, NotificationReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete GenericNotificationConfig by refId: [{}/{}]", referenceId, referenceType);

        try {
            final var notificationConfigIds = internalRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(GenericNotificationConfigMongo::getId)
                .toList();
            LOGGER.debug("Delete GenericNotificationConfig by refId: [{}/{}] - Done", referenceId, referenceType);
            return notificationConfigIds;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete GenericNotificationConfig by refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete GenericNotificationConfig by reference");
        }
    }

    private GenericNotificationConfigMongo map(GenericNotificationConfig genericNotificationConfig) {
        GenericNotificationConfigMongo mongo = new GenericNotificationConfigMongo();
        mongo.setId(genericNotificationConfig.getId());
        mongo.setName(genericNotificationConfig.getName());
        mongo.setReferenceType(genericNotificationConfig.getReferenceType());
        mongo.setReferenceId(genericNotificationConfig.getReferenceId());
        mongo.setNotifier(genericNotificationConfig.getNotifier());
        mongo.setConfig(genericNotificationConfig.getConfig());
        mongo.setUseSystemProxy(genericNotificationConfig.isUseSystemProxy());
        mongo.setHooks(genericNotificationConfig.getHooks());
        mongo.setCreatedAt(genericNotificationConfig.getCreatedAt());
        mongo.setUpdatedAt(genericNotificationConfig.getUpdatedAt());

        return mongo;
    }

    private GenericNotificationConfig map(GenericNotificationConfigMongo mongo) {
        GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setId(mongo.getId());
        cfg.setName(mongo.getName());
        cfg.setReferenceType(mongo.getReferenceType());
        cfg.setReferenceId(mongo.getReferenceId());
        cfg.setNotifier(mongo.getNotifier());
        cfg.setConfig(mongo.getConfig());
        cfg.setUseSystemProxy(mongo.isUseSystemProxy());
        cfg.setHooks(mongo.getHooks());
        cfg.setCreatedAt(mongo.getCreatedAt());
        cfg.setUpdatedAt(mongo.getUpdatedAt());

        return cfg;
    }

    @Override
    public Set<GenericNotificationConfig> findAll() throws TechnicalException {
        return internalRepo.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
