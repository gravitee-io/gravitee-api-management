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
package io.gravitee.rest.api.service.impl.configuration.dictionary;

import static io.gravitee.repository.management.model.Audit.AuditProperties.DICTIONARY;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProvider;
import io.gravitee.repository.management.model.DictionaryTrigger;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DictionaryServiceImpl extends AbstractService implements DictionaryService {

    private final Logger LOGGER = LoggerFactory.getLogger(DictionaryServiceImpl.class);

    @Lazy
    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public Set<DictionaryEntity> findAll(ExecutionContext executionContext) {
        try {
            return dictionaryRepository
                .findAllByEnvironments(Collections.singleton(executionContext.getEnvironmentId()))
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to retrieve dictionaries", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve dictionaries", ex);
        }
    }

    @Override
    public DictionaryEntity deploy(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Deploy dictionary {}", id);

            Dictionary dictionary = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(id));

            // add deployment date
            dictionary.setUpdatedAt(new Date());
            dictionary.setDeployedAt(dictionary.getUpdatedAt());

            dictionary = dictionaryRepository.update(dictionary);

            // And create event
            eventService.createDictionaryEvent(
                executionContext,
                Collections.singleton(executionContext.getEnvironmentId()),
                executionContext.getOrganizationId(),
                EventType.PUBLISH_DICTIONARY,
                dictionary
            );
            return convert(dictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to deploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to deploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity undeploy(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Undeploy dictionary {}", id);

            Dictionary dictionary = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(id));

            // add deployment date
            dictionary.setUpdatedAt(new Date());
            dictionary.setDeployedAt(dictionary.getUpdatedAt());

            dictionary = dictionaryRepository.update(dictionary);

            // And create event
            eventService.createDictionaryEvent(
                executionContext,
                Collections.singleton(executionContext.getEnvironmentId()),
                executionContext.getOrganizationId(),
                EventType.UNPUBLISH_DICTIONARY,
                dictionary
            );
            return convert(dictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to undeploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity start(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Start dictionary {}", id);

            Dictionary dictionary = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(id));

            // add deployment date
            dictionary.setUpdatedAt(new Date());
            dictionary.setState(LifecycleState.STARTED);

            Dictionary updatedDictionary = dictionaryRepository.update(dictionary);

            // And create event
            eventService.createDynamicDictionaryEvent(
                executionContext,
                Collections.singleton(executionContext.getEnvironmentId()),
                executionContext.getOrganizationId(),
                EventType.START_DICTIONARY,
                id
            );

            // Audit
            createAuditLog(
                executionContext,
                Dictionary.AuditEvent.DICTIONARY_UPDATED,
                updatedDictionary.getUpdatedAt(),
                dictionary,
                updatedDictionary
            );

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to undeploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity stop(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Stop dictionary {}", id);

            Dictionary dictionary = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(id));

            // add deployment date
            dictionary.setUpdatedAt(new Date());
            dictionary.setState(LifecycleState.STOPPED);

            Dictionary updatedDictionary = dictionaryRepository.update(dictionary);

            // And create event
            eventService.createDynamicDictionaryEvent(
                executionContext,
                Collections.singleton(executionContext.getEnvironmentId()),
                executionContext.getOrganizationId(),
                EventType.STOP_DICTIONARY,
                id
            );

            // Audit
            createAuditLog(
                executionContext,
                Dictionary.AuditEvent.DICTIONARY_UPDATED,
                updatedDictionary.getUpdatedAt(),
                dictionary,
                updatedDictionary
            );

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to undeploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity create(ExecutionContext executionContext, NewDictionaryEntity newDictionaryEntity) {
        try {
            LOGGER.debug("Create dictionary {}", newDictionaryEntity);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(IdGenerator.generate(newDictionaryEntity.getName()));
            if (optDictionary.isPresent()) {
                throw new DictionaryAlreadyExistsException(newDictionaryEntity.getName());
            }

            Dictionary dictionary = convert(newDictionaryEntity);

            dictionary.setEnvironmentId(executionContext.getEnvironmentId());

            // Set date fields
            dictionary.setCreatedAt(new Date());
            dictionary.setState(LifecycleState.STOPPED);
            dictionary.setUpdatedAt(dictionary.getCreatedAt());

            Dictionary createdDictionary = dictionaryRepository.create(dictionary);

            createAuditLog(executionContext, Dictionary.AuditEvent.DICTIONARY_CREATED, dictionary.getCreatedAt(), null, dictionary);
            return convert(createdDictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create dictionary {}", newDictionaryEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newDictionaryEntity, ex);
        }
    }

    @Override
    public DictionaryEntity update(ExecutionContext executionContext, String id, UpdateDictionaryEntity updateDictionaryEntity) {
        try {
            LOGGER.debug("Update dictionary {}", updateDictionaryEntity);

            Dictionary dictionaryToUpdate = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(updateDictionaryEntity.getName()));

            Dictionary dictionary = convert(updateDictionaryEntity);

            dictionary.setId(id);
            dictionary.setCreatedAt(dictionaryToUpdate.getCreatedAt());
            dictionary.setEnvironmentId(dictionaryToUpdate.getEnvironmentId());
            dictionary.setUpdatedAt(new Date());
            dictionary.setState(dictionaryToUpdate.getState());

            Dictionary updatedDictionary = dictionaryRepository.update(dictionary);

            // Force a new start event if the dictionary is already started when updating.
            if (updatedDictionary.getType() == DictionaryType.DYNAMIC && updatedDictionary.getState() == LifecycleState.STARTED) {
                eventService.createDynamicDictionaryEvent(
                    executionContext,
                    Collections.singleton(executionContext.getEnvironmentId()),
                    executionContext.getOrganizationId(),
                    EventType.START_DICTIONARY,
                    id
                );
            }

            // Audit
            createAuditLog(
                executionContext,
                Dictionary.AuditEvent.DICTIONARY_UPDATED,
                updatedDictionary.getUpdatedAt(),
                dictionaryToUpdate,
                updatedDictionary
            );

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update dictionary {}", updateDictionaryEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to update " + updateDictionaryEntity, ex);
        }
    }

    @Override
    public DictionaryEntity updateProperties(ExecutionContext executionContext, final String id, final Map<String, String> properties) {
        try {
            LOGGER.debug("Update dictionary properties {}", id);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(id);
            if (optDictionary.isEmpty()) {
                throw new DictionaryNotFoundException(id);
            }
            Dictionary dictionary = optDictionary.get();
            dictionary.setProperties(properties);
            dictionary.setUpdatedAt(new Date());
            dictionary.setDeployedAt(dictionary.getUpdatedAt());
            Dictionary updatedDictionary = dictionaryRepository.update(dictionary);

            // Create publish event
            eventService.createDictionaryEvent(
                executionContext,
                Collections.singleton(executionContext.getEnvironmentId()),
                executionContext.getOrganizationId(),
                EventType.PUBLISH_DICTIONARY,
                dictionary
            );
            // Audit
            createAuditLog(
                executionContext,
                Dictionary.AuditEvent.DICTIONARY_UPDATED,
                updatedDictionary.getUpdatedAt(),
                optDictionary.get(),
                updatedDictionary
            );

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to update dictionary '" + id + "' properties", ex);
        }
    }

    @Override
    public DictionaryEntity findById(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Find dictionary by ID: {}", id);
            Optional<Dictionary> byId = dictionaryRepository.findById(id);
            //FIXME filter should be always applied but DictionaryManager (sync service) does not handle environments for dictionaries
            if (executionContext.hasEnvironmentId()) {
                byId = byId.filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()));
            }
            return byId.map(this::convert).orElseThrow(() -> new DictionaryNotFoundException(id));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete a dictionary using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a dictionary using its ID " + id, ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Delete dictionary: {}", id);

            Dictionary dictionary = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(id));

            if (dictionary.getType() == DictionaryType.DYNAMIC) {
                this.stop(executionContext, id);
            }

            dictionaryRepository.delete(id);

            // And create event
            eventService.createDictionaryEvent(
                executionContext,
                Collections.singleton(executionContext.getEnvironmentId()),
                executionContext.getOrganizationId(),
                EventType.UNPUBLISH_DICTIONARY,
                dictionary
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete a dictionary using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a dictionary using its ID " + id, ex);
        }
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        Audit.AuditEvent event,
        Date createdAt,
        Dictionary oldValue,
        Dictionary newValue
    ) {
        String dictionaryName = oldValue != null ? oldValue.getName() : newValue.getName();

        auditService.createAuditLog(
            executionContext,
            Collections.singletonMap(DICTIONARY, dictionaryName),
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    private DictionaryEntity convert(Dictionary dictionary) {
        DictionaryEntity.DictionaryEntityBuilder dictionaryEntityBuilder = DictionaryEntity
            .builder()
            .id(dictionary.getId())
            .name(dictionary.getName())
            .description(dictionary.getDescription())
            .createdAt(dictionary.getCreatedAt())
            .updatedAt(dictionary.getUpdatedAt())
            .deployedAt(dictionary.getDeployedAt())
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.valueOf(dictionary.getType().name()))
            .properties(dictionary.getProperties())
            .state(Lifecycle.State.valueOf(dictionary.getState().name()));

        if (dictionary.getType() == DictionaryType.DYNAMIC) {
            dictionaryEntityBuilder.provider(convert(dictionary.getProvider())).trigger(convert(dictionary.getTrigger()));
        }

        return dictionaryEntityBuilder.build();
    }

    private Dictionary convert(UpdateDictionaryEntity updateDictionaryEntity) {
        Dictionary dictionary = new Dictionary();

        dictionary.setName(updateDictionaryEntity.getName());
        dictionary.setDescription(updateDictionaryEntity.getDescription());
        dictionary.setProperties(updateDictionaryEntity.getProperties());

        final io.gravitee.rest.api.model.configuration.dictionary.DictionaryType type = updateDictionaryEntity.getType();
        if (type != null) {
            dictionary.setType(io.gravitee.repository.management.model.DictionaryType.valueOf(type.name()));
        }

        if (type == io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC) {
            dictionary.setProvider(convert(updateDictionaryEntity.getProvider()));
            dictionary.setTrigger(convert(updateDictionaryEntity.getTrigger()));
        }

        return dictionary;
    }

    private Dictionary convert(NewDictionaryEntity newDictionaryEntity) {
        Dictionary dictionary = new Dictionary();

        dictionary.setId(IdGenerator.generate(newDictionaryEntity.getName()));
        dictionary.setName(newDictionaryEntity.getName());
        dictionary.setDescription(newDictionaryEntity.getDescription());

        final io.gravitee.rest.api.model.configuration.dictionary.DictionaryType type = newDictionaryEntity.getType();
        if (type != null) {
            dictionary.setType(io.gravitee.repository.management.model.DictionaryType.valueOf(type.name()));
        }

        if (type == io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL) {
            dictionary.setProperties(newDictionaryEntity.getProperties());
        } else {
            dictionary.setProvider(convert(newDictionaryEntity.getProvider()));
            dictionary.setTrigger(convert(newDictionaryEntity.getTrigger()));
        }

        return dictionary;
    }

    private DictionaryProvider convert(DictionaryProviderEntity providerEntity) {
        DictionaryProvider provider = null;
        if (providerEntity != null && providerEntity.getType() != null && providerEntity.getConfiguration() != null) {
            provider = new DictionaryProvider();
            provider.setType(providerEntity.getType());
            provider.setConfiguration(providerEntity.getConfiguration().toString());
        }
        return provider;
    }

    private DictionaryProviderEntity convert(DictionaryProvider provider) {
        DictionaryProviderEntity entity = null;

        if (provider != null) {
            entity = new DictionaryProviderEntity();
            entity.setType(provider.getType());
            try {
                entity.setConfiguration(mapper.readTree(provider.getConfiguration()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return entity;
    }

    private DictionaryTrigger convert(DictionaryTriggerEntity triggerEntity) {
        DictionaryTrigger trigger = null;
        if (triggerEntity != null) {
            trigger = new DictionaryTrigger();
            trigger.setRate(triggerEntity.getRate());
            trigger.setUnit(triggerEntity.getUnit());
        }
        return trigger;
    }

    private DictionaryTriggerEntity convert(DictionaryTrigger trigger) {
        DictionaryTriggerEntity entity = null;

        if (trigger != null) {
            entity = new DictionaryTriggerEntity();
            entity.setRate(trigger.getRate());
            entity.setUnit(trigger.getUnit());
        }

        return entity;
    }
}
