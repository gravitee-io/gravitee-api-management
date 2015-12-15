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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JacksonDeserializerTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaulthttpconfig.json");

        Assert.assertEquals("http://localhost:1234", api.getProxy().getEndpoints().iterator().next());
        Assert.assertNull(api.getProxy().getHttpClient());
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-overridedhttpconfig.json");

        Assert.assertEquals("http://localhost:1234", api.getProxy().getEndpoints().iterator().next());
        Assert.assertTrue(api.getProxy().getHttpClient().isUseProxy());

        Assert.assertNotNull(api.getProxy().getHttpClient().getHttpProxy());
    }

    @Test(expected = JsonMappingException.class)
    public void definition_noProxyPart() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-noproxy-part.json");
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-nopath.json");

        Assert.assertNull(api.getPaths());
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, Path> paths = api.getPaths();
        Assert.assertEquals("/*", paths.keySet().iterator().next());

        Assert.assertEquals("/*", paths.get("/*").getPath());
        List<Rule> rules = paths.get("/*").getRules();
        Assert.assertEquals(4, rules.size());
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-multiplepath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(2, api.getPaths().size());
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, Path> paths = api.getPaths();

        List<Rule> rules = paths.get("/*").getRules();
        Assert.assertEquals(4, rules.size());

        List<HttpMethod> methods = rules.iterator().next().getMethods();
        Assert.assertEquals(2, methods.size());
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-nohttpmethod.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, Path> paths = api.getPaths();

        List<Rule> rules = paths.get("/*").getRules();
        Assert.assertEquals(1, rules.size());

        List<HttpMethod> methods = rules.iterator().next().getMethods();
        Assert.assertEquals(9, methods.size());
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");
        Map<String, Path> paths = api.getPaths();
        List<Rule> rules = paths.get("/*").getRules();

        Policy policy = rules.iterator().next().getPolicy();
        Assert.assertNotNull(policy);
        Assert.assertEquals("access-control", policy.getName());
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");
        Map<String, Path> paths = api.getPaths();
        List<Rule> rules= paths.get("/*").getRules();

        Policy policy = rules.iterator().next().getPolicy();
        Assert.assertNotNull(policy);

        Assert.assertEquals("access-control", policy.getName());
        Assert.assertFalse(policy.isEnabled());
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-withoutpolicy.json");
        Map<String, Path> paths = api.getPaths();
        List<Rule> rules = paths.get("/*").getRules();

        Assert.assertEquals(0, rules.size());
    }

    @Test
    public void definition_apiWithoutProperties() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withoutproperties.json");
        Map<String, Object> properties = api.getProperties();

        Assert.assertNull(properties);
    }

    @Test
    public void definition_apiWithEmptyProperties() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withemptyproperties.json");
        Map<String, Object> properties = api.getProperties();

        Assert.assertNotNull(properties);
        Assert.assertTrue(properties.isEmpty());
    }

    @Test
    public void definition_apiWithProperties() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withproperties.json");
        Map<String, Object> properties = api.getProperties();

        Assert.assertNotNull(properties);
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals(properties.get("my_property"), true);
        Assert.assertNotEquals(properties.get("my_property"), "true");
        Assert.assertEquals(properties.get("my_property2"), 123);
        Assert.assertEquals(properties.get("my_property3"), "text");
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withoutID() throws Exception {
        getDefinition("/io/gravitee/definition/jackson/api-withoutid.json");
    }

    @Test
    public void definition_withMonitoring() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withmonitoring.json");
        Assert.assertNotNull(api.getMonitoring());
        Assert.assertTrue(api.getMonitoring().isEnabled());
    }

    @Test(expected = IllegalArgumentException.class)
    public void definition_withMonitoring_badUnit() throws Exception {
        getDefinition("/io/gravitee/definition/jackson/api-withmonitoring-badUnit.json");
    }

    @Test
    public void definition_withMonitoring_unitInLowerCase() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withmonitoring-unitInLowerCase.json");
        Assert.assertNotNull(api.getMonitoring());
        Assert.assertFalse(api.getMonitoring().isEnabled());
    }

    @Test
    public void definition_withoutTags() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withouttags.json");
        Assert.assertNotNull(api.getTags());
        Assert.assertEquals(0, api.getTags().size());
    }

    @Test
    public void definition_withTags() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-withtags.json");
        Assert.assertNotNull(api.getTags());
        Assert.assertEquals(2, api.getTags().size());
        Assert.assertEquals("tag1", api.getTags().iterator().next());
    }

    @Test
    public void definition_singleEndpoint() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-singleendpoint.json");
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_inArray() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json");
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
    }

    @Test
    public void definition_multipleEndpoints() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-multipleendpoints.json");
        Assert.assertEquals(2, api.getProxy().getEndpoints().size());
    }

    @Test
    public void definition_multipleEndpoints_inSingleEndpoint() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-multipleendpoints-insingleendpoint.json");
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
        Assert.assertEquals("http://host1:8083/myapi", api.getProxy().getEndpoints().iterator().next());
    }

    private Api getDefinition(String resource) throws Exception {
        URL jsonFile = JacksonDeserializerTest.class.getResource(resource);
        return objectMapper().readValue(jsonFile, Api.class);
    }

    private ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }
}
