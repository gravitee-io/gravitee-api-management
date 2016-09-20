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
package io.gravitee.definition.jackson.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeserializerTest extends AbstractTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        Endpoint endpoint = api.getProxy().getEndpoints().iterator().next();
        Assert.assertEquals("http://localhost:1234", endpoint.getTarget());
        Assert.assertNotNull(endpoint.getHttpClientOptions());
        Assert.assertFalse(endpoint.getHttpClientOptions().isUseCompression());
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-overridedhttpconfig.json", Api.class);

        Endpoint endpoint = api.getProxy().getEndpoints().iterator().next();
        Assert.assertEquals("http://localhost:1234", endpoint.getTarget());
        Assert.assertNotNull(endpoint.getHttpProxy());
        Assert.assertTrue(endpoint.getHttpProxy().isEnabled());
    }

    @Test(expected = JsonMappingException.class)
    public void definition_noProxyPart() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-noproxy-part.json", Api.class);
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-nopath.json", Api.class);

        Assert.assertNull(api.getPaths());
    }

    @Test
    public void definition_reformatContextPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-reformat-contextpath.json", Api.class);

        Assert.assertNotNull(api.getProxy().getContextPath());
        Assert.assertEquals("/my-api/team", api.getProxy().getContextPath());
    }

    @Test(expected = JsonMappingException.class)
    public void definition_contextPathExpected() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-no-contextpath.json", Api.class);
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

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
        Api api = load("/io/gravitee/definition/jackson/api-multiplepath.json", Api.class);

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(2, api.getPaths().size());
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

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
        Api api = load("/io/gravitee/definition/jackson/api-path-nohttpmethod.json", Api.class);

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
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        Map<String, Path> paths = api.getPaths();
        List<Rule> rules = paths.get("/*").getRules();

        Policy policy = rules.iterator().next().getPolicy();
        Assert.assertNotNull(policy);
        Assert.assertEquals("access-control", policy.getName());
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        Map<String, Path> paths = api.getPaths();
        List<Rule> rules = paths.get("/*").getRules();

        Rule accessControlRule = rules.get(0);
        Policy policy = accessControlRule.getPolicy();
        Assert.assertNotNull(policy);
        Assert.assertEquals("access-control", policy.getName());
        Assert.assertFalse(accessControlRule.isEnabled());

        Rule corsRule = rules.get(1);
        policy = corsRule.getPolicy();
        Assert.assertNotNull(policy);
        Assert.assertEquals("cors", policy.getName());
        Assert.assertTrue(corsRule.isEnabled());
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-withoutpolicy.json", Api.class);
        Map<String, Path> paths = api.getPaths();
        List<Rule> rules = paths.get("/*").getRules();

        Assert.assertEquals(0, rules.size());
    }

    @Test
    public void definition_apiWithoutProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withoutproperties.json", Api.class);
        Map<String, String> properties = api.getProperties();

        Assert.assertNull(properties);
    }

    @Test
    public void definition_apiWithEmptyProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withemptyproperties.json", Api.class);
        Map<String, String> properties = api.getProperties();

        Assert.assertNotNull(properties);
        Assert.assertTrue(properties.isEmpty());
    }

    @Test
    public void definition_apiWithProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withproperties.json", Api.class);
        Map<String, String> properties = api.getProperties();

        Assert.assertNotNull(properties);
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("true", properties.get("my_property"));
        Assert.assertEquals("123", properties.get("my_property2"));
        Assert.assertEquals("text", properties.get("my_property3"));
        Assert.assertEquals("text", properties.get("my_property4"));
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withoutID() throws Exception {
        load("/io/gravitee/definition/jackson/api-withoutid.json", Api.class);
    }

    @Test
    public void definition_withoutTags() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withouttags.json", Api.class);
        Assert.assertNotNull(api.getTags());
        Assert.assertEquals(0, api.getTags().size());
    }

    @Test
    public void definition_withTags() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withtags.json", Api.class);
        Assert.assertNotNull(api.getTags());
        Assert.assertEquals(2, api.getTags().size());
        Assert.assertEquals("tag1", api.getTags().iterator().next());
    }

    @Test
    public void definition_singleEndpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint.json", Api.class);
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint.json", Api.class);
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
        Assert.assertFalse(api.getProxy().getEndpoints().get(0).isBackup());
    }

    @Test
    public void definition_singleEndpoint_inArray() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json", Api.class);
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
    }

    @Test
    public void definition_multipleEndpoints() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multipleendpoints.json", Api.class);
        Assert.assertEquals(2, api.getProxy().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_inArray_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json", Api.class);
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
        Assert.assertFalse(api.getProxy().getEndpoints().get(0).isBackup());
    }

    @Test
    public void definition_multipleEndpoints_inSingleEndpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multipleendpoints-insingleendpoint.json", Api.class);
        Assert.assertEquals(1, api.getProxy().getEndpoints().size());
        Assert.assertEquals("http://host1:8083/myapi", api.getProxy().getEndpoints().iterator().next().getTarget());
    }

    @Test
    public void definition_withclientoptions() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions.json", Api.class);

        Endpoint endpoint = api.getProxy().getEndpoints().iterator().next();
        Assert.assertNotNull(endpoint.getHttpClientOptions());
        Assert.assertNotNull(endpoint.getHttpClientSslOptions());
    }

    @Test
    public void definition_withclientoptions_nossl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-nossl.json", Api.class);

        Endpoint endpoint = api.getProxy().getEndpoints().iterator().next();
        Assert.assertNotNull(endpoint.getHttpClientOptions());
        Assert.assertNull(endpoint.getHttpClientSslOptions());
    }

    @Test
    public void definition_withclientoptions_nooptions() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-nooptions.json", Api.class);

        Endpoint endpoint = api.getProxy().getEndpoints().iterator().next();
        Assert.assertNotNull(endpoint.getHttpClientOptions());
        Assert.assertNull(endpoint.getHttpClientSslOptions());
    }

    @Test
    public void definition_withclientoptions_nooptions_defaultconfiguration() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-nooptions.json", Api.class);

        Endpoint endpoint = api.getProxy().getEndpoints().iterator().next();
        Assert.assertNotNull(endpoint.getHttpClientOptions());
        HttpClientOptions options = endpoint.getHttpClientOptions();
        Assert.assertNotNull(options);
        Assert.assertEquals(HttpClientOptions.DEFAULT_CONNECT_TIMEOUT, options.getConnectTimeout());
        Assert.assertEquals(HttpClientOptions.DEFAULT_IDLE_TIMEOUT, options.getIdleTimeout());
        Assert.assertEquals(HttpClientOptions.DEFAULT_KEEP_ALIVE, options.isKeepAlive());
        Assert.assertEquals(HttpClientOptions.DEFAULT_PIPELINING, options.isPipelining());
        Assert.assertEquals(HttpClientOptions.DEFAULT_MAX_CONCURRENT_CONNECTIONS, options.getMaxConcurrentConnections());
    }

    @Test
    public void definition_defaultLoadBalancer_roundRobin() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        Assert.assertNotNull(api.getProxy().getLoadBalancer());
        Assert.assertEquals(LoadBalancerType.ROUND_ROBIN, api.getProxy().getLoadBalancer().getType());
    }

    @Test
    public void definition_no_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        Assert.assertNull(api.getProxy().getFailover());
        Assert.assertFalse(api.getProxy().failoverEnabled());
    }

    @Test
    public void definition_default_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-default-failover.json", Api.class);

        Assert.assertNotNull(api.getProxy().getFailover());
        Assert.assertTrue(api.getProxy().failoverEnabled());

        Assert.assertEquals(Failover.DEFAULT_MAX_ATTEMPTS, api.getProxy().getFailover().getMaxAttempts());
        Assert.assertEquals(Failover.DEFAULT_RETRY_TIMEOUT, api.getProxy().getFailover().getRetryTimeout());
        Assert.assertEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_override_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-override-failover.json", Api.class);

        Assert.assertNotNull(api.getProxy().getFailover());
        Assert.assertTrue(api.getProxy().failoverEnabled());

        Assert.assertEquals(3, api.getProxy().getFailover().getMaxAttempts());
        Assert.assertEquals(3000, api.getProxy().getFailover().getRetryTimeout());
        Assert.assertEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_failover_singlecase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-failover-singlecase.json", Api.class);

        Assert.assertNotNull(api.getProxy().getFailover());
        Assert.assertTrue(api.getProxy().failoverEnabled());

        Assert.assertEquals(3, api.getProxy().getFailover().getMaxAttempts());
        Assert.assertEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_failover_singlecase_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-failover-singlecase.json", Api.class);

        Assert.assertFalse(api.getProxy().getEndpoints().get(0).isBackup());
        Assert.assertTrue(api.getProxy().getEndpoints().get(1).isBackup());
    }
}
