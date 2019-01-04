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
package io.gravitee.management.service.impl.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.configuration.dictionary.*;
import io.gravitee.management.service.AuditService;
import io.gravitee.management.service.EventService;
import io.gravitee.management.service.configuration.dictionary.DictionaryService;
import io.gravitee.management.service.exceptions.DictionaryAlreadyExistsException;
import io.gravitee.management.service.exceptions.DictionaryNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.AbstractService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.DICTIONARY;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DictionaryServiceImpl extends AbstractService implements DictionaryService {

    private final Logger LOGGER = LoggerFactory.getLogger(DictionaryServiceImpl.class);

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public Set<DictionaryEntity> findAll() {
        try {
            return dictionaryRepository
                    .findAll()
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to retrieve dictionaries", ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to retrieve dictionaries", ex);
        }
    }

    @Override
    public DictionaryEntity deploy(String id) {
        try {
            LOGGER.debug("Deploy dictionary {}", id);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(id);
            if (!optDictionary.isPresent()) {
                throw new DictionaryNotFoundException(id);
            }

            // add deployment date
            Dictionary dictionary = optDictionary.get();
            dictionary.setUpdatedAt(new Date());
            dictionary.setDeployedAt(dictionary.getUpdatedAt());

            dictionary = dictionaryRepository.update(dictionary);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), id);

            // And create event
            eventService.create(EventType.PUBLISH_DICTIONARY, mapper.writeValueAsString(dictionary), properties);
            return convert(dictionary);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to deploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to deploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity undeploy(String id) {
        try {
            LOGGER.debug("Undeploy dictionary {}", id);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(id);
            if (!optDictionary.isPresent()) {
                throw new DictionaryNotFoundException(id);
            }

            // add deployment date
            Dictionary dictionary = optDictionary.get();
            dictionary.setUpdatedAt(new Date());

            dictionary = dictionaryRepository.update(dictionary);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), id);

            // And create event
            eventService.create(EventType.UNPUBLISH_DICTIONARY, mapper.writeValueAsString(dictionary), properties);
            return convert(dictionary);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to undeploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity start(String id) {
        try {
            LOGGER.debug("Start dictionary {}", id);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(id);
            if (!optDictionary.isPresent()) {
                throw new DictionaryNotFoundException(id);
            }

            // add deployment date
            Dictionary dictionary = optDictionary.get();
            dictionary.setUpdatedAt(new Date());
            dictionary.setState(LifecycleState.STARTED);

            dictionary = dictionaryRepository.update(dictionary);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), id);

            // And create event
            eventService.create(EventType.START_DICTIONARY, null, properties);

            // Audit
            createAuditLog(
                    Dictionary.AuditEvent.DICTIONARY_UPDATED,
                    dictionary.getCreatedAt(),
                    optDictionary.get(),
                    dictionary);

            return convert(dictionary);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to undeploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity stop(String id) {
        try {
            LOGGER.debug("Stop dictionary {}", id);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(id);
            if (!optDictionary.isPresent()) {
                throw new DictionaryNotFoundException(id);
            }

            // add deployment date
            Dictionary dictionary = optDictionary.get();
            dictionary.setUpdatedAt(new Date());
            dictionary.setState(LifecycleState.STOPPED);

            dictionary = dictionaryRepository.update(dictionary);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), id);

            // And create event
            eventService.create(EventType.STOP_DICTIONARY, null, properties);

            // Audit
            createAuditLog(
                    Dictionary.AuditEvent.DICTIONARY_UPDATED,
                    dictionary.getCreatedAt(),
                    optDictionary.get(),
                    dictionary);

            return convert(dictionary);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to undeploy dictionary {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity create(NewDictionaryEntity newDictionaryEntity) {
        try {
            LOGGER.debug("Create dictionary {}", newDictionaryEntity);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(
                    IdGenerator.generate(newDictionaryEntity.getName()));
            if (optDictionary.isPresent()) {
                throw new DictionaryAlreadyExistsException(newDictionaryEntity.getName());
            }

            Dictionary dictionary = convert(newDictionaryEntity);

            // Set date fields
            dictionary.setCreatedAt(new Date());
            dictionary.setState(LifecycleState.STOPPED);
            dictionary.setUpdatedAt(dictionary.getCreatedAt());

            Dictionary createdDictionary = dictionaryRepository.create(dictionary);

            createAuditLog(Dictionary.AuditEvent.DICTIONARY_CREATED, dictionary.getCreatedAt(), null, dictionary);
            return convert(createdDictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create dictionary {}", newDictionaryEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newDictionaryEntity, ex);
        }
    }

    @Override
    public DictionaryEntity update(String id, UpdateDictionaryEntity updateDictionaryEntity) {
        try {
            LOGGER.debug("Update dictionary {}", updateDictionaryEntity);

            Optional<Dictionary> optDictionary = dictionaryRepository.findById(id);

            if (!optDictionary.isPresent()) {
                throw new DictionaryNotFoundException(updateDictionaryEntity.getName());
            }

            Dictionary dictionary = convert(updateDictionaryEntity);

            dictionary.setId(id);
            dictionary.setCreatedAt(optDictionary.get().getCreatedAt());
            dictionary.setUpdatedAt(new Date());
            dictionary.setState(optDictionary.get().getState());

            Dictionary updatedDictionary =  dictionaryRepository.update(dictionary);

            // Audit
            createAuditLog(
                    Dictionary.AuditEvent.DICTIONARY_UPDATED,
                    dictionary.getCreatedAt(),
                    optDictionary.get(),
                    updatedDictionary);

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update dictionary {}", updateDictionaryEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to update " + updateDictionaryEntity, ex);
        }
    }

    @Override
    public DictionaryEntity findById(String id) {
        try {
            LOGGER.debug("Find dictionary by ID: {}", id);

            Optional<Dictionary> dictionary = dictionaryRepository.findById(id);

            if (dictionary.isPresent()) {
                return convert(dictionary.get());
            }

            throw new DictionaryNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete a dictionary using its ID {}", id, ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to delete a dictionary using its ID " + id, ex);
        }
    }

    @Override
    public void delete(String id) {
        try {
            LOGGER.debug("Delete dictionary: {}", id);

            Optional<Dictionary> dictionary = dictionaryRepository.findById(id);

            if (!dictionary.isPresent()) {
                throw new DictionaryNotFoundException(id);
            }

            // Force un-deployment
            if (dictionary.get().getType() == DictionaryType.MANUAL) {
                undeploy(id);
            }

            this.stop(id);

            dictionaryRepository.delete(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a dictionary using its ID {}", id, ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to find a dictionary using its ID " + id, ex);
        }
    }

    private void createAuditLog(Audit.AuditEvent event, Date createdAt, Dictionary oldValue, Dictionary newValue) {
        String dictionaryName = oldValue != null ? oldValue.getName() : newValue.getName();

            auditService.createPortalAuditLog(
                    Collections.singletonMap(DICTIONARY, dictionaryName),
                    event,
                    createdAt,
                    oldValue,
                    newValue
            );
    }

    private DictionaryEntity convert(Dictionary dictionary) {
        DictionaryEntity dictionaryEntity = new DictionaryEntity();

        dictionaryEntity.setId(dictionary.getId());
        dictionaryEntity.setName(dictionary.getName());
        dictionaryEntity.setDescription((dictionary.getDescription()));
        dictionaryEntity.setCreatedAt(dictionary.getCreatedAt());
        dictionaryEntity.setUpdatedAt(dictionary.getUpdatedAt());
        dictionaryEntity.setDeployedAt(dictionary.getDeployedAt());
        dictionaryEntity.setType(io.gravitee.management.model.configuration.dictionary.DictionaryType.valueOf(dictionary.getType().name()));
        dictionaryEntity.setProperties(dictionary.getProperties());

        if (dictionary.getType() == DictionaryType.DYNAMIC) {
            dictionaryEntity.setProvider(convert(dictionary.getProvider()));
            dictionaryEntity.setTrigger(convert(dictionary.getTrigger()));
        }

        dictionaryEntity.setState(Lifecycle.State.valueOf(dictionary.getState().name()));

        return dictionaryEntity;
    }

    private Dictionary convert(UpdateDictionaryEntity updateDictionaryEntity) {
        Dictionary dictionary = new Dictionary();

        dictionary.setName(updateDictionaryEntity.getName());
        dictionary.setDescription(updateDictionaryEntity.getDescription());
        dictionary.setProperties(updateDictionaryEntity.getProperties());

        final io.gravitee.management.model.configuration.dictionary.DictionaryType type = updateDictionaryEntity.getType();
        if (type != null) {
            dictionary.setType(io.gravitee.repository.management.model.DictionaryType.valueOf(type.name()));
        }

        if (type == io.gravitee.management.model.configuration.dictionary.DictionaryType.DYNAMIC) {
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

        final io.gravitee.management.model.configuration.dictionary.DictionaryType type = newDictionaryEntity.getType();
        if (type != null) {
            dictionary.setType(io.gravitee.repository.management.model.DictionaryType.valueOf(type.name()));
        }

        if (type == io.gravitee.management.model.configuration.dictionary.DictionaryType.MANUAL) {
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
