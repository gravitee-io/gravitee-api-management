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
package io.gravitee.definition.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URL;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefinitionTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaulthttpconfig.json");

        Assert.assertEquals(URI.create("http://localhost:1234"), api.getProxy().getTarget());
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-multiplepath.json");

        Assert.assertEquals(URI.create("http://localhost:1234/weather"), api.getProxy().getTarget());

        String apiStr = objectMapper().writeValueAsString(api);

        Assert.assertNotNull(apiStr);
    }

    private Api getDefinition(String resource) throws Exception {
        URL jsonFile = DefinitionTest.class.getResource(resource);
        return objectMapper().readValue(jsonFile, Api.class);
    }

    private ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }
}
