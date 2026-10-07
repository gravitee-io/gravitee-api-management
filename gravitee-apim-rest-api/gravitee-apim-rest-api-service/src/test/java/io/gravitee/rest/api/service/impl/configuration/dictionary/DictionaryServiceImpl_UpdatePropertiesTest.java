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

import static io.gravitee.apim.core.utils.EncryptedValueMask.ENCRYPTED_VALUE_MASK;
import static io.gravitee.repository.management.model.Audit.AuditProperties.ENCRYPTED;
import static io.gravitee.repository.management.model.Dictionary.AuditEvent.DICTIONARY_UPDATED;
import static io.gravitee.rest.api.service.impl.configuration.dictionary.DictionaryAuditPatch.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryEncryptionPolicy;
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

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

        when(dataEncryptor.decrypt("previous-cipher")).thenReturn("previous-secret");
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
    public void should_encrypt_a_newly_fetched_property_when_the_toggle_is_on() throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(Map.of(), true);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataEncryptor.encrypt("fresh-value")).thenReturn("fresh-cipher");
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("apiKey", "fresh-value"));

        verify(dictionaryRepository).update(
            argThat(
                dict -> dict.getProperties().get("apiKey").encrypted() && dict.getProperties().get("apiKey").value().equals("fresh-cipher")
            )
        );
    }

    @Test
    public void should_keep_the_rest_of_the_dictionary_updating_when_one_property_fails_to_encrypt()
        throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(Map.of(), true);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataEncryptor.encrypt("good-value")).thenReturn("good-cipher");
        when(dataEncryptor.encrypt("bad-value")).thenThrow(new GeneralSecurityException("boom"));
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("good", "good-value", "bad", "bad-value"));

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("good").encrypted() &&
                    dict.getProperties().get("good").value().equals("good-cipher") &&
                    !dict.getProperties().containsKey("bad")
            )
        );
    }

    @Test
    public void should_keep_the_stored_plain_value_when_a_changed_fetch_fails_to_encrypt()
        throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(Map.of("plain", new DictionaryProperty("old-value", false)), true);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dataEncryptor.encrypt("new-value")).thenThrow(new GeneralSecurityException("boom"));

        DictionaryEntity result = dictionaryService.updateProperties(DICTIONARY_ID, Map.of("plain", "new-value"));

        assertThat(result.getProperties()).containsEntry("plain", "old-value");
        verify(dictionaryRepository, never()).update(any(Dictionary.class));
    }

    @Test
    public void should_keep_a_stored_encrypted_property_when_its_changed_value_fails_to_encrypt_while_another_key_still_updates()
        throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(
            Map.of("apiKey", new DictionaryProperty("old-cipher", true), "region", new DictionaryProperty("old-region", false)),
            null
        );
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataEncryptor.decrypt("old-cipher")).thenReturn("old-secret");
        when(dataEncryptor.encrypt("new-secret")).thenThrow(new GeneralSecurityException("boom"));
        given_environment();

        DictionaryEntity result = dictionaryService.updateProperties(DICTIONARY_ID, Map.of("apiKey", "new-secret", "region", "new-region"));

        assertThat(result.getProperties()).containsEntry("apiKey", ENCRYPTED_VALUE_MASK);
        assertThat(result.getProperties()).containsEntry("region", "new-region");
        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("apiKey").encrypted() &&
                    dict.getProperties().get("apiKey").value().equals("old-cipher") &&
                    !dict.getProperties().get("region").encrypted() &&
                    dict.getProperties().get("region").value().equals("new-region")
            )
        );
    }

    @Test
    public void should_encrypt_an_existing_plain_property_the_next_time_it_is_fetched_when_the_toggle_is_on()
        throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(Map.of("plain", new DictionaryProperty("old-value", false)), true);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataEncryptor.encrypt("old-value")).thenReturn("now-encrypted");
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("plain", "old-value"));

        verify(dictionaryRepository).update(
            argThat(
                dict -> dict.getProperties().get("plain").encrypted() && dict.getProperties().get("plain").value().equals("now-encrypted")
            )
        );
    }

    @Test
    public void should_leave_an_already_encrypted_property_alone_even_when_the_toggle_is_on()
        throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(Map.of("secret", new DictionaryProperty("previous-cipher", true)), true);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dataEncryptor.decrypt("previous-cipher")).thenReturn("previous-cipher");

        DictionaryEntity result = dictionaryService.updateProperties(DICTIONARY_ID, Map.of("secret", "previous-cipher"));

        assertThat(result.getProperties()).containsEntry("secret", ENCRYPTED_VALUE_MASK);
        verify(dictionaryRepository, never()).update(any(Dictionary.class));
        verify(dataEncryptor, never()).encrypt(any());
    }

    @Test
    public void should_leave_a_fetched_property_plain_when_the_toggle_is_off() throws TechnicalException, GeneralSecurityException {
        Dictionary dictionaryInDb = startedDynamicDictionaryWith(Map.of(), false);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(dictionaryInDb));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("apiKey", "fresh-value"));

        verify(dictionaryRepository).update(
            argThat(
                dict -> !dict.getProperties().get("apiKey").encrypted() && dict.getProperties().get("apiKey").value().equals("fresh-value")
            )
        );
        verify(dataEncryptor, never()).encrypt(any());
    }

    private static Dictionary startedDynamicDictionaryWith(Map<String, DictionaryProperty> properties, Boolean encryptOnFetch) {
        Dictionary dictionary = startedDynamicDictionaryWith(properties);
        if (encryptOnFetch != null) {
            DictionaryEncryptionPolicy encryption = new DictionaryEncryptionPolicy();
            encryption.setEncryptOnFetch(encryptOnFetch);
            dictionary.setEncryption(encryption);
        }
        return dictionary;
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
    public void should_skip_the_write_and_publication_when_an_encrypted_value_is_unchanged()
        throws TechnicalException, GeneralSecurityException {
        Dictionary existing = startedDynamicDictionaryWith(Map.of("secret", new DictionaryProperty("ENC(unchanged)", true)));
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dataEncryptor.decrypt("ENC(unchanged)")).thenReturn("unchanged-plaintext");

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("secret", "unchanged-plaintext"));

        verify(dictionaryRepository, never()).update(any(Dictionary.class));
        verify(eventService, never()).createDictionaryEvent(any(), any(), any(), any(), any(Dictionary.class));
        verify(dataEncryptor, never()).encrypt(any());
    }

    @Test
    public void should_skip_the_write_and_publication_when_a_plain_value_is_unchanged() throws TechnicalException {
        Dictionary existing = startedDynamicDictionaryWith(Map.of("plain", new DictionaryProperty("plain-value", false)));
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("plain", "plain-value"));

        verify(dictionaryRepository, never()).update(any(Dictionary.class));
        verify(eventService, never()).createDictionaryEvent(any(), any(), any(), any(), any(Dictionary.class));
    }

    @Test
    public void should_write_and_publish_when_a_key_disappears_from_the_fetched_values() throws TechnicalException {
        Dictionary existing = startedDynamicDictionaryWith(
            Map.of("kept", new DictionaryProperty("value", false), "dropped", new DictionaryProperty("value", false))
        );
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("kept", "value"));

        verify(dictionaryRepository).update(argThat(dict -> dict.getProperties().keySet().equals(Set.of("kept"))));
        verify(eventService).createDictionaryEvent(any(), any(), any(), eq(EventType.PUBLISH_DICTIONARY), any(Dictionary.class));
    }

    @Test
    public void should_mark_audit_as_encrypted_when_the_refresh_drops_the_last_encrypted_property() throws TechnicalException {
        Dictionary existing = startedDynamicDictionaryWith(
            Map.of("secret", new DictionaryProperty("ENC(secret)", true), "plain", new DictionaryProperty("value", false))
        );
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("plain", "value"));

        verify(auditService).createAuditLog(any(), argThat(data -> "true".equals(data.getProperties().get(ENCRYPTED))));
    }

    @Test
    public void should_re_encrypt_when_the_stored_ciphertext_cannot_be_decrypted() throws TechnicalException, GeneralSecurityException {
        Dictionary existing = startedDynamicDictionaryWith(Map.of("secret", new DictionaryProperty("ENC(undecipherable)", true)));
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        when(dataEncryptor.decrypt("ENC(undecipherable)")).thenThrow(new GeneralSecurityException("wrong key"));
        when(dataEncryptor.encrypt("fetched-plaintext")).thenReturn("ENC(fetched-plaintext)");

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("secret", "fetched-plaintext"));

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() &&
                    dict.getProperties().get("secret").value().equals("ENC(fetched-plaintext)")
            )
        );
    }

    @Test
    public void should_audit_the_refresh_against_the_properties_it_replaced() throws TechnicalException {
        Dictionary existing = startedDynamicDictionaryWith(Map.of("plain", new DictionaryProperty("old", false)), false);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("plain", "new"));

        JsonNode patch = auditedPatch();
        assertThat(patch).contains(
            json(
                """
                {"op":"replace","path":"/properties/plain","value":"new"}
                """
            )
        );
        assertThat(patch).noneMatch(operation -> operation.get("path").asText().startsWith("/encryption"));
    }

    @Test
    public void should_not_audit_encrypted_values_on_a_dynamic_refresh() throws TechnicalException, GeneralSecurityException {
        Dictionary existing = startedDynamicDictionaryWith(
            Map.of("secret", new DictionaryProperty("previous-cipher", true), "plain", new DictionaryProperty("previous-value", false))
        );
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();
        when(dataEncryptor.decrypt("previous-cipher")).thenReturn("previous-secret");
        when(dataEncryptor.encrypt("fetched-secret")).thenReturn("ENC(fetched-secret)");

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("secret", "fetched-secret", "plain", "fetched-plain"));

        JsonNode patch = auditedPatch();
        assertThat(patch).contains(
            json(
                """
                {"op":"replace","path":"/properties/plain","value":"fetched-plain"}
                """
            )
        );
        assertThat(patch).noneMatch(operation -> operation.get("path").asText().startsWith("/properties/secret"));
        assertThat(patch.toString()).doesNotContain("previous-cipher").doesNotContain("previous-secret").doesNotContain("fetched-secret");

        ArgumentCaptor<Dictionary> published = ArgumentCaptor.forClass(Dictionary.class);
        verify(eventService).createDictionaryEvent(any(), any(), any(), eq(EventType.PUBLISH_DICTIONARY), published.capture());
        assertThat(published.getValue().getProperties()).containsEntry("secret", new DictionaryProperty("ENC(fetched-secret)", true));
    }

    private JsonNode auditedPatch() {
        ArgumentCaptor<AuditService.AuditLogData> auditLogData = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createAuditLog(any(ExecutionContext.class), auditLogData.capture());
        return DictionaryAuditPatch.of(auditLogData.getValue());
    }

    @Test
    public void should_publish_the_refreshed_properties() throws TechnicalException {
        Dictionary existing = startedDynamicDictionaryWith(Map.of("plain", new DictionaryProperty("old", false)));
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("plain", "new"));

        ArgumentCaptor<Dictionary> published = ArgumentCaptor.forClass(Dictionary.class);
        verify(eventService).createDictionaryEvent(any(), any(), any(), eq(EventType.PUBLISH_DICTIONARY), published.capture());
        assertThat(published.getValue().getProperties()).isEqualTo(Map.of("plain", new DictionaryProperty("new", false)));
    }

    @Test
    public void should_re_encrypt_when_the_stored_value_is_not_base64() throws TechnicalException, GeneralSecurityException {
        Dictionary existing = startedDynamicDictionaryWith(Map.of("secret", new DictionaryProperty("not base64!", true)));
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given_environment();

        when(dataEncryptor.decrypt("not base64!")).thenThrow(new IllegalArgumentException("Illegal base64 character 20"));
        when(dataEncryptor.encrypt("fetched-plaintext")).thenReturn("ENC(fetched-plaintext)");

        dictionaryService.updateProperties(DICTIONARY_ID, Map.of("secret", "fetched-plaintext"));

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() &&
                    dict.getProperties().get("secret").value().equals("ENC(fetched-plaintext)")
            )
        );
        verify(eventService).createDictionaryEvent(any(), any(), any(), eq(EventType.PUBLISH_DICTIONARY), any(Dictionary.class));
    }

    private void given_environment() {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environment);
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
