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

import static io.gravitee.repository.management.model.Dictionary.AuditEvent.DICTIONARY_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DictionaryServiceImpl_CreateTest {

    private static final String ENVIRONMENT_ID = GraviteeContext.getCurrentEnvironment();

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
    public void shouldCreateWithExplicitKeyLegacy() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("foo", "bar"));

        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DictionaryEntity result = dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        assertThat(result.getId()).isNotBlank().isNotEqualTo("my-key");
        assertThat(result.getKey()).isEqualTo("my-key");
        assertThat(result.getName()).isEqualTo("My Dictionary");
        assertThat(result.getProperties()).containsEntry("foo", "bar");

        verify(dictionaryRepository).create(argThat(dict -> "my-key".equals(dict.getKey()) && dict.getId() != null));
        verify(auditService).createAuditLog(any(), argThat(data -> data.getEvent().equals(DICTIONARY_CREATED)));
    }

    @Test
    public void shouldCreateWithExplicitKeyWhenIdAlreadyTaken() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("foo", "bar"));

        Dictionary existingById = new Dictionary();
        existingById.setId("my-key");
        existingById.setEnvironmentId("OTHER_ENV");
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DictionaryEntity result = dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        assertThat(result.getKey()).isEqualTo("my-key");
        assertThat(result.getId()).isNotEqualTo("my-key");

        verify(dictionaryRepository).create(argThat(dict -> !"my-key".equals(dict.getId()) && "my-key".equals(dict.getKey())));
    }

    @Test
    public void shouldCreateWithGeneratedKeyWhenKeyIsNull() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("foo", "bar"));

        when(dictionaryRepository.findById(any())).thenReturn(Optional.empty());
        when(dictionaryRepository.findByKeyAndEnvironment(any(), any())).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DictionaryEntity result = dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("My Dictionary");
    }

    @Test
    public void shouldNotCreateWhenKeyAlreadyExistsInSameEnvironment() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);

        Dictionary existing = new Dictionary();
        existing.setEnvironmentId(ENVIRONMENT_ID);
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.of(existing));

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        assertThatThrownBy(() -> dictionaryService.create(executionContext, newDictionary)).isInstanceOf(
            DictionaryAlreadyExistsException.class
        );
    }

    @Test
    public void shouldNotCreateWhenIdAlreadyExistsInSameEnvironment() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);

        Dictionary existingById = new Dictionary();
        existingById.setKey("my-key");
        existingById.setEnvironmentId(ENVIRONMENT_ID);
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.of(existingById));

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        assertThatThrownBy(() -> dictionaryService.create(executionContext, newDictionary)).isInstanceOf(
            DictionaryAlreadyExistsException.class
        );
    }

    @Test
    public void shouldSetStoppedStateOnCreate() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.DYNAMIC);

        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        verify(dictionaryRepository).create(
            argThat(
                dict ->
                    dict.getState() == LifecycleState.STOPPED &&
                    dict.getCreatedAt() != null &&
                    dict.getUpdatedAt() != null &&
                    ENVIRONMENT_ID.equals(dict.getEnvironmentId())
            )
        );
    }
}
