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
import static io.gravitee.repository.management.model.Audit.AuditProperties.DICTIONARY_ENCRYPTED;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProvider;
import io.gravitee.repository.management.model.DictionaryTrigger;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class DictionaryServiceImpl extends AbstractService implements DictionaryService {

    @Lazy
    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private DataEncryptor dataEncryptor;

    /**
     * Server-owned sentinel returned on read in place of an encrypted value. On write it means "leave
     * the stored ciphertext alone", so it is only accepted for a key already stored encrypted.
     */
    static final String ENCRYPTED_VALUE_MASK = "••••••••••••";

    @Override
    public Set<DictionaryEntity> findAll(ExecutionContext executionContext) {
        try {
            return dictionaryRepository
                .findAllByEnvironments(Collections.singleton(executionContext.getEnvironmentId()))
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to retrieve dictionaries", ex);
        }
    }

    @Override
    public DictionaryEntity deploy(ExecutionContext executionContext, String id) {
        try {
            log.debug("Deploy dictionary {}", id);

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
            throw new TechnicalManagementException("An error occurs while trying to deploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity undeploy(ExecutionContext executionContext, String id) {
        try {
            log.debug("Undeploy dictionary {}", id);

            Dictionary dictionary = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(id));

            // add deployment date
            dictionary.setUpdatedAt(new Date());
            dictionary.setDeployedAt(null);

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
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity start(ExecutionContext executionContext, String id) {
        try {
            log.debug("Start dictionary {}", id);

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
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity stop(ExecutionContext executionContext, String id) {
        try {
            log.debug("Stop dictionary {}", id);

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
            throw new TechnicalManagementException("An error occurs while trying to undeploy " + id, ex);
        }
    }

    @Override
    public DictionaryEntity create(ExecutionContext executionContext, NewDictionaryEntity newDictionaryEntity) {
        try {
            log.debug("Create dictionary name={} key={}", newDictionaryEntity.getName(), newDictionaryEntity.getKey());
            final Dictionary dictionary;
            if (newDictionaryEntity.getKey() == null) {
                String key = IdGenerator.generate(newDictionaryEntity.getName());
                Optional<Dictionary> idDictionary = dictionaryRepository.findById(key);
                ensureRuntimeKeyIsAvailable(executionContext, key, newDictionaryEntity.getName(), idDictionary, false);
                dictionary = convert(newDictionaryEntity, key, idDictionary.isEmpty());
            } else {
                String key = newDictionaryEntity.getKey();
                Optional<Dictionary> idDictionary = dictionaryRepository.findById(key);
                ensureRuntimeKeyIsAvailable(executionContext, key, newDictionaryEntity.getName(), idDictionary, true);
                dictionary = convert(newDictionaryEntity, key, false);
                // if ID is already set, let's use it
                if (newDictionaryEntity.getId() != null) {
                    dictionary.setId(newDictionaryEntity.getId());
                }
            }
            // Convert by setting a UUID as ID and key if not an id in the DB

            dictionary.setEnvironmentId(executionContext.getEnvironmentId());

            // Set date fields
            dictionary.setCreatedAt(new Date());
            dictionary.setState(LifecycleState.STOPPED);
            dictionary.setUpdatedAt(dictionary.getCreatedAt());

            Dictionary createdDictionary = dictionaryRepository.create(dictionary);

            createAuditLog(executionContext, Dictionary.AuditEvent.DICTIONARY_CREATED, dictionary.getCreatedAt(), null, dictionary);
            return convert(createdDictionary);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to create dictionary '" + newDictionaryEntity.getName() + "'",
                ex
            );
        }
    }

    @Override
    public DictionaryEntity update(ExecutionContext executionContext, String id, UpdateDictionaryEntity updateDictionaryEntity) {
        try {
            log.debug("Update dictionary id={} name={}", id, updateDictionaryEntity.getName());

            Dictionary dictionaryToUpdate = dictionaryRepository
                .findById(id)
                .filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
                .orElseThrow(() -> new DictionaryNotFoundException(updateDictionaryEntity.getName()));

            Dictionary updatedDictionary = dictionaryRepository.update(
                withUnmanagedFieldsOf(convert(updateDictionaryEntity, dictionaryToUpdate), id, dictionaryToUpdate)
            );

            restartIfAlreadyStarted(executionContext, id, updatedDictionary);
            createAuditLog(
                executionContext,
                Dictionary.AuditEvent.DICTIONARY_UPDATED,
                updatedDictionary.getUpdatedAt(),
                dictionaryToUpdate,
                updatedDictionary
            );

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to update dictionary '" + updateDictionaryEntity.getName() + "'",
                ex
            );
        }
    }

    private static Dictionary withUnmanagedFieldsOf(Dictionary dictionary, String id, Dictionary stored) {
        dictionary.setId(id);
        dictionary.setKey(stored.getKey());
        dictionary.setCreatedAt(stored.getCreatedAt());
        dictionary.setEnvironmentId(stored.getEnvironmentId());
        dictionary.setUpdatedAt(new Date());
        dictionary.setState(stored.getState());
        return dictionary;
    }

    /** A running dynamic dictionary needs a fresh start event to pick the update up. */
    private void restartIfAlreadyStarted(ExecutionContext executionContext, String id, Dictionary dictionary) {
        if (dictionary.getType() != DictionaryType.DYNAMIC || dictionary.getState() != LifecycleState.STARTED) {
            return;
        }
        eventService.createDynamicDictionaryEvent(
            executionContext,
            Collections.singleton(executionContext.getEnvironmentId()),
            executionContext.getOrganizationId(),
            EventType.START_DICTIONARY,
            id
        );
    }

    @Override
    public DictionaryEntity updateProperties(final String id, final Map<String, String> properties) {
        try {
            log.debug("Update dynamic dictionary properties {}", id);

            Dictionary dictionary = dictionaryRepository.findById(id).orElseThrow(() -> new DictionaryNotFoundException(id));
            if (dictionary.getState() != LifecycleState.STARTED) {
                log.warn("Update dictionary {} properties not applied: dictionary is {}", id, dictionary.getState());
                return convert(dictionary);
            }
            Map<String, DictionaryProperty> refreshed = toFetchedProperties(id, properties, dictionary.getProperties());
            if (Objects.equals(refreshed, dictionary.getProperties())) {
                return convert(dictionary);
            }

            dictionary.setProperties(refreshed);
            dictionary.setUpdatedAt(new Date());
            dictionary.setDeployedAt(dictionary.getUpdatedAt());
            Dictionary updatedDictionary = dictionaryRepository.update(dictionary);

            publishRefreshedProperties(dictionary, updatedDictionary);

            return convert(updatedDictionary);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to update dictionary '" + id + "' properties", ex);
        }
    }

    /**
     * Publishes and audits a refresh in the dictionary's own environment, which the refresher — a
     * scheduled job with no execution context of its own — cannot supply.
     */
    private void publishRefreshedProperties(Dictionary dictionary, Dictionary updatedDictionary) {
        EnvironmentEntity environment = environmentService.findById(dictionary.getEnvironmentId());
        ExecutionContext executionContext = new ExecutionContext(environment.getOrganizationId(), environment.getId());

        eventService.createDictionaryEvent(
            executionContext,
            Collections.singleton(executionContext.getEnvironmentId()),
            executionContext.getOrganizationId(),
            EventType.PUBLISH_DICTIONARY,
            dictionary
        );
        createAuditLog(
            executionContext,
            Dictionary.AuditEvent.DICTIONARY_UPDATED,
            updatedDictionary.getUpdatedAt(),
            dictionary,
            updatedDictionary
        );
    }

    @Override
    public DictionaryEntity findById(ExecutionContext executionContext, String id) {
        try {
            log.debug("Find dictionary by ID: {}", id);
            Optional<Dictionary> byId = dictionaryRepository.findById(id);
            if (executionContext.hasEnvironmentId()) {
                byId = byId.filter(d -> d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()));
            }
            return byId.map(this::convert).orElseThrow(() -> new DictionaryNotFoundException(id));
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete a dictionary using its ID " + id, ex);
        }
    }

    @Override
    public Optional<DictionaryEntity> findByKeyAndEnvironment(ExecutionContext executionContext, String key) {
        try {
            log.debug("Find dictionary by key: {} and environment: {}", key, executionContext.getEnvironmentId());
            return dictionaryRepository.findByKeyAndEnvironment(key, executionContext.getEnvironmentId()).map(this::convert);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find a dictionary using its key " + key, ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String id) {
        try {
            log.debug("Delete dictionary: {}", id);

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

        Map<Audit.AuditProperties, String> auditProperties = new EnumMap<>(Audit.AuditProperties.class);
        auditProperties.put(DICTIONARY, dictionaryName);
        if (hasEncryptedProperty(oldValue) || hasEncryptedProperty(newValue)) {
            auditProperties.put(DICTIONARY_ENCRYPTED, Boolean.TRUE.toString());
        }

        auditService.createAuditLog(
            executionContext,
            AuditService.AuditLogData.builder()
                .properties(auditProperties)
                .event(event)
                .createdAt(createdAt)
                .oldValue(oldValue)
                .newValue(newValue)
                .build()
        );
    }

    private static boolean hasEncryptedProperty(Dictionary dictionary) {
        if (dictionary == null || dictionary.getProperties() == null) {
            return false;
        }
        return dictionary.getProperties().values().stream().filter(Objects::nonNull).anyMatch(DictionaryProperty::encrypted);
    }

    private DictionaryEntity convert(Dictionary dictionary) {
        DictionaryEntity.DictionaryEntityBuilder dictionaryEntityBuilder = DictionaryEntity.builder()
            .id(dictionary.getId())
            .name(dictionary.getName())
            .key(dictionary.getKey())
            .description(dictionary.getDescription())
            .createdAt(dictionary.getCreatedAt())
            .updatedAt(dictionary.getUpdatedAt())
            .deployedAt(dictionary.getDeployedAt())
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.valueOf(dictionary.getType().name()))
            .properties(toFlatProperties(dictionary.getProperties()))
            .propertyOptions(toPropertyOptions(dictionary.getProperties()))
            .state(Lifecycle.State.valueOf(dictionary.getState().name()));

        if (dictionary.getType() == DictionaryType.DYNAMIC) {
            dictionaryEntityBuilder.provider(convert(dictionary.getProvider())).trigger(convert(dictionary.getTrigger()));
        }

        return dictionaryEntityBuilder.build();
    }

    /**
     * Console dictionaries store a null key, so the gateway indexes them by id. Reject create when
     * {@code key} is already used in this environment as an id or as a key.
     */
    private void ensureRuntimeKeyIsAvailable(
        ExecutionContext executionContext,
        String key,
        String dictionaryName,
        Optional<Dictionary> dictionaryById,
        boolean explicitKey
    ) throws TechnicalException {
        dictionaryById
            .filter(d -> d.getEnvironmentId() != null && d.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()))
            .ifPresent(existing -> {
                if (explicitKey) {
                    throw new DictionaryKeyCollidesWithIdException(key, existing.getId(), existing.getName());
                }
                throw new DictionaryAlreadyExistsException(dictionaryName);
            });
        if (dictionaryRepository.findByKeyAndEnvironment(key, executionContext.getEnvironmentId()).isPresent()) {
            throw new DictionaryAlreadyExistsException(dictionaryName);
        }
    }

    /**
     * Wraps the submitted properties into the stored typed form.
     *
     * <p>Values come from {@code properties} alone; {@code propertyOptions} only says how each one is
     * classified. A key the options do not mention keeps the classification it already has, so a
     * caller can save a dictionary it only partly edited — and a caller that knows nothing about
     * encryption cannot take it away.
     *
     * <p>Omitting {@code properties} altogether leaves the stored ones alone, so an edit confined to
     * the provider or the trigger cannot silently drop a value or its encrypted classification. An
     * empty map still means "clear them", which is how the last property is deleted.
     */
    private Map<String, DictionaryProperty> toTypedProperties(
        String dictionaryId,
        Map<String, String> properties,
        Map<String, DictionaryPropertyOptions> options,
        Map<String, DictionaryProperty> existing
    ) {
        if (properties == null) {
            rejectOptionsWithoutProperty(Map.of(), options);
            return existing;
        }
        rejectOptionsWithoutProperty(properties, options);
        rejectValuelessProperties(properties);
        return properties
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry ->
                    toTypedProperty(dictionaryId, entry, optionsFor(options, entry.getKey()), existing)
                )
            );
    }

    private static void rejectValuelessProperties(Map<String, String> properties) {
        properties
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() == null)
            .findFirst()
            .ifPresent(entry -> {
                throw new DictionaryPropertyValueRequiredException(entry.getKey());
            });
    }

    private static DictionaryPropertyOptions optionsFor(Map<String, DictionaryPropertyOptions> options, String key) {
        return options == null ? null : options.get(key);
    }

    private static void rejectOptionsWithoutProperty(Map<String, String> properties, Map<String, DictionaryPropertyOptions> options) {
        if (options == null) {
            return;
        }
        options
            .keySet()
            .stream()
            .filter(key -> !properties.containsKey(key))
            .findFirst()
            .ifPresent(key -> {
                throw new InvalidDictionaryPropertyOptionsException(key, "there is no such property");
            });
    }

    private DictionaryProperty toTypedProperty(
        String dictionaryId,
        Map.Entry<String, String> property,
        DictionaryPropertyOptions options,
        Map<String, DictionaryProperty> existing
    ) {
        DictionaryProperty stored = existing == null ? null : existing.get(property.getKey());
        boolean storedEncrypted = stored != null && stored.encrypted();
        rejectContradictoryOptions(property.getKey(), options);
        rejectMaskUnlessStoredEncrypted(property.getKey(), property.getValue(), storedEncrypted);

        boolean desiredEncrypted = desiredEncrypted(options, storedEncrypted);
        if (!desiredEncrypted) {
            if (storedEncrypted) {
                throw new DictionaryPropertyEncryptedToPlainException(dictionaryId, property.getKey());
            }
            return new DictionaryProperty(property.getValue(), false);
        }
        if (storedEncrypted && ENCRYPTED_VALUE_MASK.equals(property.getValue())) {
            return stored;
        }
        if (options != null && Boolean.TRUE.equals(options.getEncrypted())) {
            return new DictionaryProperty(property.getValue(), true);
        }
        if (storedEncrypted && Objects.equals(stored.value(), property.getValue())) {
            // must precede the decrypt-and-compare below, or a raw ciphertext resend is encrypted twice
            return stored;
        }
        return encryptUnlessStoredDecryptsTo(dictionaryId, property.getKey(), property.getValue(), stored);
    }

    private static void rejectContradictoryOptions(String key, DictionaryPropertyOptions options) {
        if (options != null && Boolean.TRUE.equals(options.getEncrypted()) && Boolean.TRUE.equals(options.getEncryptable())) {
            throw new InvalidDictionaryPropertyOptionsException(
                key,
                "'encrypted' and 'encryptable' cannot both be true — the value is either already ciphertext or plaintext to encrypt"
            );
        }
    }

    private static void rejectMaskUnlessStoredEncrypted(String key, String value, boolean storedEncrypted) {
        if (!storedEncrypted && ENCRYPTED_VALUE_MASK.equals(value)) {
            throw new DictionaryPropertyMaskedValueException(key);
        }
    }

    private static boolean desiredEncrypted(DictionaryPropertyOptions options, boolean storedEncrypted) {
        if (options == null) {
            return storedEncrypted;
        }
        if (Boolean.TRUE.equals(options.getEncryptable())) {
            return true;
        }
        return options.getEncrypted() == null ? storedEncrypted : options.getEncrypted();
    }

    private String encryptOrFail(String dictionaryId, String key, String value) {
        try {
            return dataEncryptor.encrypt(value);
        } catch (GeneralSecurityException e) {
            throw new TechnicalManagementException(
                "Failed to encrypt dictionary property [" + key + "] on dictionary [" + dictionaryId + "]",
                e
            );
        }
    }

    /**
     * Re-applies each key's stored classification to the value the provider just fetched. The fetch
     * carries plaintext only, so it can neither declare nor change a classification. A fetch that
     * yields a property without a value fails the refresh, for the same reason the write path rejects
     * one.
     */
    private Map<String, DictionaryProperty> toFetchedProperties(
        String dictionaryId,
        Map<String, String> fetched,
        Map<String, DictionaryProperty> existing
    ) {
        if (fetched == null) {
            return null;
        }
        rejectValuelessProperties(fetched);
        return fetched
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry ->
                    toFetchedProperty(dictionaryId, entry, existing == null ? null : existing.get(entry.getKey()))
                )
            );
    }

    /** An encrypted key keeps that classification across a refresh, which is what makes it sticky. */
    private DictionaryProperty toFetchedProperty(String dictionaryId, Map.Entry<String, String> fetched, DictionaryProperty stored) {
        if (stored == null || !stored.encrypted()) {
            return new DictionaryProperty(fetched.getValue(), false);
        }
        return encryptUnlessStoredDecryptsTo(dictionaryId, fetched.getKey(), fetched.getValue(), stored);
    }

    /**
     * Reuses the stored ciphertext when it still decrypts to {@code plaintext}, so resubmitting or
     * re-fetching an unchanged secret is a no-op whatever the cipher mode. Ciphertext that no longer
     * decrypts, from a rotated secret or a corrupt value, counts as changed.
     */
    private DictionaryProperty encryptUnlessStoredDecryptsTo(String dictionaryId, String key, String plaintext, DictionaryProperty stored) {
        if (stored != null && stored.encrypted() && Objects.equals(decryptStoredValue(dictionaryId, key, stored.value()), plaintext)) {
            return stored;
        }
        return new DictionaryProperty(encryptOrFail(dictionaryId, key, plaintext), true);
    }

    private String decryptStoredValue(String dictionaryId, String key, String ciphertext) {
        try {
            return dataEncryptor.decrypt(ciphertext);
        } catch (GeneralSecurityException e) {
            log.warn(
                "Stored value of dictionary property [{}] on dictionary [{}] could not be decrypted; it will be encrypted again",
                key,
                dictionaryId
            );
            return null;
        }
    }

    /**
     * Flattens the stored properties for the wire, ordered by key so a client diffing successive
     * reads — a GitOps reconcile in particular — sees no drift from the storage layer's map ordering.
     *
     * <p>An encrypted value is replaced by {@link #ENCRYPTED_VALUE_MASK}: the ciphertext never leaves
     * through this read.
     */
    private static Map<String, String> toFlatProperties(Map<String, DictionaryProperty> typed) {
        if (typed == null) {
            return null;
        }
        return typed
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .sorted(Map.Entry.comparingByKey())
            .collect(LinkedHashMap::new, (flat, entry) -> flat.put(entry.getKey(), flatValue(entry.getValue())), LinkedHashMap::putAll);
    }

    private static String flatValue(DictionaryProperty property) {
        if (property.encrypted()) {
            return ENCRYPTED_VALUE_MASK;
        }
        return property.value();
    }

    /**
     * Reports only the properties whose classification is not the default, so a plain dictionary
     * carries no options at all and a reader sees an entry exactly where something is encrypted.
     */
    private static Map<String, DictionaryPropertyOptions> toPropertyOptions(Map<String, DictionaryProperty> typed) {
        if (typed == null) {
            return null;
        }
        return typed
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().encrypted())
            .sorted(Map.Entry.comparingByKey())
            .collect(
                LinkedHashMap::new,
                (options, entry) -> options.put(entry.getKey(), DictionaryPropertyOptions.builder().encrypted(true).build()),
                LinkedHashMap::putAll
            );
    }

    private Dictionary convert(UpdateDictionaryEntity updateDictionaryEntity, Dictionary existing) {
        Dictionary dictionary = new Dictionary();

        dictionary.setName(updateDictionaryEntity.getName());
        dictionary.setDescription(updateDictionaryEntity.getDescription());
        dictionary.setProperties(
            toTypedProperties(
                existing.getId(),
                updateDictionaryEntity.getProperties(),
                updateDictionaryEntity.getPropertyOptions(),
                existing.getProperties()
            )
        );

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

    private Dictionary convert(NewDictionaryEntity newDictionaryEntity, String key, boolean keyIsUniqueAcrossAllEnvs) {
        Dictionary dictionary = new Dictionary();
        // This is for legacy dictionaries to be backward compatible
        dictionary.setId(keyIsUniqueAcrossAllEnvs ? key : UuidString.generateRandom());
        dictionary.setKey(keyIsUniqueAcrossAllEnvs ? null : key);

        dictionary.setName(newDictionaryEntity.getName());
        dictionary.setDescription(newDictionaryEntity.getDescription());

        final io.gravitee.rest.api.model.configuration.dictionary.DictionaryType type = newDictionaryEntity.getType();
        if (type != null) {
            dictionary.setType(io.gravitee.repository.management.model.DictionaryType.valueOf(type.name()));
        }

        if (type == io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL) {
            dictionary.setProperties(
                toTypedProperties(dictionary.getId(), newDictionaryEntity.getProperties(), newDictionaryEntity.getPropertyOptions(), null)
            );
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
                log.error(e.getMessage(), e);
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
