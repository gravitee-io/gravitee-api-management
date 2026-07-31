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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_DELETED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static io.gravitee.rest.api.model.parameters.Key.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ParameterServiceTest {

    @InjectMocks
    private ParameterService parameterService = new ParameterServiceImpl();

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private BrandedSendersEnvironmentReader brandedSendersEnvironmentReader;

    @Mock
    private EventManager eventManager;

    @Mock
    private EnvironmentService environmentService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Node node;

    @Mock
    private CommandRepository commandRepository;

    @BeforeEach
    public void init() {
        GraviteeContext.getCurrentParameters().clear();
        when(node.id()).thenReturn("test-node-id");
    }

    @Test
    public void shouldServeBrandedSendersFromYamlAsSystemParameter() throws TechnicalException {
        final String json = "[{\"domains\":[\"example.com\"],\"from\":\"noreply@example.com\",\"subject\":\"[Example] %s\"}]";
        when(brandedSendersEnvironmentReader.read()).thenReturn(of(json));

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // A gravitee.yml value is served at SYSTEM scope and wins over the console (env/org) value.
        assertEquals(List.of(json), values);
        verifyNoInteractions(parameterRepository);
    }

    @Test
    public void shouldFallBackToDefaultWhenNoBrandedSendersInYaml() throws TechnicalException {
        when(brandedSendersEnvironmentReader.read()).thenReturn(empty());
        when(parameterRepository.findById(eq(EMAIL_BRANDED_SENDERS.key()), any(), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            empty()
        );

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ORGANIZATION
        );

        assertEquals(List.of(EMAIL_BRANDED_SENDERS.defaultValue()), values);
    }

    @Test
    public void shouldRouteFlatBrandedSendersThroughTheReaderNotVerbatim() throws TechnicalException {
        // Even when the flat property is present, the value must come from the reader (normalised via parse -> write)
        // rather than the raw environment string, so the flat and native forms behave identically.
        final String normalized = "[{\"domains\":[\"example.com\"],\"from\":\"noreply@example.com\",\"subject\":\"[Example] %s\"}]";
        lenient().when(environment.containsProperty(EMAIL_BRANDED_SENDERS.key())).thenReturn(true);
        lenient().when(environment.getProperty(EMAIL_BRANDED_SENDERS.key())).thenReturn("raw-unescaped-value");
        when(brandedSendersEnvironmentReader.read()).thenReturn(of(normalized));

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(List.of(normalized), values);
    }

    @Test
    public void shouldNotPersistBrandedSendersSaveWhenConfiguredInYaml() throws TechnicalException {
        // A yaml-configured (system-controlled) branded_senders must short-circuit save() and NOT be persisted to
        // org/env storage; otherwise a stale value would survive once the yaml entry is removed. containsProperty is
        // false for the native list, so save() must resolve it through the reader, mirroring getSystemParameter().
        final String systemValue = "[{\"domains\":[\"example.com\"],\"from\":\"noreply@example.com\"}]";
        when(brandedSendersEnvironmentReader.read()).thenReturn(of(systemValue));
        when(parameterRepository.findById(eq(EMAIL_BRANDED_SENDERS.key()), any(), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            empty()
        );

        final Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            "[{\"domains\":[\"other.com\"],\"from\":\"x@other.com\"}]",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ORGANIZATION
        );

        assertEquals(systemValue, result.getValue());
        verify(parameterRepository, never()).create(any());
        verify(parameterRepository, never()).update(any());
    }

    @Test
    public void shouldFindAllWithCache() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, times(1)).findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void shouldInvalidateCacheOnSave() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));
        when(parameterRepository.update(any())).thenAnswer(i -> i.getArguments()[0]);

        // First call to fill cache
        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // Save to invalidate cache
        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api3",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // Call again, should call repository again
        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, times(3)).findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void shouldFindAllKeysWithCache() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2");

        when(parameterRepository.findByKeys(anyList(), eq("DEFAULT"), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            Collections.singletonList(parameter)
        );

        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(PORTAL_TOP_APIS),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(PORTAL_TOP_APIS),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, times(1)).findByKeys(anyList(), eq("DEFAULT"), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldFindAllFromEnvVar() throws TechnicalException {
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api1,api2");

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            API_LABELS_DICTIONARY,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2"), values);
        verify(parameterRepository, times(0)).findById(any(), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldNotFindAllIfNotOverridable() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2;api3");

        when(environment.containsProperty(PORTAL_TOP_APIS.key())).thenReturn(true);
        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            Optional.of(parameter)
        );

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2", "api3"), values);
        verify(parameterRepository, times(1)).findById(any(), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldFindAllWithFilter() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2;;api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            value -> value,
            value -> !value.isEmpty(),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2", "api1"), values);
    }

    @Test
    public void shouldFindAllWithFilterFromEnvVar() throws TechnicalException {
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api1,api2,api1");

        final List<String> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            API_LABELS_DICTIONARY,
            value -> value,
            value -> !value.isEmpty(),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2", "api1"), values);
        verify(parameterRepository, times(0)).findById(any(), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldFindAllKeysWithFilter() throws TechnicalException {
        final Key p1key = PORTAL_TOP_APIS;
        final Key p2key = PORTAL_ANALYTICS_ENABLED;
        final Key p3key = PORTAL_ANALYTICS_TRACKINGID;
        final Parameter parameter1 = new Parameter();
        parameter1.setKey(PORTAL_TOP_APIS.key());
        parameter1.setValue("api1;api2;;api1");

        final Parameter parameter2 = new Parameter();
        parameter2.setKey(PORTAL_ANALYTICS_ENABLED.key());
        parameter2.setValue("api3;api4;;api5");

        final Parameter parameter3 = new Parameter();
        parameter3.setKey(PORTAL_ANALYTICS_TRACKINGID.key());

        when(
            parameterRepository.findByKeys(
                Arrays.asList(PORTAL_TOP_APIS.key(), PORTAL_ANALYTICS_ENABLED.key(), PORTAL_ANALYTICS_TRACKINGID.key()),
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Arrays.asList(parameter1, parameter2, parameter3));

        final Map<String, List<String>> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            Arrays.asList(p1key, p2key, p3key),
            value -> value,
            value -> !value.isEmpty(),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2", "api1"), values.get(p1key.key()));
        assertEquals(asList("api3", "api4", "api5"), values.get(p2key.key()));
        assertTrue(values.get(p3key.key()).isEmpty());
    }

    @Test
    public void shouldFindAllKeysWithFilterMapper() throws TechnicalException {
        final Key p1key = PORTAL_TOP_APIS;
        final Key p2key = PORTAL_ANALYTICS_ENABLED;
        final Key p3key = PORTAL_ANALYTICS_TRACKINGID;
        final Parameter parameter1 = new Parameter();
        parameter1.setKey(PORTAL_TOP_APIS.key());
        parameter1.setValue("api1;api2 ;; api1");

        final Parameter parameter2 = new Parameter();
        parameter2.setKey(PORTAL_ANALYTICS_ENABLED.key());
        parameter2.setValue("api3;api4 ;; api5");

        final Parameter parameter3 = new Parameter();
        parameter3.setKey(PORTAL_ANALYTICS_TRACKINGID.key());

        when(
            parameterRepository.findByKeys(
                Arrays.asList(PORTAL_TOP_APIS.key(), PORTAL_ANALYTICS_ENABLED.key(), PORTAL_ANALYTICS_TRACKINGID.key()),
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Arrays.asList(parameter1, parameter2, parameter3));

        final Map<String, List<String>> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            Arrays.asList(p1key, p2key, p3key),
            value -> value.trim(),
            value -> !value.isEmpty(),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2", "api1"), values.get(p1key.key()));
        assertEquals(asList("api3", "api4", "api5"), values.get(p2key.key()));
        assertTrue(values.get(p3key.key()).isEmpty());
    }

    @Test
    public void shouldFindAllKeysWithFilterFromEnvVar() throws TechnicalException {
        final Key p1key = API_LABELS_DICTIONARY;
        final Key p2key = PORTAL_ANALYTICS_ENABLED;
        final Key p3key = PORTAL_ANALYTICS_TRACKINGID;
        final Key p4key = PORTAL_APIKEY_HEADER;

        final Parameter parameter1 = new Parameter();
        parameter1.setKey(API_LABELS_DICTIONARY.key());
        parameter1.setValue("api1;api2;api1");

        final Parameter parameter2 = new Parameter();
        parameter2.setKey(PORTAL_ANALYTICS_ENABLED.key());
        parameter2.setValue("api3;api4;;api5");

        final Parameter parameter3 = new Parameter();
        parameter3.setKey(PORTAL_ANALYTICS_TRACKINGID.key());

        final Parameter parameter4 = new Parameter();
        parameter4.setKey(PORTAL_APIKEY_HEADER.key());

        List<Parameter> parametersFromRepository = new ArrayList<>();
        parametersFromRepository.add(parameter2);
        parametersFromRepository.add(parameter3);

        when(
            parameterRepository.findByKeys(
                Arrays.asList(PORTAL_ANALYTICS_ENABLED.key(), PORTAL_ANALYTICS_TRACKINGID.key()),
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(parametersFromRepository);
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api1,api12");
        when(environment.containsProperty(PORTAL_APIKEY_HEADER.key())).thenReturn(true);
        when(environment.getProperty(PORTAL_APIKEY_HEADER.key())).thenReturn("header");

        final Map<String, List<String>> values = parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            Arrays.asList(p1key, p2key, p3key, p4key),
            value -> value,
            value -> !value.isEmpty(),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api12"), values.get(p1key.key()));
        assertEquals(asList("api3", "api4", "api5"), values.get(p2key.key()));
        assertTrue(values.get(p3key.key()).isEmpty());
        assertEquals("header", values.get(p4key.key()).get(0));
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1", result.getValue());
        verify(parameterRepository).create(parameter);
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(parameter)
            )
        );
    }

    @Test
    public void shouldCreateList() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api1;api2",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1;api2", result.getValue());
        verify(parameterRepository).create(parameter);
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(parameter)
            )
        );
    }

    @Test
    public void shouldNotCreateOrUpdateIfEnvVar() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(API_LABELS_DICTIONARY.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.findById(API_LABELS_DICTIONARY.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api10");

        Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            API_LABELS_DICTIONARY,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api10", result.getValue());
        verify(parameterRepository, times(0)).create(any());
        verify(auditService, times(0)).createAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldCreateOrUpdateIfNotOverridable() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(any())).thenReturn(parameter);
        when(environment.containsProperty(PORTAL_TOP_APIS.key())).thenReturn(true);

        Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1", result.getValue());
        verify(parameterRepository, times(1)).create(any());
        verify(auditService, times(1)).createAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldNotCreateOrUpdateListIfEnvVar() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(API_LABELS_DICTIONARY.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.findById(API_LABELS_DICTIONARY.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api10,api11,api12");

        Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            API_LABELS_DICTIONARY,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api10;api11;api12", result.getValue());
        verify(parameterRepository, times(0)).create(any());
        verify(auditService, times(0)).createAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldCreateOrUpdateListIfNotOverridable() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.create(parameter)).thenReturn(parameter);
        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(environment.containsProperty(PORTAL_TOP_APIS.key())).thenReturn(true);

        Parameter result = parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1", result.getValue());
        verify(parameterRepository, times(1)).create(any());
        verify(auditService, times(1)).createAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setReferenceId("DEFAULT");
        newParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        newParameter.setValue("api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api2",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).update(newParameter);
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_UPDATED) &&
                    auditLogData.getOldValue().equals(parameter) &&
                    auditLogData.getNewValue().equals(newParameter)
            )
        );
    }

    @Test
    public void shouldCreateMultipleValue() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            Collections.singletonList("api1"),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).create(parameter);
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(parameter)
            )
        );
    }

    @Test
    public void shouldNotCreateMultipleValueWithExistingParameter() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());

        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setReferenceId("DEFAULT");
        newParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        newParameter.setValue("api1;api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            Collections.singletonList("api1"),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, never()).update(newParameter);
        verify(auditService, never()).createAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldUpdateMultipleValue() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setReferenceId("DEFAULT");
        newParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        newParameter.setValue("api1;api2;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            asList("api1", "api2", "api2"),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).update(newParameter);
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_UPDATED) &&
                    auditLogData.getOldValue().equals(parameter) &&
                    auditLogData.getNewValue().equals(newParameter)
            )
        );
    }

    @Test
    public void shouldFindAsBoolean() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_RATING_ENABLED.key());
        parameter.setValue("true");

        when(parameterRepository.findById(PORTAL_RATING_ENABLED.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            of(parameter)
        );

        assertTrue(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                PORTAL_RATING_ENABLED,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
    }

    @Test
    public void shouldFindAsBooleanFromEnvVar() throws TechnicalException {
        when(environment.containsProperty(PORTAL_RATING_ENABLED.key())).thenReturn(true);
        when(environment.getProperty(PORTAL_RATING_ENABLED.key())).thenReturn("true");

        assertTrue(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                PORTAL_RATING_ENABLED,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
        verify(parameterRepository, times(0)).findById(any(), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldFindAsBooleanDefaultValue() throws TechnicalException {
        when(parameterRepository.findById(PORTAL_USERCREATION_ENABLED.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            empty()
        );
        when(
            parameterRepository.findById(PORTAL_USERCREATION_ENABLED.key(), "DEFAULT_ORG", ParameterReferenceType.ORGANIZATION)
        ).thenReturn(empty());
        EnvironmentEntity defaultEnv = new EnvironmentEntity();
        defaultEnv.setOrganizationId("DEFAULT_ORG");
        when(environmentService.findById("DEFAULT")).thenReturn(defaultEnv);
        assertTrue(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                PORTAL_USERCREATION_ENABLED,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
    }

    @Test
    public void shouldFind() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(COMPANY_NAME.key());
        parameter.setValue("company name");

        when(parameterRepository.findById(COMPANY_NAME.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        assertEquals(
            parameter.getValue(),
            parameterService.find(
                GraviteeContext.getExecutionContext(),
                COMPANY_NAME,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
    }

    @Test
    public void shouldFindList() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(API_LABELS_DICTIONARY.key());
        parameter.setValue("label1;label2");

        when(parameterRepository.findById(API_LABELS_DICTIONARY.key(), "DEFAULT", ParameterReferenceType.ORGANIZATION)).thenReturn(
            of(parameter)
        );

        assertEquals(
            parameter.getValue(),
            parameterService.find(
                GraviteeContext.getExecutionContext(),
                API_LABELS_DICTIONARY,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ORGANIZATION
            )
        );
    }

    @Test
    public void shouldFindFromEnvVar() throws TechnicalException {
        final String companyName = "company name";

        when(environment.containsProperty(COMPANY_NAME.key())).thenReturn(true);
        when(environment.getProperty(COMPANY_NAME.key())).thenReturn(companyName);

        assertEquals(
            companyName,
            parameterService.find(
                GraviteeContext.getExecutionContext(),
                COMPANY_NAME,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
        verify(parameterRepository, times(0)).findById(COMPANY_NAME.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void shouldFindListFromEnvVar() throws TechnicalException {
        final String labels = "label1,label2";

        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn(labels);

        assertEquals(
            "label1;label2",
            parameterService.find(
                GraviteeContext.getExecutionContext(),
                API_LABELS_DICTIONARY,
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
        verify(parameterRepository, times(0)).findById(API_LABELS_DICTIONARY.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void should_invalidate_cache_by_key_and_reference() throws TechnicalException {
        // given
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        // First call to fill cache
        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // Invalidate cache directly
        parameterService.invalidateCache(PORTAL_TOP_APIS.key(), "DEFAULT", "ENVIRONMENT");

        // Call again, should call repository again since cache was invalidated
        parameterService.findAll(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, times(2)).findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    @Test
    public void shouldSendInvalidateCacheCommandOnCreate() throws TechnicalException {
        // given
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        // when
        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // then
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).create(commandCaptor.capture());

        Command command = commandCaptor.getValue();
        assertEquals(List.of(CommandTags.PARAMETER_CACHE_UPDATE.name()), command.getTags());
        assertTrue(command.getContent().contains(PORTAL_TOP_APIS.key()));
        assertTrue(command.getContent().contains("DEFAULT"));
        assertTrue(command.getContent().contains("ENVIRONMENT"));
    }

    @Test
    public void shouldSendInvalidateCacheCommandOnUpdate() throws TechnicalException {
        // given
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setReferenceId("DEFAULT");
        newParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        newParameter.setValue("api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        // when
        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            "api2",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // then
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).create(commandCaptor.capture());

        Command command = commandCaptor.getValue();
        assertEquals(List.of(CommandTags.PARAMETER_CACHE_UPDATE.name()), command.getTags());
        assertTrue(command.getContent().contains(PORTAL_TOP_APIS.key()));
    }

    @Test
    public void shouldAuditPortalNextToggleOnFirstCreate() throws TechnicalException {
        final Parameter savedParameter = new Parameter();
        savedParameter.setKey(PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_ENABLED.key());
        savedParameter.setReferenceId("DEFAULT");
        savedParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        savedParameter.setValue("true");

        when(
            parameterRepository.findById(PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_ENABLED.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)
        ).thenReturn(empty());
        when(parameterRepository.create(savedParameter)).thenReturn(savedParameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_ENABLED,
            "true",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_ENABLED.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(savedParameter)
            )
        );
    }

    @Test
    public void shouldAuditPortalNextToggleOnUpdate() throws TechnicalException {
        final Parameter oldParameter = new Parameter();
        oldParameter.setKey(PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_INVITATIONS_ENABLED.key());
        oldParameter.setReferenceId("DEFAULT");
        oldParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        oldParameter.setValue("false");

        final Parameter updatedParameter = new Parameter();
        updatedParameter.setKey(PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_INVITATIONS_ENABLED.key());
        updatedParameter.setReferenceId("DEFAULT");
        updatedParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        updatedParameter.setValue("true");

        when(
            parameterRepository.findById(
                PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_INVITATIONS_ENABLED.key(),
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(of(oldParameter));
        when(parameterRepository.update(updatedParameter)).thenReturn(updatedParameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_INVITATIONS_ENABLED,
            "true",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData
                        .getProperties()
                        .equals(singletonMap(PARAMETER, PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_INVITATIONS_ENABLED.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_UPDATED) &&
                    auditLogData.getOldValue().equals(oldParameter) &&
                    auditLogData.getNewValue().equals(updatedParameter)
            )
        );
    }

    @Test
    public void shouldNotAuditPortalNextToggleWhenValueUnchanged() throws TechnicalException {
        final Parameter existingParameter = new Parameter();
        existingParameter.setKey(PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_TRANSFER_OWNERSHIP_ENABLED.key());
        existingParameter.setReferenceId("DEFAULT");
        existingParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existingParameter.setValue("false");

        when(
            parameterRepository.findById(
                PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_TRANSFER_OWNERSHIP_ENABLED.key(),
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(of(existingParameter));

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_TRANSFER_OWNERSHIP_ENABLED,
            "false",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService, never()).createAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldSendInvalidateCacheCommandOnDelete() throws TechnicalException {
        // given
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        // when
        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            (String) null,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        // then
        verify(parameterRepository).delete(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);

        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).create(commandCaptor.capture());

        Command command = commandCaptor.getValue();
        assertEquals(List.of(CommandTags.PARAMETER_CACHE_UPDATE.name()), command.getTags());
        assertTrue(command.getContent().contains(PORTAL_TOP_APIS.key()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void shouldReportParameterExistenceOnScope(boolean present) throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(EMAIL_BRANDED_SENDERS.key());
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            present ? of(parameter) : empty()
        );

        boolean exists = parameterService.existsOnScope(
            EMAIL_BRANDED_SENDERS,
            "DEFAULT",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(present, exists);
    }

    @Test
    public void shouldWrapTechnicalExceptionWhenCheckingExistenceOnScope() throws TechnicalException {
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenThrow(
            new TechnicalException("boom")
        );

        assertThrows(TechnicalManagementException.class, () ->
            parameterService.existsOnScope(
                EMAIL_BRANDED_SENDERS,
                "DEFAULT",
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
    }

    @Test
    public void shouldDeleteParameterOnScopeAndInvalidateCacheWhenPresent() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(EMAIL_BRANDED_SENDERS.key());
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            of(parameter)
        );

        parameterService.delete(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            "DEFAULT",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).delete(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);

        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository).create(commandCaptor.capture());
        assertEquals(List.of(CommandTags.PARAMETER_CACHE_UPDATE.name()), commandCaptor.getValue().getTags());
    }

    @Test
    public void shouldNotDeleteParameterOnScopeWhenAbsent() throws TechnicalException {
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());

        parameterService.delete(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            "DEFAULT",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, never()).delete(anyString(), anyString(), any());
        verifyNoInteractions(commandRepository);
    }

    @Test
    public void shouldWrapTechnicalExceptionWhenDeletingOnScope() throws TechnicalException {
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenThrow(
            new TechnicalException("boom")
        );

        assertThrows(TechnicalManagementException.class, () ->
            parameterService.delete(
                GraviteeContext.getExecutionContext(),
                EMAIL_BRANDED_SENDERS,
                "DEFAULT",
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );
    }

    @Test
    public void shouldResolveNullReferenceIdFromContextWhenDeleting() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(EMAIL_BRANDED_SENDERS.key());
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            of(parameter)
        );

        // A null referenceId resolves to the execution context's environment id ("DEFAULT"), unlike existsOnScope.
        parameterService.delete(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            null,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).delete(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }

    /**
     * Matches the audit log expected for the deletion of {@code expected}.
     * <p>
     * Both {@code equals} and {@code getValue} are asserted on purpose: {@link Parameter#equals(Object)} compares only
     * key, referenceId and referenceType — {@code value} is deliberately excluded — so neither check alone would catch
     * a regression that recorded a partially populated parameter.
     */
    private static boolean isDeletionAuditOf(AuditService.AuditLogData auditLogData, Parameter expected) {
        // instanceof keeps the matcher null-safe: a null oldValue must fail the match, not throw inside argThat.
        return (
            auditLogData.getOldValue() instanceof Parameter oldValue &&
            auditLogData.getProperties().equals(singletonMap(PARAMETER, expected.getKey())) &&
            auditLogData.getEvent().equals(PARAMETER_DELETED) &&
            oldValue.equals(expected) &&
            Objects.equals(oldValue.getValue(), expected.getValue()) &&
            auditLogData.getNewValue() == null
        );
    }

    @ParameterizedTest
    @EnumSource(value = io.gravitee.rest.api.model.parameters.ParameterReferenceType.class, names = { "ENVIRONMENT", "ORGANIZATION" })
    public void should_emit_audit_log_when_deleting_parameter_on_scope(
        io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) throws TechnicalException {
        final ParameterReferenceType repositoryReferenceType = ParameterReferenceType.valueOf(referenceType.name());
        final String previousValue = """
            [{"domains":["airtel.com"],"from":"noreply@airtel.com","subject":"[Airtel] %s"}]""";
        final Parameter existing = new Parameter();
        existing.setKey(EMAIL_BRANDED_SENDERS.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(repositoryReferenceType);
        existing.setValue(previousValue);

        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", repositoryReferenceType)).thenReturn(of(existing));

        parameterService.delete(GraviteeContext.getExecutionContext(), EMAIL_BRANDED_SENDERS, "DEFAULT", referenceType);

        verify(parameterRepository).delete(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", repositoryReferenceType);

        // Note: the audit entry's own reference scope is derived by AuditServiceImpl from the ExecutionContext, not
        // from the parameter's ParameterReferenceType — same as the create and update paths.
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> isDeletionAuditOf(auditLogData, existing))
        );
    }

    @Test
    public void should_not_emit_audit_log_when_deleting_absent_parameter_on_scope() throws TechnicalException {
        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());

        parameterService.delete(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            "DEFAULT",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verifyNoInteractions(auditService);
    }

    @Test
    public void should_not_emit_audit_log_when_the_parameter_deletion_fails() throws TechnicalException {
        final Parameter existing = new Parameter();
        existing.setKey(EMAIL_BRANDED_SENDERS.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existing.setValue("[]");

        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            of(existing)
        );
        doThrow(new TechnicalException("boom"))
            .when(parameterRepository)
            .delete(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);

        assertThrows(TechnicalManagementException.class, () ->
            parameterService.delete(
                GraviteeContext.getExecutionContext(),
                EMAIL_BRANDED_SENDERS,
                "DEFAULT",
                io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
            )
        );

        // The audit log must never record a deletion that did not happen: it is emitted only after the
        // repository delete succeeds.
        verifyNoInteractions(auditService);
    }

    @Test
    public void should_emit_audit_log_when_saving_a_null_value_deletes_the_parameter() throws TechnicalException {
        final Parameter existing = new Parameter();
        existing.setKey(PORTAL_TOP_APIS.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existing.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(existing));

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            (String) null,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> isDeletionAuditOf(auditLogData, existing))
        );
    }

    @Test
    public void should_emit_audit_log_when_an_empty_list_collapses_to_a_deletion() throws TechnicalException {
        final Parameter existing = new Parameter();
        existing.setKey(PORTAL_TOP_APIS.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existing.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(existing));

        // An empty list collapses to a null value, which flips the update branch to the delete branch — this is how
        // TopApiServiceImpl.delete() removes the last remaining top API.
        parameterService.save(
            GraviteeContext.getExecutionContext(),
            PORTAL_TOP_APIS,
            List.of(),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).delete(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> isDeletionAuditOf(auditLogData, existing))
        );
    }

    @Test
    public void should_not_delete_or_audit_when_a_null_value_is_saved_for_a_key_overridden_by_configuration() throws TechnicalException {
        final Parameter existing = new Parameter();
        existing.setKey(EMAIL_FROM.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existing.setValue("stored@example.com");

        when(parameterRepository.findById(EMAIL_FROM.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(existing));
        when(environment.containsProperty(EMAIL_FROM.key())).thenReturn(true);
        when(environment.getProperty(EMAIL_FROM.key())).thenReturn("yaml@example.com");

        // The system-override guard runs before the delete branch, so a null value must not wipe the stored fallback
        // behind a configuration-locked key.
        parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_FROM,
            (String) null,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, never()).delete(anyString(), anyString(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    public void should_not_delete_or_audit_when_a_null_value_is_saved_for_branded_senders_configured_in_yaml() throws TechnicalException {
        final Parameter existing = new Parameter();
        existing.setKey(EMAIL_BRANDED_SENDERS.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existing.setValue("[]");

        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            of(existing)
        );
        // containsProperty is false for a native yaml list, so the reader is the only guard for this key.
        when(brandedSendersEnvironmentReader.read()).thenReturn(
            Optional.of(
                """
                [{"domains":["airtel.com"],"from":"noreply@airtel.com","subject":"[Airtel] %s"}]"""
            )
        );

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            (String) null,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, never()).delete(anyString(), anyString(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    public void should_emit_audit_log_when_creating_email_branded_senders() throws TechnicalException {
        final String value = """
            [{"domains":["airtel.com"],"from":"noreply@airtel.com","subject":"[Airtel] %s"}]""";
        final Parameter parameter = new Parameter();
        parameter.setKey(EMAIL_BRANDED_SENDERS.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue(value);

        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            value,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, EMAIL_BRANDED_SENDERS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(parameter)
            )
        );
    }

    @Test
    public void should_emit_audit_log_with_full_json_before_and_after_when_updating_email_branded_senders() throws TechnicalException {
        final String previousValue = """
            [{"domains":["airtel.com"],"from":"noreply@airtel.com","subject":"[Airtel] %s"}]""";
        final String newValue = """
            [{"domains":["airtel.com"],"from":"noreply@airtel.com","subject":"[Airtel] %s"},\
            {"domains":["vodafone.com"],"from":"noreply@vodafone.com","subject":"[Vodafone] %s"}]""";

        final Parameter existing = new Parameter();
        existing.setKey(EMAIL_BRANDED_SENDERS.key());
        existing.setReferenceId("DEFAULT");
        existing.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        existing.setValue(previousValue);

        final Parameter updated = new Parameter();
        updated.setKey(EMAIL_BRANDED_SENDERS.key());
        updated.setReferenceId("DEFAULT");
        updated.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        updated.setValue(newValue);

        when(parameterRepository.findById(EMAIL_BRANDED_SENDERS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(
            of(existing)
        );
        when(parameterRepository.update(updated)).thenReturn(updated);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_BRANDED_SENDERS,
            newValue,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, EMAIL_BRANDED_SENDERS.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_UPDATED) &&
                    ((Parameter) auditLogData.getOldValue()).getValue().equals(previousValue) &&
                    ((Parameter) auditLogData.getNewValue()).getValue().equals(newValue)
            )
        );
    }

    @Test
    public void should_emit_audit_log_when_saving_email_from_at_organization_scope() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(EMAIL_FROM.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ORGANIZATION);
        parameter.setValue("noreply@example.com");

        when(parameterRepository.findById(EMAIL_FROM.key(), "DEFAULT", ParameterReferenceType.ORGANIZATION)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_FROM,
            "noreply@example.com",
            "DEFAULT",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ORGANIZATION
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, EMAIL_FROM.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getNewValue().equals(parameter)
            )
        );
    }

    @Test
    public void should_emit_audit_log_when_saving_email_subject() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(EMAIL_SUBJECT.key());
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameter.setValue("[Example] %s");

        when(parameterRepository.findById(EMAIL_SUBJECT.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.save(
            GraviteeContext.getExecutionContext(),
            EMAIL_SUBJECT,
            "[Example] %s",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getProperties().equals(singletonMap(PARAMETER, EMAIL_SUBJECT.key())) &&
                    auditLogData.getEvent().equals(PARAMETER_CREATED) &&
                    auditLogData.getNewValue().equals(parameter)
            )
        );
    }
}
