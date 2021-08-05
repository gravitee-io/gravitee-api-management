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
package io.gravitee.rest.api.service;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static io.gravitee.rest.api.model.parameters.Key.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.ParameterServiceImpl;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
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
    private EventManager eventManager;

    @Mock
    private EnvironmentService environmentService;

    @Before
    public void init() {
        GraviteeContext.getCurrentParameters().clear();
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT)).thenReturn(of(parameter));

        final List<String> values = parameterService.findAll(
            PORTAL_TOP_APIS,
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals(asList("api1", "api2"), values);
    }

    @Test
    public void shouldFindAllFromEnvVar() throws TechnicalException {
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api1,api2");

        final List<String> values = parameterService.findAll(
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
        when(parameterRepository.findById(PORTAL_TOP_APIS.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Optional.of(parameter));

        final List<String> values = parameterService.findAll(
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
        )
            .thenReturn(Arrays.asList(parameter1, parameter2, parameter3));

        final Map<String, List<String>> values = parameterService.findAll(
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
        )
            .thenReturn(Arrays.asList(parameter1, parameter2, parameter3));

        final Map<String, List<String>> values = parameterService.findAll(
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
        )
            .thenReturn(parametersFromRepository);
        when(environment.containsProperty(API_LABELS_DICTIONARY.key())).thenReturn(true);
        when(environment.getProperty(API_LABELS_DICTIONARY.key())).thenReturn("api1,api12");
        when(environment.containsProperty(PORTAL_APIKEY_HEADER.key())).thenReturn(true);
        when(environment.getProperty(PORTAL_APIKEY_HEADER.key())).thenReturn("header");

        final Map<String, List<String>> values = parameterService.findAll(
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
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1", result.getValue());
        verify(parameterRepository).create(parameter);
        verify(auditService)
            .createEnvironmentAuditLog(
                eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())),
                eq(PARAMETER_CREATED),
                any(),
                eq(null),
                eq(parameter)
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
            PORTAL_TOP_APIS,
            "api1;api2",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1;api2", result.getValue());
        verify(parameterRepository).create(parameter);
        verify(auditService)
            .createEnvironmentAuditLog(
                eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())),
                eq(PARAMETER_CREATED),
                any(),
                eq(null),
                eq(parameter)
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
            API_LABELS_DICTIONARY,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api10", result.getValue());
        verify(parameterRepository, times(0)).create(any());
        verify(auditService, times(0)).createEnvironmentAuditLog(any(), any(), any(), any(), any());
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
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1", result.getValue());
        verify(parameterRepository, times(1)).create(any());
        verify(auditService, times(1)).createEnvironmentAuditLog(any(), any(), any(), any(), any());
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
            API_LABELS_DICTIONARY,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api10;api11;api12", result.getValue());
        verify(parameterRepository, times(0)).create(any());
        verify(auditService, times(0)).createEnvironmentAuditLog(any(), any(), any(), any(), any());
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
            PORTAL_TOP_APIS,
            "api1",
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        assertEquals("api1", result.getValue());
        verify(parameterRepository, times(1)).create(any());
        verify(auditService, times(1)).createEnvironmentAuditLog(any(), any(), any(), any(), any());
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

        parameterService.save(PORTAL_TOP_APIS, "api2", io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT);

        verify(parameterRepository).update(newParameter);
        verify(auditService)
            .createEnvironmentAuditLog(
                eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())),
                eq(PARAMETER_UPDATED),
                any(),
                eq(parameter),
                eq(newParameter)
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
            PORTAL_TOP_APIS,
            Collections.singletonList("api1"),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).create(parameter);
        verify(auditService)
            .createEnvironmentAuditLog(
                eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())),
                eq(PARAMETER_CREATED),
                any(),
                eq(null),
                eq(parameter)
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
            PORTAL_TOP_APIS,
            Collections.singletonList("api1"),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository, never()).update(newParameter);
        verify(auditService, never()).createEnvironmentAuditLog(any(), any(), any(), any(), any());
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
            PORTAL_TOP_APIS,
            asList("api1", "api2", "api2"),
            io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
        );

        verify(parameterRepository).update(newParameter);
        verify(auditService)
            .createEnvironmentAuditLog(
                eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())),
                eq(PARAMETER_UPDATED),
                any(),
                eq(parameter),
                eq(newParameter)
            );
    }

    @Test
    public void shouldFindAsBoolean() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_RATING_ENABLED.key());
        parameter.setValue("true");

        when(parameterRepository.findById(PORTAL_RATING_ENABLED.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(of(parameter));

        assertTrue(
            parameterService.findAsBoolean(PORTAL_RATING_ENABLED, io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT)
        );
    }

    @Test
    public void shouldFindAsBooleanFromEnvVar() throws TechnicalException {
        when(environment.containsProperty(PORTAL_RATING_ENABLED.key())).thenReturn(true);
        when(environment.getProperty(PORTAL_RATING_ENABLED.key())).thenReturn("true");

        assertTrue(
            parameterService.findAsBoolean(PORTAL_RATING_ENABLED, io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT)
        );
        verify(parameterRepository, times(0)).findById(any(), any(), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldFindAsBooleanDefaultValue() throws TechnicalException {
        when(parameterRepository.findById(PORTAL_USERCREATION_ENABLED.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(empty());
        when(parameterRepository.findById(PORTAL_USERCREATION_ENABLED.key(), "DEFAULT_ORG", ParameterReferenceType.ORGANIZATION))
            .thenReturn(empty());
        EnvironmentEntity defaultEnv = new EnvironmentEntity();
        defaultEnv.setOrganizationId("DEFAULT_ORG");
        when(environmentService.findById("DEFAULT")).thenReturn(defaultEnv);
        assertTrue(
            parameterService.findAsBoolean(
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
            parameterService.find(COMPANY_NAME, io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT)
        );
    }

    @Test
    public void shouldFindList() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(API_LABELS_DICTIONARY.key());
        parameter.setValue("label1;label2");

        when(parameterRepository.findById(API_LABELS_DICTIONARY.key(), "DEFAULT", ParameterReferenceType.ORGANIZATION))
            .thenReturn(of(parameter));

        assertEquals(
            parameter.getValue(),
            parameterService.find(API_LABELS_DICTIONARY, io.gravitee.rest.api.model.parameters.ParameterReferenceType.ORGANIZATION)
        );
    }

    @Test
    public void shouldFindFromEnvVar() throws TechnicalException {
        final String companyName = "company name";

        when(environment.containsProperty(COMPANY_NAME.key())).thenReturn(true);
        when(environment.getProperty(COMPANY_NAME.key())).thenReturn(companyName);

        assertEquals(
            companyName,
            parameterService.find(COMPANY_NAME, io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT)
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
            parameterService.find(API_LABELS_DICTIONARY, io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT)
        );
        verify(parameterRepository, times(0)).findById(API_LABELS_DICTIONARY.key(), "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }
}
