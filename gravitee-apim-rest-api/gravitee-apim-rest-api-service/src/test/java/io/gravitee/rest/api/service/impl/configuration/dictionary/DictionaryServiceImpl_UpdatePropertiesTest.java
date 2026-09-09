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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.security.GeneralSecurityException;
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
public class DictionaryServiceImpl_UpdatePropertiesTest {

    private static final String DICTIONARY_ID = "dictionaryId";
    private static final String ENVIRONMENT_ID = "my-specific-environment";
    private static final String ORGANIZATION_ID = "my-specific-organization";

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

    @Mock
    private DataEncryptor dataEncryptor;

    @Test
    public void should_update_properties_using_dictionary_environment() throws TechnicalException {
        Dictionary dictionaryInDb = new Dictionary();
        dictionaryInDb.setId(DICTIONARY_ID);
        dictionaryInDb.setCreatedAt(new Date());
        dictionaryInDb.setUpdatedAt(new Date());
        dictionaryInDb.setState(LifecycleState.STARTED);
        dictionaryInDb.setEnvironmentId(ENVIRONMENT_ID);
        dictionaryInDb.setType(DictionaryType.DYNAMIC);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));

        Dictionary updatedDictionary = new Dictionary();
        updatedDictionary.setId(DICTIONARY_ID);
        updatedDictionary.setUpdatedAt(new Date());
        updatedDictionary.setState(LifecycleState.STARTED);
        updatedDictionary.setEnvironmentId(ENVIRONMENT_ID);
        updatedDictionary.setType(DictionaryType.DYNAMIC);
        when(dictionaryRepository.update(any(Dictionary.class))).thenReturn(updatedDictionary);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);

        Map<String, String> newProperties = Map.of("key1", "value1", "key2", "value2");

        DictionaryEntity result = dictionaryService.updateProperties(DICTIONARY_ID, newProperties);
        assertNotNull(result);

        verify(environmentService).findById(ENVIRONMENT_ID);

        ExecutionContext expectedContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        verify(eventService).createDictionaryEvent(
            eq(expectedContext),
            eq(Collections.singleton(ENVIRONMENT_ID)),
            eq(ORGANIZATION_ID),
            eq(EventType.PUBLISH_DICTIONARY),
            any(Dictionary.class)
        );
        verify(auditService).createAuditLog(
            eq(expectedContext),
            argThat(auditLogData -> auditLogData.getEvent().equals(DICTIONARY_UPDATED))
        );
    }

    @Test
    public void should_reapply_stored_classification_to_freshly_fetched_values() throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(
            Map.of("secret", new DictionaryProperty("previous-cipher", true), "plain", new DictionaryProperty("previous-value", false))
        );
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);

        when(dataEncryptor.encrypt("fetched-secret")).thenReturn("ENC(fetched-secret)");

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("secret", "fetched-secret", "plain", "fetched-plain"));

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() &&
                    dict.getProperties().get("secret").value().equals("ENC(fetched-secret)") &&
                    !dict.getProperties().get("plain").encrypted() &&
                    dict.getProperties().get("plain").value().equals("fetched-plain")
            )
        );
    }

    @Test
    public void should_reject_a_fetched_property_without_a_value() throws TechnicalException {
        Dictionary dictionaryInDb = new Dictionary();
        dictionaryInDb.setId(DICTIONARY_ID);
        dictionaryInDb.setCreatedAt(new Date());
        dictionaryInDb.setState(LifecycleState.STARTED);
        dictionaryInDb.setEnvironmentId(ENVIRONMENT_ID);
        dictionaryInDb.setType(DictionaryType.DYNAMIC);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));

        Map<String, String> fetched = new HashMap<>();
        fetched.put("valid", "value");
        fetched.put("broken", null);

        assertThatThrownBy(() -> dictionaryService.updateProperties(DICTIONARY_ID, fetched))
            .isInstanceOf(DictionaryPropertyValueRequiredException.class)
            .hasMessageContaining("broken");
        verify(dictionaryRepository, never()).update(any(Dictionary.class));
    }

    @Test
    public void should_not_update_properties_because_not_found() throws TechnicalException {
        assertThrows(DictionaryNotFoundException.class, () -> {
            when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.empty());

            dictionaryService.updateProperties(DICTIONARY_ID, Map.of("key", "value"));
        });
    }

    @Test
    public void should_re_encrypt_sticky_key_on_refresh_even_when_nothing_changed() throws TechnicalException, GeneralSecurityException {
        Dictionary existing = new Dictionary();
        existing.setId(DICTIONARY_ID);
        existing.setState(LifecycleState.STARTED);
        existing.setEnvironmentId(ENVIRONMENT_ID);
        existing.setType(DictionaryType.DYNAMIC);
        Map<String, DictionaryProperty> existingProperties = new HashMap<>();
        existingProperties.put("secret", new DictionaryProperty("ENC(old-cipher-of-unchanged-plaintext)", true));
        existing.setProperties(existingProperties);

        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);

        when(dataEncryptor.encrypt("unchanged-plaintext")).thenReturn("ENC(old-cipher-of-unchanged-plaintext)");

        Map<String, String> freshlyFetched = Map.of("secret", "unchanged-plaintext");

        dictionaryService.updateProperties(DICTIONARY_ID, freshlyFetched);

        // No skip-on-no-change: every refresh persists and republishes, even when the re-derived
        // ciphertext is identical to what's already stored. See
        // should_retry_publication_when_a_previous_attempt_persisted_but_failed_to_publish for why.
        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() &&
                    dict.getProperties().get("secret").value().equals("ENC(old-cipher-of-unchanged-plaintext)")
            )
        );
        verify(eventService).createDictionaryEvent(any(), any(), any(), eq(EventType.PUBLISH_DICTIONARY), any(Dictionary.class));
    }

    @Test
    public void should_retry_publication_when_a_previous_attempt_persisted_but_failed_to_publish() throws TechnicalException {
        // Simulates the state left behind by a prior refresh tick that persisted the new value but then
        // failed to publish it: storage already reflects the fresh value, so a naive "did the resolved
        // value change?" check would see no difference and skip republishing forever.
        Dictionary existing = new Dictionary();
        existing.setId(DICTIONARY_ID);
        existing.setState(LifecycleState.STARTED);
        existing.setEnvironmentId(ENVIRONMENT_ID);
        existing.setType(DictionaryType.DYNAMIC);
        Map<String, DictionaryProperty> existingProperties = new HashMap<>();
        existingProperties.put("plain", new DictionaryProperty("plain-value", false));
        existing.setProperties(existingProperties);

        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);

        Map<String, String> freshlyFetched = Map.of("plain", "plain-value");

        dictionaryService.updateProperties(DICTIONARY_ID, freshlyFetched);

        verify(dictionaryRepository).update(any(Dictionary.class));
        verify(eventService).createDictionaryEvent(any(), any(), any(), eq(EventType.PUBLISH_DICTIONARY), any(Dictionary.class));
    }

    @Test
    public void should_re_encrypt_sticky_key_on_refresh_when_the_underlying_value_genuinely_changed()
        throws TechnicalException, GeneralSecurityException {
        Dictionary existing = new Dictionary();
        existing.setId(DICTIONARY_ID);
        existing.setState(LifecycleState.STARTED);
        existing.setEnvironmentId(ENVIRONMENT_ID);
        existing.setType(DictionaryType.DYNAMIC);
        Map<String, DictionaryProperty> existingProperties = new HashMap<>();
        existingProperties.put("secret", new DictionaryProperty("ENC(old-cipher-of-unchanged-plaintext)", true));
        existing.setProperties(existingProperties);

        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);

        when(dataEncryptor.encrypt("new-plaintext")).thenReturn("ENC(new-cipher)");

        Map<String, String> freshlyFetched = Map.of("secret", "new-plaintext");

        dictionaryService.updateProperties(DICTIONARY_ID, freshlyFetched);

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("ENC(new-cipher)")
            )
        );
    }

    private static Dictionary startedDynamicDictionaryWith(Map<String, DictionaryProperty> properties) {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(DICTIONARY_ID);
        dictionary.setCreatedAt(new Date());
        dictionary.setState(LifecycleState.STARTED);
        dictionary.setEnvironmentId(ENVIRONMENT_ID);
        dictionary.setType(DictionaryType.DYNAMIC);
        dictionary.setProperties(new HashMap<>(properties));
        return dictionary;
    }
}
