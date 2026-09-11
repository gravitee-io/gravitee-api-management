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

import static io.gravitee.repository.management.model.Dictionary.AuditEvent.DICTIONARY_UPDATED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProperty;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DictionaryServiceImpl_UpdateTest {

    private static final String ENVIRONMENT_ID = GraviteeContext.getCurrentEnvironment();
    private static final String ORGANIZATION_ID = GraviteeContext.getCurrentOrganization();
    public static final String DICTIONARY_ID = "dictionaryId";

    @InjectMocks
    private DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventService eventService;

    @Mock
    private AuditService auditService;

    @Test
    public void should_update_dictionary() throws TechnicalException {
        Dictionary dictionaryInDb = new Dictionary();
        dictionaryInDb.setId(DICTIONARY_ID);
        dictionaryInDb.setCreatedAt(new Date());
        dictionaryInDb.setState(LifecycleState.STARTED);
        dictionaryInDb.setEnvironmentId(ENVIRONMENT_ID);
        when(dictionaryRepository.findById(dictionaryInDb.getId())).thenReturn(Optional.of(dictionaryInDb));

        UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
        updateDictionaryEntity.setName("UpdatedName");
        updateDictionaryEntity.setDescription("UpdatedDescription");
        updateDictionaryEntity.setProperties(Collections.singletonMap("foo", "bar"));
        updateDictionaryEntity.setType(DictionaryType.MANUAL);

        Dictionary updatedDictionary = new Dictionary();
        updatedDictionary.setId(DICTIONARY_ID);
        updatedDictionary.setUpdatedAt(new Date());
        updatedDictionary.setState(LifecycleState.STARTED);
        updatedDictionary.setEnvironmentId(ENVIRONMENT_ID);
        updatedDictionary.setType(io.gravitee.repository.management.model.DictionaryType.MANUAL);
        when(
            dictionaryRepository.update(
                argThat(
                    arg ->
                        arg.getId().equals(dictionaryInDb.getId()) &&
                        arg.getCreatedAt().equals(dictionaryInDb.getCreatedAt()) &&
                        arg.getState().equals(dictionaryInDb.getState()) &&
                        arg.getEnvironmentId().equals(ENVIRONMENT_ID) &&
                        arg.getName().equals(updateDictionaryEntity.getName()) &&
                        arg.getDescription().equals(updateDictionaryEntity.getDescription()) &&
                        arg.getProperties().keySet().equals(updateDictionaryEntity.getProperties().keySet()) &&
                        arg
                            .getProperties()
                            .entrySet()
                            .stream()
                            .allMatch(
                                e ->
                                    !e.getValue().encrypted() &&
                                    e.getValue().value().equals(updateDictionaryEntity.getProperties().get(e.getKey()))
                            ) &&
                        arg.getType().name().equals(updateDictionaryEntity.getType().name())
                )
            )
        ).thenReturn(updatedDictionary);

        DictionaryEntity dictionaryEntityUpdated = dictionaryService.update(
            GraviteeContext.getExecutionContext(),
            dictionaryInDb.getId(),
            updateDictionaryEntity
        );
        assertNotNull(dictionaryEntityUpdated);

        verify(dictionaryRepository, times(1)).update(any(Dictionary.class));
        verify(eventService, never()).createDynamicDictionaryEvent(
            eq(GraviteeContext.getExecutionContext()),
            eq(Collections.singleton(ENVIRONMENT_ID)),
            eq(ORGANIZATION_ID),
            eq(EventType.START_DICTIONARY),
            eq("dictionaryId")
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent().equals(DICTIONARY_UPDATED) &&
                    auditLogData.getCreatedAt().equals(updatedDictionary.getUpdatedAt())
            )
        );
    }

    @Test
    public void should_update_dynamic_dictionary() throws TechnicalException {
        Dictionary dictionaryInDb = new Dictionary();
        dictionaryInDb.setId(DICTIONARY_ID);
        dictionaryInDb.setCreatedAt(new Date());
        dictionaryInDb.setState(LifecycleState.STARTED);
        dictionaryInDb.setEnvironmentId(ENVIRONMENT_ID);
        when(dictionaryRepository.findById(dictionaryInDb.getId())).thenReturn(Optional.of(dictionaryInDb));

        UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
        updateDictionaryEntity.setName("UpdatedName");
        updateDictionaryEntity.setDescription("UpdatedDescription");
        updateDictionaryEntity.setProperties(Collections.singletonMap("foo", "bar"));
        updateDictionaryEntity.setType(DictionaryType.DYNAMIC);

        Dictionary updatedDictionary = new Dictionary();
        updatedDictionary.setId(DICTIONARY_ID);
        updatedDictionary.setUpdatedAt(new Date());
        updatedDictionary.setState(LifecycleState.STARTED);
        updatedDictionary.setEnvironmentId(ENVIRONMENT_ID);
        updatedDictionary.setType(io.gravitee.repository.management.model.DictionaryType.DYNAMIC);
        when(
            dictionaryRepository.update(
                argThat(
                    arg ->
                        arg.getId().equals(dictionaryInDb.getId()) &&
                        arg.getCreatedAt().equals(dictionaryInDb.getCreatedAt()) &&
                        arg.getState().equals(dictionaryInDb.getState()) &&
                        arg.getEnvironmentId().equals(ENVIRONMENT_ID) &&
                        arg.getName().equals(updateDictionaryEntity.getName()) &&
                        arg.getDescription().equals(updateDictionaryEntity.getDescription()) &&
                        arg.getProperties().keySet().equals(updateDictionaryEntity.getProperties().keySet()) &&
                        arg
                            .getProperties()
                            .entrySet()
                            .stream()
                            .allMatch(
                                e ->
                                    !e.getValue().encrypted() &&
                                    e.getValue().value().equals(updateDictionaryEntity.getProperties().get(e.getKey()))
                            ) &&
                        arg.getType().name().equals(updateDictionaryEntity.getType().name())
                )
            )
        ).thenReturn(updatedDictionary);

        DictionaryEntity dictionaryEntityUpdated = dictionaryService.update(
            GraviteeContext.getExecutionContext(),
            dictionaryInDb.getId(),
            updateDictionaryEntity
        );
        assertNotNull(dictionaryEntityUpdated);

        verify(dictionaryRepository, times(1)).update(any(Dictionary.class));
        verify(eventService, times(1)).createDynamicDictionaryEvent(
            eq(GraviteeContext.getExecutionContext()),
            eq(Collections.singleton(ENVIRONMENT_ID)),
            eq(ORGANIZATION_ID),
            eq(EventType.START_DICTIONARY),
            eq("dictionaryId")
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent().equals(DICTIONARY_UPDATED) &&
                    auditLogData.getCreatedAt().equals(updatedDictionary.getUpdatedAt())
            )
        );
    }

    @Test
    public void should_not_update_because_does_not_belong_to_environment() throws TechnicalException {
        assertThrows(DictionaryNotFoundException.class, () -> {
            Dictionary dictionaryInDb = new Dictionary();
            dictionaryInDb.setId(DICTIONARY_ID);
            dictionaryInDb.setCreatedAt(new Date());
            dictionaryInDb.setState(LifecycleState.STARTED);
            dictionaryInDb.setEnvironmentId("Another_environment");
            when(dictionaryRepository.findById(dictionaryInDb.getId())).thenReturn(Optional.of(dictionaryInDb));

            UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
            updateDictionaryEntity.setName("UpdatedName");
            dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);
        });
    }

    @Test
    public void should_not_update_because_not_found() throws TechnicalException {
        assertThrows(DictionaryNotFoundException.class, () -> {
            when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.empty());

            UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
            updateDictionaryEntity.setName("UpdatedName");
            dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);
        });
    }

    @Test
    public void should_preserve_encrypted_flag_when_value_unchanged() throws TechnicalException {
        Dictionary existing = new Dictionary();
        existing.setId(DICTIONARY_ID);
        existing.setName("My Dictionary");
        existing.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        existing.setType(io.gravitee.repository.management.model.DictionaryType.MANUAL);
        existing.setState(LifecycleState.STOPPED);
        Map<String, DictionaryProperty> existingProperties = new HashMap<>();
        existingProperties.put("already-encrypted", new DictionaryProperty("ENC(cipher)", true));
        existingProperties.put("plain", new DictionaryProperty("plain-value", false));
        existing.setProperties(existingProperties);

        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
        updateDictionaryEntity.setName("My Dictionary");
        updateDictionaryEntity.setType(DictionaryType.MANUAL);
        Map<String, String> incoming = new HashMap<>();
        incoming.put("already-encrypted", "ENC(cipher)"); // resubmitted unchanged, e.g. Console round-trip
        incoming.put("plain", "new-plain-value"); // genuinely changed
        updateDictionaryEntity.setProperties(incoming);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("already-encrypted").encrypted() &&
                    dict.getProperties().get("already-encrypted").value().equals("ENC(cipher)") &&
                    !dict.getProperties().get("plain").encrypted() &&
                    dict.getProperties().get("plain").value().equals("new-plain-value")
            )
        );
    }
}
