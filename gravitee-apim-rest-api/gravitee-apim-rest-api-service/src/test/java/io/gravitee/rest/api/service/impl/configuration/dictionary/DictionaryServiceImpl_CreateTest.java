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

import static io.gravitee.repository.management.model.Audit.AuditProperties.ENCRYPTED;
import static io.gravitee.repository.management.model.Dictionary.AuditEvent.DICTIONARY_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryPropertyOptions;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
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

    @Mock
    private Appender<ILoggingEvent> appender;

    @Mock
    private DataEncryptor dataEncryptor;

    @BeforeEach
    public void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(DictionaryServiceImpl.class);
        logger.addAppender(appender);
    }

    @Test
    public void should_reject_a_null_property_value() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        Map<String, String> properties = new HashMap<>();
        properties.put("hostname", null);
        newDictionary.setProperties(properties);

        assertThatThrownBy(() -> dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary))
            .isInstanceOf(DictionaryPropertyValueRequiredException.class)
            .hasMessageContaining("hostname");

        verify(dictionaryRepository, never()).create(any(Dictionary.class));
    }

    @Test
    public void should_create_with_explicit_key_legacy() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("foo", "bar"));

        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.empty());
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DictionaryEntity result = dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        assertThat(result.getId()).isNotBlank().isNotEqualTo("my-key");
        assertThat(result.getKey()).isEqualTo("my-key");
        assertThat(result.getName()).isEqualTo("My Dictionary");
        assertThat(result.getProperties()).containsExactlyEntriesOf(Map.of("foo", "bar"));

        verify(dictionaryRepository).create(argThat(dict -> "my-key".equals(dict.getKey()) && dict.getId() != null));
        verify(auditService).createAuditLog(any(), argThat(data -> data.getEvent().equals(DICTIONARY_CREATED)));
    }

    @Test
    public void should_create_with_explicit_key_when_id_already_taken() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("foo", "bar"));

        Dictionary existingById = new Dictionary();
        existingById.setId("my-key");
        existingById.setEnvironmentId("OTHER_ENV");
        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.of(existingById));
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DictionaryEntity result = dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        assertThat(result.getKey()).isEqualTo("my-key");
        assertThat(result.getId()).isNotEqualTo("my-key");

        verify(dictionaryRepository).create(argThat(dict -> !"my-key".equals(dict.getId()) && "my-key".equals(dict.getKey())));
    }

    @Test
    public void should_create_with_generated_key_when_key_is_null() throws TechnicalException {
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
    public void should_not_create_when_key_already_exists_in_same_environment() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);

        Dictionary existing = new Dictionary();
        existing.setEnvironmentId(ENVIRONMENT_ID);
        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.empty());
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.of(existing));

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        assertThatThrownBy(() -> dictionaryService.create(executionContext, newDictionary)).isInstanceOf(
            DictionaryAlreadyExistsException.class
        );
    }

    @Test
    public void should_not_create_when_explicit_key_matches_existing_id_in_same_environment() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("idp-server-details");
        newDictionary.setName("tf_idp-server-details");
        newDictionary.setType(DictionaryType.MANUAL);

        Dictionary existingById = new Dictionary();
        existingById.setId("idp-server-details");
        existingById.setName("idp-server-details");
        existingById.setEnvironmentId(ENVIRONMENT_ID);
        when(dictionaryRepository.findById("idp-server-details")).thenReturn(Optional.of(existingById));

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        assertThatThrownBy(() -> dictionaryService.create(executionContext, newDictionary))
            .isInstanceOf(DictionaryKeyCollidesWithIdException.class)
            .hasMessage(
                "A dictionary with key [idp-server-details] cannot be created because dictionary [idp-server-details] already uses that value as its id in this environment."
            );
    }

    @Test
    public void should_create_dictionary_with_encrypted_property_declared_by_caller() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("plain", "plain-value", "secret", "cipher"));
        newDictionary.setPropertyOptions(Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build()));

        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.empty());
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        verify(dictionaryRepository).create(
            argThat(
                dict ->
                    dict.getProperties().get("secret").encrypted() &&
                    dict.getProperties().get("secret").value().equals("cipher") &&
                    !dict.getProperties().get("plain").encrypted() &&
                    dict.getProperties().get("plain").value().equals("plain-value")
            )
        );
    }

    @Test
    public void should_not_log_encrypted_property_values_on_create() throws Exception {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("secret", "super-secret-value"));
        newDictionary.setPropertyOptions(Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build()));

        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.empty());
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any(Dictionary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataEncryptor.encrypt("super-secret-value")).thenReturn("ENC(cipher)");

        dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary);

        verify(appender, never()).doAppend(argThat(event -> event.getFormattedMessage().contains("super-secret-value")));
    }

    @Test
    public void should_mark_audit_as_encrypted_when_a_property_is_encrypted() throws Exception {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setName("my-dict");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("secret", "s3cr3t"));
        newDictionary.setPropertyOptions(Map.of("secret", DictionaryPropertyOptions.builder().encryptable(true).build()));

        when(dictionaryRepository.findById(any())).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataEncryptor.encrypt("s3cr3t")).thenReturn("ENCRYPTED");

        dictionaryService.create(new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID), newDictionary);

        verify(auditService).createAuditLog(
            any(ExecutionContext.class),
            argThat(data -> "true".equals(data.getProperties().get(ENCRYPTED)))
        );
    }

    @Test
    public void should_not_mark_audit_as_encrypted_when_no_property_is_encrypted() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setName("my-dict");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("plain", "value"));

        when(dictionaryRepository.findById(any())).thenReturn(Optional.empty());
        when(dictionaryRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        dictionaryService.create(new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID), newDictionary);

        verify(auditService).createAuditLog(any(ExecutionContext.class), argThat(data -> !data.getProperties().containsKey(ENCRYPTED)));
    }

    @Test
    public void should_set_stopped_state_on_create() throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.DYNAMIC);

        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.empty());
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

    static Stream<Arguments> optionsSentWithTheMask() {
        return Stream.of(
            Arguments.of("no options", null),
            Arguments.of("encrypted", Map.of("secret", DictionaryPropertyOptions.builder().encrypted(true).build())),
            Arguments.of("encryptable", Map.of("secret", DictionaryPropertyOptions.builder().encryptable(true).build()))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optionsSentWithTheMask")
    public void should_reject_the_mask_on_create(String description, Map<String, DictionaryPropertyOptions> options)
        throws TechnicalException {
        NewDictionaryEntity newDictionary = new NewDictionaryEntity();
        newDictionary.setKey("my-key");
        newDictionary.setName("My Dictionary");
        newDictionary.setType(DictionaryType.MANUAL);
        newDictionary.setProperties(Map.of("secret", DictionaryServiceImpl.ENCRYPTED_VALUE_MASK));
        newDictionary.setPropertyOptions(options);

        when(dictionaryRepository.findById("my-key")).thenReturn(Optional.empty());
        when(dictionaryRepository.findByKeyAndEnvironment("my-key", ENVIRONMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dictionaryService.create(GraviteeContext.getExecutionContext(), newDictionary))
            .isInstanceOf(DictionaryPropertyMaskedValueException.class)
            .hasMessageContaining("secret");
        verify(dictionaryRepository, never()).create(any(Dictionary.class));
        verifyNoInteractions(dataEncryptor);
    }
}
