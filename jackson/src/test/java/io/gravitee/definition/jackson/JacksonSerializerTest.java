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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JacksonSerializerTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaulthttpconfig.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-overridedhttpconfig.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-nopath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-multiplepath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-nohttpmethod.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-withoutpolicy.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_apiWithoutProperties() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withoutproperties.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_apiWithEmptyProperties() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withemptyproperties.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_apiWithProperties() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withproperties.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_withMonitoring() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withmonitoring.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    @Test
    public void definition_withMonitoring_unitInLowerCase() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withmonitoring-unitInLowerCase.json");

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);
    }

    private Api getDefinition(String resource) throws Exception {
        URL jsonFile = JacksonDeserializerTest.class.getResource(resource);
        return objectMapper().readValue(jsonFile, Api.class);
    }

    private ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }
}
