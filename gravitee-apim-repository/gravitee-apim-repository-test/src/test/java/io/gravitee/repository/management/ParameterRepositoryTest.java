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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class ParameterRepositoryTest extends AbstractManagementRepositoryTest {

    private static final String REFERENCE_ID = "DEFAULT";
    private static final ParameterReferenceType REFERENCE_TYPE = ParameterReferenceType.ENVIRONMENT;

    @Override
    protected String getTestCasesPath() {
        return "/data/parameter-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Parameter parameter = new Parameter();
        parameter.setKey("new-parameter");
        parameter.setValue("Parameter value");
        parameter.setReferenceId(REFERENCE_ID);
        parameter.setReferenceType(REFERENCE_TYPE);

        assertFalse("Parameter already exists", parameterRepository.findById("new-parameter", REFERENCE_ID, REFERENCE_TYPE).isPresent());
        parameterRepository.create(parameter);
        assertTrue("Parameter not created", parameterRepository.findById("new-parameter", REFERENCE_ID, REFERENCE_TYPE).isPresent());

        Optional<Parameter> optional = parameterRepository.findById("new-parameter", REFERENCE_ID, REFERENCE_TYPE);
        assertTrue("Parameter saved not found", optional.isPresent());

        final Parameter parameterSaved = optional.get();
        assertEquals("Invalid saved parameter value.", parameter.getValue(), parameterSaved.getValue());
        assertEquals("Invalid saved parameter referenceId.", parameter.getReferenceId(), parameterSaved.getReferenceId());
        assertEquals("Invalid saved parameter referenceType.", parameter.getReferenceType(), parameterSaved.getReferenceType());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Parameter> optional = parameterRepository.findById("portal.top-apis", REFERENCE_ID, REFERENCE_TYPE);
        assertTrue("Parameter to update not found", optional.isPresent());
        assertEquals("Invalid saved parameter value.", "api1;api2;api2", optional.get().getValue());

        final Parameter parameter = optional.get();
        parameter.setValue("New value");

        assertTrue("Parameter does not exist", parameterRepository.findById("portal.top-apis", REFERENCE_ID, REFERENCE_TYPE).isPresent());
        parameterRepository.update(parameter);

        Optional<Parameter> optionalUpdated = parameterRepository.findById("portal.top-apis", REFERENCE_ID, REFERENCE_TYPE);
        assertTrue("Parameter to update not found", optionalUpdated.isPresent());

        final Parameter parameterUpdated = optionalUpdated.get();
        assertEquals("Invalid saved parameter name.", "New value", parameterUpdated.getValue());
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(
            "Parameter to delete does not exist",
            parameterRepository.findById("management.oAuth.clientId", REFERENCE_ID, REFERENCE_TYPE).isPresent()
        );
        parameterRepository.delete("management.oAuth.clientId", REFERENCE_ID, REFERENCE_TYPE);
        assertFalse(
            "Parameter not deleted",
            parameterRepository.findById("management.oAuth.clientId", REFERENCE_ID, REFERENCE_TYPE).isPresent()
        );
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownParameter() throws Exception {
        Parameter unknownParameter = new Parameter();
        unknownParameter.setKey("unknown");
        unknownParameter.setReferenceId("unknown");
        unknownParameter.setReferenceType(REFERENCE_TYPE);
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
        List<Parameter> parameters = parameterRepository.findByKeys(
            Arrays.asList("management.oAuth.clientId", "management.oAuth.clientSecret", "unknown"),
            REFERENCE_ID,
            REFERENCE_TYPE
        );
        assertNotNull(parameters);
        assertFalse(parameters.isEmpty());
        assertEquals(2, parameters.size());
    }

    @Test
    public void shouldFindAllByReferenceIdAndReferenceType() throws Exception {
        List<Parameter> parameters = parameterRepository.findAll(REFERENCE_ID, REFERENCE_TYPE);
        assertNotNull(parameters);
        assertFalse(parameters.isEmpty());
        assertEquals(3, parameters.size());
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        assertEquals(2, parameterRepository.findAll("env-deleted", ParameterReferenceType.ENVIRONMENT).size());

        List<String> deleted = parameterRepository.deleteByReferenceIdAndReferenceType("env-deleted", ParameterReferenceType.ENVIRONMENT);

        assertEquals(2, deleted.size());
        assertEquals(0, parameterRepository.findAll("env-deleted", ParameterReferenceType.ENVIRONMENT).size());
    }
}
