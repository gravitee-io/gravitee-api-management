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

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyServiceSerializerTest extends AbstractTest {

    @Test
    public void definition_withDynamicProperty() throws Exception {
        String oldDefinition = "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty.json";
        String expectedDefinition =
            "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-expected.json";
        Api api = load(oldDefinition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assertions.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withDynamicProperty_v2() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-v2.json";
        String expectedDefinition =
            "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assertions.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withDynamicProperty_v3() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-v3.json";
        String expectedDefinition =
            "/io/gravitee/definition/jackson/services/dynamicproperty/api-withservice-dynamicproperty-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assertions.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }
}
