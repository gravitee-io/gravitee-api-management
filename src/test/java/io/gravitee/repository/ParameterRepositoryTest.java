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
package io.gravitee.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;

public class ParameterRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/parameter-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Parameter parameter = new Parameter();
        parameter.setKey("new-parameter");
        parameter.setValue("Parameter value");
        parameter.setReferenceId("DEFAULT");
        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);

        assertFalse("Parameter already exists", parameterRepository.findById("new-parameter").isPresent());
        parameterRepository.create(parameter);
        assertTrue("Parameter not created", parameterRepository.findById("new-parameter").isPresent());

        Optional<Parameter> optional = parameterRepository.findById("new-parameter");
        assertTrue("Parameter saved not found", optional.isPresent());

        final Parameter parameterSaved = optional.get();
        assertEquals("Invalid saved parameter value.", parameter.getValue(), parameterSaved.getValue());
        assertEquals("Invalid saved parameter referenceId.", parameter.getReferenceId(), parameterSaved.getReferenceId());
        assertEquals("Invalid saved parameter referenceType.", parameter.getReferenceType(), parameterSaved.getReferenceType());
        
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Parameter> optional = parameterRepository.findById("portal.top-apis");
        assertTrue("Parameter to update not found", optional.isPresent());
        assertEquals("Invalid saved parameter value.", "api1;api2;api2", optional.get().getValue());

        final Parameter parameter = optional.get();
        parameter.setValue("New value");

        assertTrue("Parameter does not exist", parameterRepository.findById("portal.top-apis").isPresent());
        parameterRepository.update(parameter);

        Optional<Parameter> optionalUpdated = parameterRepository.findById("portal.top-apis");
        assertTrue("Parameter to update not found", optionalUpdated.isPresent());

        final Parameter parameterUpdated = optionalUpdated.get();
        assertEquals("Invalid saved parameter name.", "New value", parameterUpdated.getValue());
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue("Parameter to delete does not exist", parameterRepository.findById("management.oAuth.clientId").isPresent());
        parameterRepository.delete("management.oAuth.clientId");
        assertFalse("Parameter not deleted", parameterRepository.findById("management.oAuth.clientId").isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownParameter() throws Exception {
        Parameter unknownParameter = new Parameter();
        unknownParameter.setKey("unknown");
        unknownParameter.setReferenceId("unknown");
        unknownParameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
        parameterRepository.update(unknownParameter);
        fail("An unknown parameter should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        parameterRepository.update(null);
        fail("A null parameter should not be updated");
    }

    @Test
    public void shouldFindAll() throws Exception {
        List<Parameter> parameters = parameterRepository.findAll(Arrays.asList("management.oAuth.clientId", "management.oAuth.clientSecret", "unknown"));
        assertNotNull(parameters);
        assertFalse(parameters.isEmpty());
        assertEquals(2, parameters.size());
    }
    
    @Test
    public void shouldFindAllByReferenceIdAndReferenceType() throws Exception {
        List<Parameter> parameters = parameterRepository.findAllByReferenceIdAndReferenceType(null, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        assertNotNull(parameters);
        assertFalse(parameters.isEmpty());
        assertEquals(3, parameters.size());
    }
}
