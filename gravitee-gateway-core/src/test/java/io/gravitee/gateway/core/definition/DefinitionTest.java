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
package io.gravitee.gateway.core.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URL;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefinitionTest {

    @Test
    public void test() throws Exception {
        URL jsonFile = DefinitionTest.class.getResource("/io/gravitee/gateway/core/definition/my-api.json");
        ApiDefinition api = objectMapper().readValue(jsonFile, ApiDefinition.class);

        Assert.assertEquals(URI.create("http://localhost:1234"), api.getProxy().getTarget());
        Assert.assertTrue(api.getProxy().getHttpClient().isUseProxy());

        Assert.assertEquals(2, api.getPaths().size());

        String policyKey = api.getPaths().keySet().iterator().next();
        Assert.assertEquals("/*", policyKey);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
