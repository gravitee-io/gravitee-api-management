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
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationConfigMongo;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationConfigPkMongo;
import io.gravitee.repository.mongodb.management.internal.notification.PortalNotificationConfigMongoRepository;
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
public class MongoPortalNotificationConfigRepository implements PortalNotificationConfigRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoPortalNotificationConfigRepository.class);

    @Autowired
    private PortalNotificationConfigMongoRepository internalRepo;

    @Override
    public PortalNotificationConfig create(PortalNotificationConfig pnc) throws TechnicalException {
        LOGGER.debug("Create PortalNotificationConfig [{}, {}, {}]", pnc.getUser(), pnc.getReferenceType(), pnc.getReferenceId());
        PortalNotificationConfig cfg = map(internalRepo.insert(map(pnc)));
        LOGGER.debug("Create PortalNotificationConfig [{}, {}, {}] - Done", cfg.getUser(), cfg.getReferenceType(), cfg.getReferenceId());
        return cfg;
    }

    @Override
    public PortalNotificationConfig update(PortalNotificationConfig pnc) throws TechnicalException {
        LOGGER.debug("Update PortalNotificationConfig [{}, {}, {}]", pnc.getUser(), pnc.getReferenceType(), pnc.getReferenceId());
        PortalNotificationConfig cfg = map(internalRepo.save(map(pnc)));
        LOGGER.debug("Update PortalNotificationConfig [{}, {}, {}] - Done", cfg.getUser(), cfg.getReferenceType(), cfg.getReferenceId());
        return cfg;
    }

    @Override
    public void delete(PortalNotificationConfig pnc) throws TechnicalException {
        LOGGER.debug("Delete PortalNotificationConfig [{}, {}, {}]", pnc.getUser(), pnc.getReferenceType(), pnc.getReferenceId());
        internalRepo.deleteById(map(pnc).getId());
        LOGGER.debug("Delete PortalNotificationConfig [{}, {}, {}] - Done", pnc.getUser(), pnc.getReferenceType(), pnc.getReferenceId());
    }

    @Override
    public Optional<PortalNotificationConfig> findById(String userId, NotificationReferenceType referenceType, String referenceId)
        throws TechnicalException {
        LOGGER.debug("Find PortalNotificationConfig [{}, {}, {}]", userId, referenceType, referenceId);
        PortalNotificationConfigPkMongo pk = new PortalNotificationConfigPkMongo();
        pk.setReferenceType(referenceType);
        pk.setReferenceId(referenceId);
        pk.setUser(userId);
        PortalNotificationConfigMongo one = internalRepo.findById(pk).orElse(null);
        if (one == null) {
            return Optional.empty();
        }
        return Optional.of(map(one));
    }

    @Override
    public List<PortalNotificationConfig> findByReferenceAndHook(String hook, NotificationReferenceType referenceType, String referenceId) {
        LOGGER.debug("Find PortalNotificationConfigs [{}, {}, {}]", hook, referenceType, referenceId);
        return internalRepo
            .findByReferenceAndHook(hook, referenceType.name(), referenceId)
            .stream()
            .map(this::map)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteByUser(String user) {
        LOGGER.debug("Delete PortalNotificationConfigs [{}]", user);
        internalRepo.deleteByUser(user);
    }

    @Override
    public void deleteByReferenceIdAndReferenceType(String referenceId, NotificationReferenceType referenceType) {
        LOGGER.debug("Delete PortalNotificationConfigs [{}, {}]", referenceType, referenceId);
        internalRepo.deleteByReference(referenceType.name(), referenceId);
    }

    private PortalNotificationConfigMongo map(PortalNotificationConfig portalNotificationConfig) {
        PortalNotificationConfigPkMongo pk = new PortalNotificationConfigPkMongo();
        pk.setReferenceType(portalNotificationConfig.getReferenceType());
        pk.setReferenceId(portalNotificationConfig.getReferenceId());
        pk.setUser(portalNotificationConfig.getUser());

        PortalNotificationConfigMongo mongo = new PortalNotificationConfigMongo();
        mongo.setId(pk);
        mongo.setHooks(portalNotificationConfig.getHooks());
        mongo.setCreatedAt(portalNotificationConfig.getCreatedAt());
        mongo.setUpdatedAt(portalNotificationConfig.getUpdatedAt());

        return mongo;
    }

    private PortalNotificationConfig map(PortalNotificationConfigMongo mongo) {
        return PortalNotificationConfig
            .builder()
            .referenceType(mongo.getId().getReferenceType())
            .referenceId(mongo.getId().getReferenceId())
            .user(mongo.getId().getUser())
            .hooks(mongo.getHooks())
            .createdAt(mongo.getCreatedAt())
            .updatedAt(mongo.getUpdatedAt())
            .build();
    }

    @Override
    public Set<PortalNotificationConfig> findAll() throws TechnicalException {
        return internalRepo.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
