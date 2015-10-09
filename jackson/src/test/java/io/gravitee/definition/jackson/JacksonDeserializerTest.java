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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Method;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Policy;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JacksonDeserializerTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaulthttpconfig.json");

        Assert.assertEquals(URI.create("http://localhost:1234"), api.getProxy().getTarget());
        Assert.assertNull(api.getProxy().getHttpClient());
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-overridedhttpconfig.json");

        Assert.assertEquals(URI.create("http://localhost:1234"), api.getProxy().getTarget());
        Assert.assertTrue(api.getProxy().getHttpClient().isUseProxy());

        Assert.assertNotNull(api.getProxy().getHttpClient().getHttpProxy());
    }

    @Test
    public void definition_noProxyPart() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-noproxy-part.json");

        Assert.assertNull(api.getProxy());
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-nopath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(0, api.getPaths().size());
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, Path> paths = api.getPaths();
        Assert.assertEquals("/*", paths.keySet().iterator().next());

        List<Method> subPaths = paths.get("/*").getMethods();
        Assert.assertEquals(1, subPaths.size());
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

        List<Method> Methods = paths.get("/*").getMethods();
        Assert.assertEquals(1, Methods.size());

        HttpMethod[] methods = Methods.iterator().next().getMethods();
        Assert.assertEquals(2, methods.length);
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-nohttpmethod.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, Path> paths = api.getPaths();

        List<Method> Methods = paths.get("/*").getMethods();
        Assert.assertEquals(1, Methods.size());

        HttpMethod[] methods = Methods.iterator().next().getMethods();
        Assert.assertEquals(9, methods.length);
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");
        Map<String, Path> paths = api.getPaths();
        List<Method> Methods = paths.get("/*").getMethods();

        List<Policy> policies = Methods.iterator().next().getPolicies();
        Assert.assertEquals(2, policies.size());

        Iterator<Policy> policyNames = policies.iterator();
        Assert.assertEquals("access-control", policyNames.next().getName());
        Assert.assertEquals("rate-limit", policyNames.next().getName());
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-defaultpath.json");
        Map<String, Path> paths = api.getPaths();
        List<Method> Methods = paths.get("/*").getMethods();

        List<Policy> policies = Methods.iterator().next().getPolicies();
        Assert.assertEquals(2, policies.size());

        Iterator<Policy> policyNames = policies.iterator();

        Policy accessControlPolicy = policyNames.next();
        Policy rateLimitPolicy = policyNames.next();

        Assert.assertEquals("access-control", accessControlPolicy.getName());
        Assert.assertFalse(accessControlPolicy.isEnabled());

        Assert.assertEquals("rate-limit", rateLimitPolicy.getName());
        Assert.assertTrue(rateLimitPolicy.isEnabled());
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = getDefinition("/io/gravitee/definition/jackson/api-path-withoutpolicy.json");
        Map<String, Path> paths = api.getPaths();
        List<Method> Methods = paths.get("/*").getMethods();

        Assert.assertEquals(1, Methods.size());
    }

    private Api getDefinition(String resource) throws Exception {
        URL jsonFile = JacksonDeserializerTest.class.getResource(resource);
        return objectMapper().readValue(jsonFile, Api.class);
    }

    private ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }
}
