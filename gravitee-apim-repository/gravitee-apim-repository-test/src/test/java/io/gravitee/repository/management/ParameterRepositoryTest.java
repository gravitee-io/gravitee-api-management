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

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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

        assertFalse(parameterRepository.findById("new-parameter", REFERENCE_ID, REFERENCE_TYPE).isPresent(), "Parameter already exists");
        parameterRepository.create(parameter);
        assertTrue(parameterRepository.findById("new-parameter", REFERENCE_ID, REFERENCE_TYPE).isPresent(), "Parameter not created");

        Optional<Parameter> optional = parameterRepository.findById("new-parameter", REFERENCE_ID, REFERENCE_TYPE);
        assertTrue(optional.isPresent(), "Parameter saved not found");

        final Parameter parameterSaved = optional.get();
        assertEquals(parameter.getValue(), parameterSaved.getValue(), "Invalid saved parameter value.");
        assertEquals(parameter.getReferenceId(), parameterSaved.getReferenceId(), "Invalid saved parameter referenceId.");
        assertEquals(parameter.getReferenceType(), parameterSaved.getReferenceType(), "Invalid saved parameter referenceType.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Parameter> optional = parameterRepository.findById("portal.top-apis", REFERENCE_ID, REFERENCE_TYPE);
        assertTrue(optional.isPresent(), "Parameter to update not found");
        assertEquals("api1;api2;api2", optional.get().getValue(), "Invalid saved parameter value.");

        final Parameter parameter = optional.get();
        parameter.setValue("New value");

        assertTrue(parameterRepository.findById("portal.top-apis", REFERENCE_ID, REFERENCE_TYPE).isPresent(), "Parameter does not exist");
        parameterRepository.update(parameter);

        Optional<Parameter> optionalUpdated = parameterRepository.findById("portal.top-apis", REFERENCE_ID, REFERENCE_TYPE);
        assertTrue(optionalUpdated.isPresent(), "Parameter to update not found");

        final Parameter parameterUpdated = optionalUpdated.get();
        assertEquals("New value", parameterUpdated.getValue(), "Invalid saved parameter name.");
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(
            parameterRepository.findById("management.oAuth.clientId", REFERENCE_ID, REFERENCE_TYPE).isPresent(),
            "Parameter to delete does not exist"
        );
        parameterRepository.delete("management.oAuth.clientId", REFERENCE_ID, REFERENCE_TYPE);
        assertFalse(
            parameterRepository.findById("management.oAuth.clientId", REFERENCE_ID, REFERENCE_TYPE).isPresent(),
            "Parameter not deleted"
        );
    }

    @Test
    public void shouldNotUpdateUnknownParameter() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Parameter unknownParameter = new Parameter();
            unknownParameter.setKey("unknown");
            unknownParameter.setReferenceId("unknown");
            unknownParameter.setReferenceType(REFERENCE_TYPE);
            parameterRepository.update(unknownParameter);
            fail("An unknown parameter should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            parameterRepository.update(null);
            fail("A null parameter should not be updated");
        });
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
