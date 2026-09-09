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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    @Mock
    private DataEncryptor dataEncryptor;

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
        updateDictionaryEntity.setProperties(Map.of("foo", "bar"));
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
                        arg.getProperties().keySet().equals(Set.of("foo")) &&
                        !arg.getProperties().get("foo").encrypted() &&
                        arg.getProperties().get("foo").value().equals("bar") &&
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
        updateDictionaryEntity.setProperties(Map.of("foo", "bar"));
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
                        arg.getProperties().keySet().equals(Set.of("foo")) &&
                        !arg.getProperties().get("foo").encrypted() &&
                        arg.getProperties().get("foo").value().equals("bar") &&
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
    public void should_keep_value_verbatim_when_the_options_declare_it_encrypted() throws TechnicalException {
        given_stored_dictionary(new HashMap<>());

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", "cipher"),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build())
        );

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(dict -> dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("cipher"))
        );
    }

    @Test
    public void should_store_plain_property_when_no_options_are_sent() throws TechnicalException {
        given_stored_dictionary(new HashMap<>());

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(Map.of("hostname", "api.example.com"), null);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    !dict.getProperties().get("hostname").encrypted() &&
                    dict.getProperties().get("hostname").value().equals("api.example.com")
            )
        );
    }

    @Test
    public void should_not_downgrade_an_encrypted_property_when_the_options_omit_it() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        // What a caller that knows nothing about encryption sends: the property map alone.
        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(Map.of("secret", "cipher"), null);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(argThat(dict -> dict.getProperties().get("secret").encrypted()));
    }

    @Test
    public void should_reject_an_explicit_downgrade_of_an_encrypted_property() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary_without_update_stub(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", "plain-again"),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(false).build())
        );

        assertThatThrownBy(() ->
            dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity)
        ).isInstanceOf(DictionaryPropertyEncryptedToPlainException.class);
        verify(dictionaryRepository, never()).update(any());
    }

    @Test
    public void should_reject_options_naming_a_property_that_does_not_exist() throws TechnicalException {
        given_stored_dictionary_without_update_stub(new HashMap<>());

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("hostname", "api.example.com"),
            Map.of("ghost", DictionaryPropertyOptions.builder().encrypted(true).build())
        );

        assertThatThrownBy(() -> dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity))
            .isInstanceOf(InvalidDictionaryPropertyOptionsException.class)
            .hasMessageContaining("ghost");
        verify(dictionaryRepository, never()).update(any());
    }

    @Test
    public void should_reject_options_asking_for_both_encrypted_and_encryptable() throws TechnicalException {
        given_stored_dictionary_without_update_stub(new HashMap<>());

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", "cipher"),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).encryptable(true).build())
        );

        assertThatThrownBy(() -> dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity))
            .isInstanceOf(InvalidDictionaryPropertyOptionsException.class)
            .hasMessageContaining("cannot both be true");
        verify(dictionaryRepository, never()).update(any());
    }

    @Test
    public void should_report_options_only_for_encrypted_properties() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("plain", new DictionaryProperty("value", false));
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(Map.of("plain", "value", "secret", "cipher"), null);

        DictionaryEntity updated = dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        assertThat(updated.getPropertyOptions()).containsExactlyEntriesOf(
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build())
        );
    }

    @Test
    public void should_return_properties_ordered_by_key() throws TechnicalException {
        given_stored_dictionary(new HashMap<>());

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(Map.of("zebra", "z", "alpha", "a"), null);

        DictionaryEntity updated = dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("alpha", "a");
        expected.put("zebra", "z");
        assertThat(updated.getProperties()).containsExactlyEntriesOf(expected);
    }

    @Test
    public void should_reapply_options_sent_by_an_automation_manifest() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        // A GitOps reconcile resubmits exactly what the read returned.
        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", "cipher"),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build())
        );

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(dict -> dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("cipher"))
        );
    }

    @Test
    public void should_reject_a_property_submitted_without_a_value() throws TechnicalException {
        given_stored_dictionary_without_update_stub(new HashMap<>());

        Map<String, String> properties = new HashMap<>();
        properties.put("hostname", "api.example.com");
        properties.put("broken", null);
        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(properties, null);

        assertThatThrownBy(() -> dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity))
            .isInstanceOf(DictionaryPropertyValueRequiredException.class)
            .hasMessageContaining("broken");
        verify(dictionaryRepository, never()).update(any());
    }

    @Test
    public void should_renew_an_encrypted_property_when_its_value_is_edited_without_options()
        throws TechnicalException, GeneralSecurityException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);
        when(dataEncryptor.encrypt("renewed-plaintext")).thenReturn("renewed-cipher");

        // The Console edits the value and sends no options: the property stays encrypted, and the new
        // plaintext is encrypted on the way in rather than stored as typed.
        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(Map.of("secret", "renewed-plaintext"), null);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("renewed-cipher")
            )
        );
    }

    @Test
    public void should_store_a_renewed_ciphertext_when_the_options_declare_it_encrypted() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", "renewed-cipher"),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build())
        );

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("renewed-cipher")
            )
        );
        verifyNoInteractions(dataEncryptor);
    }

    @Test
    public void should_encrypt_a_value_the_options_mark_encryptable() throws TechnicalException, GeneralSecurityException {
        given_stored_dictionary(new HashMap<>());
        when(dataEncryptor.encrypt("plaintext")).thenReturn("cipher");

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", "plaintext"),
            Map.of("secret", DictionaryPropertyOptions.builder().encryptable(true).build())
        );

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(dict -> dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("cipher"))
        );
    }

    @Test
    public void should_keep_the_stored_ciphertext_when_the_client_echoes_the_mask() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(Map.of("secret", DictionaryServiceImpl.ENCRYPTED_VALUE_MASK), null);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(dict -> dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("cipher"))
        );
        verifyNoInteractions(dataEncryptor);
    }

    @Test
    public void should_reject_an_explicit_downgrade_when_the_client_echoes_the_mask() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary_without_update_stub(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", DictionaryServiceImpl.ENCRYPTED_VALUE_MASK),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(false).build())
        );

        assertThatThrownBy(() ->
            dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity)
        ).isInstanceOf(DictionaryPropertyEncryptedToPlainException.class);
        verify(dictionaryRepository, never()).update(any());
    }

    @Test
    public void should_reject_contradictory_options_when_the_client_echoes_the_mask() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary_without_update_stub(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", DictionaryServiceImpl.ENCRYPTED_VALUE_MASK),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).encryptable(true).build())
        );

        assertThatThrownBy(() ->
            dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity)
        ).isInstanceOf(InvalidDictionaryPropertyOptionsException.class);
        verify(dictionaryRepository, never()).update(any());
    }

    @Test
    public void should_keep_the_stored_ciphertext_when_the_mask_is_echoed_with_encrypted_declared() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("secret", DictionaryServiceImpl.ENCRYPTED_VALUE_MASK),
            Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build())
        );

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(dict -> dict.getProperties().get("secret").encrypted() && dict.getProperties().get("secret").value().equals("cipher"))
        );
    }

    @Test
    public void should_encrypt_a_dynamic_dictionary_value_the_options_mark_encryptable()
        throws TechnicalException, GeneralSecurityException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("fetched-key", new DictionaryProperty("fetched-value", false));
        given_stored_dictionary(stored, io.gravitee.repository.management.model.DictionaryType.DYNAMIC);
        when(dataEncryptor.encrypt("fetched-value")).thenReturn("cipher");

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            Map.of("fetched-key", "fetched-value"),
            Map.of("fetched-key", DictionaryPropertyOptions.builder().encryptable(true).build()),
            DictionaryType.DYNAMIC
        );

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity);

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().get("fetched-key").encrypted() &&
                    dict.getProperties().get("fetched-key").value().equals("cipher")
            )
        );
    }

    @Test
    public void should_keep_the_stored_properties_when_an_update_omits_them() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        stored.put("plain", new DictionaryProperty("value", false));
        given_stored_dictionary(stored);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, anUpdate(null, null));

        verify(dictionaryRepository).update(
            argThat(
                dict ->
                    dict.getProperties().keySet().equals(Set.of("secret", "plain")) &&
                    dict.getProperties().get("secret").encrypted() &&
                    dict.getProperties().get("secret").value().equals("cipher") &&
                    !dict.getProperties().get("plain").encrypted()
            )
        );
    }

    @Test
    public void should_clear_the_stored_properties_when_an_update_sends_an_empty_map() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary(stored);

        dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, anUpdate(Map.of(), null));

        verify(dictionaryRepository).update(argThat(dict -> dict.getProperties().isEmpty()));
    }

    @Test
    public void should_reject_property_options_when_the_update_omits_the_properties() throws TechnicalException {
        Map<String, DictionaryProperty> stored = new HashMap<>();
        stored.put("secret", new DictionaryProperty("cipher", true));
        given_stored_dictionary_without_update_stub(stored);

        UpdateDictionaryEntity updateDictionaryEntity = anUpdate(
            null,
            Map.of("secret", DictionaryPropertyOptions.builder().encryptable(true).build())
        );

        assertThatThrownBy(() ->
            dictionaryService.update(GraviteeContext.getExecutionContext(), DICTIONARY_ID, updateDictionaryEntity)
        ).isInstanceOf(InvalidDictionaryPropertyOptionsException.class);
        verify(dictionaryRepository, never()).update(any());
    }

    private void given_stored_dictionary(Map<String, DictionaryProperty> properties) throws TechnicalException {
        given_stored_dictionary(properties, io.gravitee.repository.management.model.DictionaryType.MANUAL);
    }

    private void given_stored_dictionary(
        Map<String, DictionaryProperty> properties,
        io.gravitee.repository.management.model.DictionaryType type
    ) throws TechnicalException {
        given_stored_dictionary_without_update_stub(properties, type);
        when(dictionaryRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void given_stored_dictionary_without_update_stub(Map<String, DictionaryProperty> properties) throws TechnicalException {
        given_stored_dictionary_without_update_stub(properties, io.gravitee.repository.management.model.DictionaryType.MANUAL);
    }

    private void given_stored_dictionary_without_update_stub(
        Map<String, DictionaryProperty> properties,
        io.gravitee.repository.management.model.DictionaryType type
    ) throws TechnicalException {
        Dictionary existing = new Dictionary();
        existing.setId(DICTIONARY_ID);
        existing.setName("My Dictionary");
        existing.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        existing.setType(type);
        existing.setState(LifecycleState.STOPPED);
        existing.setProperties(properties);

        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(existing));
    }

    private static UpdateDictionaryEntity anUpdate(Map<String, String> properties, Map<String, DictionaryPropertyOptions> options) {
        return anUpdate(properties, options, DictionaryType.MANUAL);
    }

    private static UpdateDictionaryEntity anUpdate(
        Map<String, String> properties,
        Map<String, DictionaryPropertyOptions> options,
        DictionaryType type
    ) {
        UpdateDictionaryEntity updateDictionaryEntity = new UpdateDictionaryEntity();
        updateDictionaryEntity.setName("My Dictionary");
        updateDictionaryEntity.setType(type);
        updateDictionaryEntity.setProperties(properties);
        updateDictionaryEntity.setPropertyOptions(options);
        return updateDictionaryEntity;
    }
}
