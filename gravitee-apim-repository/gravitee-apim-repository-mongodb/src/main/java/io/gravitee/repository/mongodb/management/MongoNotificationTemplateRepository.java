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
import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import io.gravitee.repository.mongodb.management.internal.model.NotificationTemplateMongo;
import io.gravitee.repository.mongodb.management.internal.notification.NotificationTemplateMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoNotificationTemplateRepository implements NotificationTemplateRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoNotificationTemplateRepository.class);

    @Autowired
    private NotificationTemplateMongoRepository internalNotificationTemplateRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<NotificationTemplate> findById(String id) throws TechnicalException {
        LOGGER.debug("Find notificationTemplate by ID [{}]", id);
        NotificationTemplateMongo notificationTemplate = internalNotificationTemplateRepo.findById(id).orElse(null);
        NotificationTemplate res = mapper.map(notificationTemplate, NotificationTemplate.class);

        LOGGER.debug("Find notificationTemplate by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public NotificationTemplate create(NotificationTemplate notificationTemplate) throws TechnicalException {
        LOGGER.debug("Create notificationTemplate [{}]", notificationTemplate.getName());

        NotificationTemplateMongo notificationTemplateMongo = mapper.map(notificationTemplate, NotificationTemplateMongo.class);
        NotificationTemplateMongo createdNotificationTemplateMongo = internalNotificationTemplateRepo.insert(notificationTemplateMongo);

        NotificationTemplate res = mapper.map(createdNotificationTemplateMongo, NotificationTemplate.class);

        LOGGER.debug("Create notificationTemplate [{}] - Done", notificationTemplate.getName());

        return res;
    }

    @Override
    public NotificationTemplate update(NotificationTemplate notificationTemplate) throws TechnicalException {
        if (notificationTemplate == null) {
            throw new IllegalStateException("NotificationTemplate must not be null");
        }

        NotificationTemplateMongo notificationTemplateMongo = internalNotificationTemplateRepo
            .findById(notificationTemplate.getId())
            .orElse(null);
        if (notificationTemplateMongo == null) {
            throw new IllegalStateException(String.format("No notificationTemplate found with id [%s]", notificationTemplate.getId()));
        }

        try {
            notificationTemplateMongo.setName(notificationTemplate.getName());
            notificationTemplateMongo.setDescription(notificationTemplate.getDescription());
            notificationTemplateMongo.setTitle(notificationTemplate.getTitle());
            notificationTemplateMongo.setType(notificationTemplate.getType().name());
            notificationTemplateMongo.setContent(notificationTemplate.getContent());
            notificationTemplateMongo.setHook(notificationTemplate.getHook());
            notificationTemplateMongo.setScope(notificationTemplate.getScope());
            notificationTemplateMongo.setReferenceId(notificationTemplate.getReferenceId());
            notificationTemplateMongo.setReferenceType(notificationTemplate.getReferenceType().name());
            notificationTemplateMongo.setUpdatedAt(notificationTemplate.getUpdatedAt());
            notificationTemplateMongo.setEnabled(notificationTemplate.isEnabled());

            NotificationTemplateMongo notificationTemplateMongoUpdated = internalNotificationTemplateRepo.save(notificationTemplateMongo);
            return mapper.map(notificationTemplateMongoUpdated, NotificationTemplate.class);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating notificationTemplate", e);
            throw new TechnicalException("An error occured when updating notificationTemplate");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalNotificationTemplateRepo.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting notificationTemplate [{}]", id, e);
            throw new TechnicalException("An error occured when deleting notificationTemplate");
        }
    }

    @Override
    public Set<NotificationTemplate> findAll() throws TechnicalException {
        LOGGER.debug("Find all notificationTemplates");

        List<NotificationTemplateMongo> notificationTemplates = internalNotificationTemplateRepo.findAll();
        Set<NotificationTemplate> res = mapper.collection2set(
            notificationTemplates,
            NotificationTemplateMongo.class,
            NotificationTemplate.class
        );

        LOGGER.debug("Find all notificationTemplates - Done");
        return res;
    }

    @Override
    public Set<NotificationTemplate> findByTypeAndReferenceIdAndReferenceType(
        NotificationTemplateType type,
        String referenceId,
        NotificationTemplateReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("Find all notificationTemplates by type {}", type);

        List<NotificationTemplateMongo> notificationTemplates = internalNotificationTemplateRepo.findByType(
            type.name(),
            referenceId,
            referenceType.name()
        );
        Set<NotificationTemplate> res = mapper.collection2set(
            notificationTemplates,
            NotificationTemplateMongo.class,
            NotificationTemplate.class
        );

        LOGGER.debug("Find all notificationTemplates by type - Done");
        return res;
    }

    @Override
    public Set<NotificationTemplate> findAllByReferenceIdAndReferenceType(
        String referenceId,
        NotificationTemplateReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("Find all notificationTemplates by environment");

        List<NotificationTemplateMongo> notificationTemplates = internalNotificationTemplateRepo.findByReferenceIdAndReferenceType(
            referenceId,
            referenceType.name()
        );
        Set<NotificationTemplate> res = mapper.collection2set(
            notificationTemplates,
            NotificationTemplateMongo.class,
            NotificationTemplate.class
        );

        LOGGER.debug("Find all notificationTemplates by environment- Done");
        return res;
    }

    @Override
    public Set<NotificationTemplate> findByHookAndScopeAndReferenceIdAndReferenceType(
        String hook,
        String scope,
        String referenceId,
        NotificationTemplateReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("Find all notificationTemplates by environment");

        List<NotificationTemplateMongo> notificationTemplates = internalNotificationTemplateRepo.findByHookAndScopeAndReferenceIdAndReferenceType(
            hook,
            scope,
            referenceId,
            referenceType.name()
        );
        Set<NotificationTemplate> res = mapper.collection2set(
            notificationTemplates,
            NotificationTemplateMongo.class,
            NotificationTemplate.class
        );

        LOGGER.debug("Find all notificationTemplates by environment- Done");
        return res;
    }
}
