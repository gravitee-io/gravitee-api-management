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
import static io.gravitee.repository.management.model.Dictionary.AuditEvent.DICTIONARY_DELETED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
public class DictionaryServiceImpl_DeleteTest {

    private static final String ENVIRONMENT_ID = GraviteeContext.getCurrentEnvironment();
    private static final String DICTIONARY_ID = "dictionaryId";

    @InjectMocks
    private DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private EventService eventService;

    @Mock
    private AuditService auditService;

    @Test
    public void should_audit_the_deletion() throws TechnicalException {
        given_stored_dictionary(Map.of("plain", new DictionaryProperty("value", false)));

        dictionaryService.delete(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent().equals(DICTIONARY_DELETED) &&
                    "my-dict".equals(auditLogData.getProperties().get(DICTIONARY)) &&
                    auditLogData.getNewValue() == null
            )
        );
    }

    @Test
    public void should_mark_the_deletion_as_encrypted_when_a_property_is_encrypted() throws TechnicalException {
        given_stored_dictionary(Map.of("secret", new DictionaryProperty("cipher", true)));

        dictionaryService.delete(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> "true".equals(auditLogData.getProperties().get(DICTIONARY_ENCRYPTED)))
        );
    }

    @Test
    public void should_not_mark_the_deletion_as_encrypted_when_no_property_is_encrypted() throws TechnicalException {
        given_stored_dictionary(Map.of("plain", new DictionaryProperty("value", false)));

        dictionaryService.delete(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> !auditLogData.getProperties().containsKey(DICTIONARY_ENCRYPTED))
        );
    }

    @Test
    public void should_not_audit_a_dictionary_of_another_environment() throws TechnicalException {
        Dictionary stored = aDictionary(Map.of());
        stored.setEnvironmentId("another-environment");
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(stored));

        assertThrows(DictionaryNotFoundException.class, () ->
            dictionaryService.delete(GraviteeContext.getExecutionContext(), DICTIONARY_ID)
        );

        verify(auditService, never()).createAuditLog(any(), any());
    }

    private void given_stored_dictionary(Map<String, DictionaryProperty> properties) throws TechnicalException {
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(aDictionary(properties)));
    }

    private static Dictionary aDictionary(Map<String, DictionaryProperty> properties) {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(DICTIONARY_ID);
        dictionary.setName("my-dict");
        dictionary.setEnvironmentId(ENVIRONMENT_ID);
        dictionary.setType(DictionaryType.MANUAL);
        dictionary.setProperties(properties);
        return dictionary;
    }
}
