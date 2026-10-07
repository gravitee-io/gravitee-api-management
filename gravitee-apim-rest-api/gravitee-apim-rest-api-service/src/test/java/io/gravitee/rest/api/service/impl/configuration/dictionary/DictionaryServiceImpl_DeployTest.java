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
import static io.gravitee.repository.management.model.Audit.AuditProperties.ENCRYPTED;
import static io.gravitee.repository.management.model.Dictionary.AuditEvent.DICTIONARY_ENCRYPTED_PROPERTIES_ACCESSED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;
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
public class DictionaryServiceImpl_DeployTest {

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
    public void should_audit_encrypted_properties_access_when_deploying_a_dictionary_holding_an_encrypted_property()
        throws TechnicalException {
        Dictionary stored = given_stored_dictionary(
            Map.of("secret", new DictionaryProperty("cipher", true), "plain", new DictionaryProperty("value", false))
        );

        dictionaryService.deploy(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent() == DICTIONARY_ENCRYPTED_PROPERTIES_ACCESSED &&
                    auditLogData.getProperties().equals(Map.of(DICTIONARY, "my-dict", ENCRYPTED, "true")) &&
                    auditLogData.getCreatedAt().equals(stored.getDeployedAt()) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue() == null
            )
        );
    }

    @Test
    public void should_not_audit_a_dictionary_without_encrypted_property() throws TechnicalException {
        given_stored_dictionary(Map.of("plain", new DictionaryProperty("value", false)));

        dictionaryService.deploy(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        verify(auditService, never()).createAuditLog(any(), any());
    }

    @Test
    public void should_not_audit_a_dictionary_without_properties() throws TechnicalException {
        given_stored_dictionary(null);

        dictionaryService.deploy(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        verify(auditService, never()).createAuditLog(any(), any());
    }

    @Test
    public void should_not_audit_a_dictionary_of_another_environment() throws TechnicalException {
        Dictionary stored = aDictionary(Map.of("secret", new DictionaryProperty("cipher", true)));
        stored.setEnvironmentId("another-environment");
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(stored));

        assertThrows(DictionaryNotFoundException.class, () ->
            dictionaryService.deploy(GraviteeContext.getExecutionContext(), DICTIONARY_ID)
        );

        verify(auditService, never()).createAuditLog(any(), any());
    }

    private Dictionary given_stored_dictionary(Map<String, DictionaryProperty> properties) throws TechnicalException {
        Dictionary stored = aDictionary(properties);
        when(dictionaryRepository.findById(DICTIONARY_ID)).thenReturn(Optional.of(stored));
        when(dictionaryRepository.update(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return stored;
    }

    private static Dictionary aDictionary(Map<String, DictionaryProperty> properties) {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(DICTIONARY_ID);
        dictionary.setName("my-dict");
        dictionary.setEnvironmentId(ENVIRONMENT_ID);
        dictionary.setType(DictionaryType.MANUAL);
        dictionary.setState(LifecycleState.STARTED);
        dictionary.setProperties(properties);
        return dictionary;
    }
}
