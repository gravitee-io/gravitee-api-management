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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypesEntity;
import io.gravitee.rest.api.service.impl.configuration.application.ApplicationTypeServiceImpl;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationTypeServiceTest {

    ObjectMapper objectMapper = new ObjectMapper();

    private ApplicationTypeServiceImpl applicationTypeService = new ApplicationTypeServiceImpl();

    @Test
    public void shouldGetAllApplicationTypes() throws TechnicalException, IOException {
        JsonNode jsonTypes = objectMapper.readTree(
            "{ \"simple\": { \"enabled\": true }, \"web\": { \"enabled\": true }, \"browser\": { \"enabled\": true }, \"backend_to_backend\": { \"enabled\": true }, \"native\": { \"enabled\": true } }"
        );
        ApplicationTypesEntity enabledApplicationsTypes = applicationTypeService.getFilteredApplicationTypes(jsonTypes);
        assertNotNull(enabledApplicationsTypes);
        assertEquals(5, enabledApplicationsTypes.getData().size());
    }

    @Test
    public void shouldGetEnabledApplicationTypes() throws TechnicalException, IOException {
        JsonNode jsonTypes = objectMapper.readTree(
            "{ \"simple\": { \"enabled\": false }, \"web\": { \"enabled\": true }, \"browser\": { \"enabled\": true }, \"backend_to_backend\": { \"enabled\": false }, \"native\": { \"enabled\": true } }"
        );
        ApplicationTypesEntity enabledApplicationsTypes = applicationTypeService.getFilteredApplicationTypes(jsonTypes);
        assertNotNull(enabledApplicationsTypes);
        assertEquals(3, enabledApplicationsTypes.getData().size());
    }
}
