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
package io.gravitee.management.service;

import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.service.impl.ParameterServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.gravitee.management.model.parameters.Key.*;
import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    public void shouldFindAll() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(of(parameter));

        final List<String> values = parameterService.findAll(PORTAL_TOP_APIS, value -> value);

        assertEquals(asList("api1", "api2"), values);
    }

    @Test
    public void shouldFindAllWithFilter() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1;api2;;api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(of(parameter));

        final List<String> values = parameterService.findAll(PORTAL_TOP_APIS, value -> value, value -> !value.isEmpty());

        assertEquals(asList("api1", "api2", "api1"), values);
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

        when(parameterRepository.findAll(Arrays.asList(PORTAL_TOP_APIS.key(), PORTAL_ANALYTICS_ENABLED.key(), PORTAL_ANALYTICS_TRACKINGID.key())))
                .thenReturn(Arrays.asList(parameter1, parameter2, parameter3));

        final Map<String, List<String>> values = parameterService.findAll(Arrays.asList(p1key, p2key, p3key), value -> value, value -> !value.isEmpty());

        assertEquals(asList("api1", "api2", "api1"), values.get(p1key.key()));
        assertEquals(asList("api3", "api4", "api5"), values.get(p2key.key()));
        assertTrue(values.get(p3key.key()).isEmpty());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.save(PORTAL_TOP_APIS, "api1");

        verify(parameterRepository).create(parameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())), eq(PARAMETER_CREATED),
                any(), eq(null), eq(parameter));
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setValue("api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.save(PORTAL_TOP_APIS, "api2");

        verify(parameterRepository).update(newParameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())), eq(PARAMETER_UPDATED),
                any(), eq(parameter), eq(newParameter));
    }

    @Test
    public void shouldCreateMultipleValue() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.save(PORTAL_TOP_APIS, Collections.singletonList("api1"));

        verify(parameterRepository).create(parameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())), eq(PARAMETER_CREATED),
                any(), eq(null), eq(parameter));
    }

    @Test
    public void shouldCreateMultipleValueWithExistingParameter() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setValue("api1;api1");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.save(PORTAL_TOP_APIS, Collections.singletonList("api1"));

        verify(parameterRepository).update(newParameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())), eq(PARAMETER_UPDATED),
                any(), eq(parameter), eq(newParameter));
    }

    @Test
    public void shouldUpdateMultipleValue() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_TOP_APIS.key());
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PORTAL_TOP_APIS.key());
        newParameter.setValue("api1;api2;api2");

        when(parameterRepository.findById(PORTAL_TOP_APIS.key())).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.save(PORTAL_TOP_APIS, asList("api1", "api2", "api2"));

        verify(parameterRepository).update(newParameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PORTAL_TOP_APIS.key())), eq(PARAMETER_UPDATED),
                any(), eq(parameter), eq(newParameter));
    }

    @Test
    public void shouldFindAsBoolean() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PORTAL_RATING_ENABLED.key());
        parameter.setValue("true");

        when(parameterRepository.findById(PORTAL_RATING_ENABLED.key())).thenReturn(of(parameter));

        assertTrue(parameterService.findAsBoolean(PORTAL_RATING_ENABLED));
    }

    @Test
    public void shouldFindAsBooleanDefaultValue() throws TechnicalException {
        when(parameterRepository.findById(PORTAL_USERCREATION_ENABLED.key())).thenReturn(empty());
        assertTrue(parameterService.findAsBoolean(PORTAL_USERCREATION_ENABLED));
    }
}