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
package io.gravitee.rest.api.service.impl.configuration.dictionary;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DictionaryServiceImpl_UpdateTest {

    private static final String ENVIRONMENT_ID = GraviteeContext.getCurrentEnvironment();
    public static final String DICTIONARY_ID = "dictionaryId";

    @InjectMocks
    private DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private EventService eventService;

    @Mock
    private AuditService auditService;

    @Test
    public void shouldUpdateDictionary() throws TechnicalException {
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
        updatedDictionary.setType(io.gravitee.repository.management.model.DictionaryType.MANUAL);
        when(
            dictionaryRepository.update(
                argThat(
                    arg ->
                        arg.getId() == dictionaryInDb.getId() &&
                        arg.getCreatedAt().equals(dictionaryInDb.getCreatedAt()) &&
                        arg.getState().equals(dictionaryInDb.getState()) &&
                        arg.getEnvironmentId() == ENVIRONMENT_ID &&
                        arg.getName() == updateDictionaryEntity.getName() &&
                        arg.getDescription() == updateDictionaryEntity.getDescription() &&
                        arg.getProperties().equals(updateDictionaryEntity.getProperties()) &&
                        arg.getType().name().equals(updateDictionaryEntity.getType().name())
                )
            )
        )
            .thenReturn(updatedDictionary);

        DictionaryEntity dictionaryEntityUpdated = dictionaryService.update(
            GraviteeContext.getExecutionContext(),
            dictionaryInDb.getId(),
            updateDictionaryEntity
        );
        assertNotNull(dictionaryEntityUpdated);

        verify(dictionaryRepository, times(1)).update(any(Dictionary.class));
        verify(eventService, times(1))
            .create(
                eq(GraviteeContext.getExecutionContext()),
                eq(Collections.singleton(ENVIRONMENT_ID)),
                eq(EventType.START_DICTIONARY),
                eq(null),
                any()
            );
        verify(auditService, times(1))
            .createAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(Dictionary.AuditEvent.DICTIONARY_UPDATED),
                eq(updatedDictionary.getUpdatedAt()),
                any(),
                any()
            );
    }

    @Test(expected = DictionaryNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.empty());

        UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
        updateDictionaryEntity.setName("UpdatedName");
        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);
    }
}
