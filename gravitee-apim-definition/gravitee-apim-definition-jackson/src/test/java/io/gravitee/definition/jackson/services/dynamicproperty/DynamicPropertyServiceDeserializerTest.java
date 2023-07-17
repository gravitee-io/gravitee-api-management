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
package io.gravitee.definition.jackson.services.dynamicproperty;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyServiceDeserializerTest extends AbstractTest {

    @Test
    public void definition_withDynamicProperty() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty.json", Api.class);
        DynamicPropertyService dynamicPropertyService = api.getService(DynamicPropertyService.class);

        assertNotNull(dynamicPropertyService);

        // Check service configuration
        assertTrue(dynamicPropertyService.isEnabled());

        // Check scheduling configuration
        assertEquals("*/60 * * * * *", dynamicPropertyService.getSchedule());

        // Check provider
        assertNotNull(dynamicPropertyService.getProvider());

        // Check configuration
        DynamicPropertyProviderConfiguration configuration = dynamicPropertyService.getConfiguration();
        assertNotNull(configuration);

        assertEquals("http://my_configuration_url", ((HttpDynamicPropertyProviderConfiguration) configuration).getUrl());
        assertEquals("{}", ((HttpDynamicPropertyProviderConfiguration) configuration).getSpecification());
        assertEquals(HttpMethod.POST, ((HttpDynamicPropertyProviderConfiguration) configuration).getMethod());
        assertEquals(1, ((HttpDynamicPropertyProviderConfiguration) configuration).getHeaders().size());
        assertTrue(
            ((HttpDynamicPropertyProviderConfiguration) configuration).getHeaders()
                .stream()
                .allMatch(header -> header.getName().equals("Content-type"))
        );
    }

    @Test
    public void definition_withDynamicProperty_badUnit() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () -> load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-badUnit.json", Api.class)
        );
    }

    @Test
    public void definition_withDynamicProperty_noProvider() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () -> load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-badUnit.json", Api.class)
        );
    }

    @Test
    public void definition_withDynamicProperty_unitInLowerCase() throws Exception {
        Api api = load(
            "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-unitInLowerCase.json",
            Api.class
        );
        DynamicPropertyService dynamicPropertyService = api.getService(DynamicPropertyService.class);
        assertNotNull(dynamicPropertyService);
        assertTrue(dynamicPropertyService.isEnabled());
        assertEquals("*/60 * * * * *", dynamicPropertyService.getSchedule());
    }

    @Test
    public void definition_withDynamicProperty_httpProvider_noUrl() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () -> load("/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-noUrl.json", Api.class)
        );
    }

    @Test
    public void definition_withDynamicProperty_httpProvider_noSpecification() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () ->
                load(
                    "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-noSpecification.json",
                    Api.class
                )
        );
    }
}
