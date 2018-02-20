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

import io.gravitee.management.service.exceptions.ParameterAlreadyExistsException;
import io.gravitee.management.service.exceptions.ParameterNotFoundException;
import io.gravitee.management.service.impl.ParameterServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

    private static final String PARAMETER_KEY = "parameter.key";

    @Mock
    private ParameterRepository parameterRepository;
    @Mock
    private AuditService auditService;

    @Test
    public void shouldFindAll() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1;api2");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(of(parameter));

        final List<String> values = parameterService.findAll(PARAMETER_KEY, value -> value);

        assertEquals(asList("api1", "api2"), values);
    }

    @Test
    public void shouldFindAllWithFilter() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1;api2;;api1");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(of(parameter));

        final List<String> values = parameterService.findAll(PARAMETER_KEY, value -> value, value -> !value.isEmpty());

        assertEquals(asList("api1", "api2", "api1"), values);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.create(PARAMETER_KEY, "api1");

        verify(parameterRepository).create(parameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PARAMETER_KEY)), eq(PARAMETER_CREATED),
                any(), eq(null), eq(parameter));
    }

    @Test(expected = ParameterAlreadyExistsException.class)
    public void shouldNotCreate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(of(parameter));

        parameterService.create(PARAMETER_KEY, "api1");
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PARAMETER_KEY);
        newParameter.setValue("api2");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.update(PARAMETER_KEY, "api2");

        verify(parameterRepository).update(newParameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PARAMETER_KEY)), eq(PARAMETER_UPDATED),
                any(), eq(parameter), eq(newParameter));
    }

    @Test(expected = ParameterNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(empty());

        parameterService.update(PARAMETER_KEY, "api2");
    }

    @Test
    public void shouldCreateMultipleValue() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(empty());
        when(parameterRepository.create(parameter)).thenReturn(parameter);

        parameterService.createMultipleValue(PARAMETER_KEY, "api1");

        verify(parameterRepository).create(parameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PARAMETER_KEY)), eq(PARAMETER_CREATED),
                any(), eq(null), eq(parameter));
    }

    @Test
    public void shouldCreateMultipleValueWithExistingParameter() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PARAMETER_KEY);
        newParameter.setValue("api1;api1");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.createMultipleValue(PARAMETER_KEY, "api1");

        verify(parameterRepository).update(newParameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PARAMETER_KEY)), eq(PARAMETER_UPDATED),
                any(), eq(parameter), eq(newParameter));
    }

    @Test
    public void shouldUpdateMultipleValue() throws TechnicalException {
        final Parameter parameter = new Parameter();
        parameter.setKey(PARAMETER_KEY);
        parameter.setValue("api1");

        final Parameter newParameter = new Parameter();
        newParameter.setKey(PARAMETER_KEY);
        newParameter.setValue("api1;api2;api2");

        when(parameterRepository.findById(PARAMETER_KEY)).thenReturn(of(parameter));
        when(parameterRepository.update(newParameter)).thenReturn(newParameter);

        parameterService.updateMultipleValue(PARAMETER_KEY, asList("api1", "api2", "api2"));

        verify(parameterRepository).update(newParameter);
        verify(auditService).createPortalAuditLog(eq(singletonMap(PARAMETER, PARAMETER_KEY)), eq(PARAMETER_UPDATED),
                any(), eq(parameter), eq(newParameter));
    }
}