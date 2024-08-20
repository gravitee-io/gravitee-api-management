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
package io.gravitee.definition.jackson.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.flow.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeserializerTest extends AbstractTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        assertTrue(api.getProxy().isPreserveHost());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        assertEquals("http://localhost:1234", endpoint.getTarget());
    }

    @Test
    public void definition_BestMatchFlowMode() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-bestMatchFlowMode.json", Api.class);
        assertEquals(FlowMode.BEST_MATCH, api.getFlowMode());
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-overridedhttpconfig.json", Api.class);

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        assertEquals("http://localhost:1234", endpoint.getTarget());
    }

    @Test
    public void definition_noProxyPart() {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-noproxy-part.json", Api.class));
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-nopath.json", Api.class);

        assertNull(api.getPaths());
    }

    @Test
    public void definition_reformatContextPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-reformat-contextpath.json", Api.class);

        assertNotNull(api.getProxy().getVirtualHosts());
        assertFalse(api.getProxy().getVirtualHosts().isEmpty());
        assertEquals("/my-api/team", api.getProxy().getVirtualHosts().iterator().next().getPath());
        assertNull(api.getProxy().getVirtualHosts().iterator().next().getHost());
        assertFalse(api.getProxy().getVirtualHosts().iterator().next().isOverrideEntrypoint());
    }

    @Test
    public void definition_contextPathExpected() {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-no-contextpath.json", Api.class));
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

        assertNotNull(api.getPaths());
        assertEquals(1, api.getPaths().size());

        Map<String, List<Rule>> paths = api.getPaths();
        assertEquals("/*", paths.keySet().iterator().next());

        List<Rule> rules = paths.get("/*");
        assertEquals(4, rules.size());
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multiplepath.json", Api.class);

        assertNotNull(api.getPaths());
        assertEquals(2, api.getPaths().size());
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

        assertNotNull(api.getPaths());
        assertEquals(1, api.getPaths().size());

        Map<String, List<Rule>> paths = api.getPaths();

        List<Rule> rules = paths.get("/*");
        assertEquals(4, rules.size());

        Set<HttpMethod> methods = rules.iterator().next().getMethods();
        assertEquals(2, methods.size());
    }

    @Test
    public void definition_withWildcardInPathMapping() throws IOException {
        Api api = load("/io/gravitee/definition/jackson/api-with-wildcard-in-path-mapping.json", Api.class);
        assertNotNull(api.getPathMappings());
        assertEquals(1, api.getPathMappings().size());
        Pattern pattern = api.getPathMappings().get("/echo/:*test/.*");
        assertNotNull(pattern);
        assertEquals("/echo/[^/]*/.*/*", pattern.pattern());
        assertFalse(pattern.matcher("/echo").matches());
        assertFalse(pattern.matcher("/echo/").matches());
        assertFalse(pattern.matcher("/echo/anything").matches());
        assertTrue(pattern.matcher("/echo/anything/").matches());
        assertTrue(pattern.matcher("/echo/anything/anything-else").matches());
    }

    @Test
    public void definition_withoutWildcardInPathMapping() throws IOException {
        Api api = load("/io/gravitee/definition/jackson/api-without-wildcard-in-path-mapping.json", Api.class);
        assertNotNull(api.getPathMappings());
        assertEquals(1, api.getPathMappings().size());
        Pattern pattern = api.getPathMappings().get("/echo/:test");
        assertNotNull(pattern);
        assertEquals("/echo/[^/]*/*", pattern.pattern());
        assertFalse(pattern.matcher("/echo").matches());
        assertTrue(pattern.matcher("/echo/").matches());
        assertTrue(pattern.matcher("/echo/anything").matches());
        assertTrue(pattern.matcher("/echo/anything/").matches());
        assertFalse(pattern.matcher("/echo/anything/anything-else").matches());
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-nohttpmethod.json", Api.class);

        assertNotNull(api.getPaths());
        assertEquals(1, api.getPaths().size());

        Map<String, List<Rule>> paths = api.getPaths();

        List<Rule> rules = paths.get("/*");
        assertEquals(1, rules.size());

        Set<HttpMethod> methods = rules.iterator().next().getMethods();
        assertEquals(10, methods.size());
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        Map<String, List<Rule>> paths = api.getPaths();
        List<Rule> rules = paths.get("/*");

        Policy policy = rules.iterator().next().getPolicy();
        assertNotNull(policy);
        assertEquals("access-control", policy.getName());
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        Map<String, List<Rule>> paths = api.getPaths();
        List<Rule> rules = paths.get("/*");

        Rule accessControlRule = rules.get(0);
        Policy policy = accessControlRule.getPolicy();
        assertNotNull(policy);
        assertEquals("access-control", policy.getName());
        assertFalse(accessControlRule.isEnabled());

        Rule corsRule = rules.get(1);
        policy = corsRule.getPolicy();
        assertNotNull(policy);
        assertEquals("cors", policy.getName());
        assertTrue(corsRule.isEnabled());
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-withoutpolicy.json", Api.class);
        Map<String, List<Rule>> paths = api.getPaths();
        List<Rule> rules = paths.get("/*");

        assertEquals(0, rules.size());
    }

    @Test
    public void definition_apiWithoutProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withoutproperties.json", Api.class);
        Properties properties = api.getProperties();

        assertNull(properties);
    }

    @Test
    public void definition_apiWithEmptyProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withemptyproperties.json", Api.class);
        Properties properties = api.getProperties();

        assertNotNull(properties);
        assertTrue(properties.getValues().isEmpty());
    }

    @Test
    public void definition_apiWithProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withproperties.json", Api.class);
        Properties properties = api.getProperties();

        assertNotNull(properties);
        assertEquals(4, properties.getValues().size());
        assertEquals("true", properties.getValues().get("my_property"));
        assertEquals("123", properties.getValues().get("my_property2"));
        assertEquals("text", properties.getValues().get("my_property3"));
        assertEquals("text", properties.getValues().get("my_property4"));
    }

    @Test
    public void definition_withoutID() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-withoutid.json", Api.class));
    }

    @Test
    public void definition_withoutTags() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withouttags.json", Api.class);
        assertNotNull(api.getTags());
        assertEquals(0, api.getTags().size());
    }

    @Test
    public void definition_withTags() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withtags.json", Api.class);
        assertNotNull(api.getTags());
        assertEquals(2, api.getTags().size());
        assertEquals("tag1", api.getTags().iterator().next());
    }

    @Test
    public void definition_withServers() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withservers.json", Api.class);
        assertNotNull(api.getProxy().getServers());
        assertEquals(2, api.getProxy().getServers().size());
        final Iterator<String> servers = api.getProxy().getServers().iterator();
        assertEquals("server1", servers.next());
        assertEquals("server2", servers.next());
    }

    @Test
    public void definition_singleEndpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
        assertFalse(api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().isBackup());
    }

    @Test
    public void definition_singleEndpoint_inArray() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
    }

    @Test
    public void definition_multipleEndpoints() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multipleendpoints.json", Api.class);
        assertEquals(2, api.getProxy().getGroups().iterator().next().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_inArray_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
        assertFalse(api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().isBackup());
    }

    @Test
    public void definition_multipleEndpoints_inSingleEndpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multipleendpoints-insingleendpoint.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
        assertEquals("http://host1:8083/myapi", api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget());
    }

    @Test
    public void definition_defaultLoadBalancer_roundRobin() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        assertNotNull(api.getProxy().getGroups().iterator().next().getLoadBalancer());
        assertEquals(LoadBalancerType.ROUND_ROBIN, api.getProxy().getGroups().iterator().next().getLoadBalancer().getType());
    }

    @Test
    public void definition_no_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        assertNull(api.getProxy().getFailover());
        assertFalse(api.getProxy().failoverEnabled());
    }

    @Test
    public void definition_default_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-default-failover.json", Api.class);

        assertNotNull(api.getProxy().getFailover());
        assertTrue(api.getProxy().failoverEnabled());

        assertEquals(Failover.DEFAULT_MAX_ATTEMPTS, api.getProxy().getFailover().getMaxAttempts());
        assertEquals(Failover.DEFAULT_RETRY_TIMEOUT, api.getProxy().getFailover().getRetryTimeout());
        assertEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_override_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-override-failover.json", Api.class);

        assertNotNull(api.getProxy().getFailover());
        assertTrue(api.getProxy().failoverEnabled());

        assertEquals(3, api.getProxy().getFailover().getMaxAttempts());
        assertEquals(3000, api.getProxy().getFailover().getRetryTimeout());
        assertArrayEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_failover_singlecase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-failover-singlecase.json", Api.class);

        assertNotNull(api.getProxy().getFailover());
        assertTrue(api.getProxy().failoverEnabled());

        assertEquals(3, api.getProxy().getFailover().getMaxAttempts());
        assertArrayEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_failover_singlecase_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-failover-singlecase.json", Api.class);

        api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getEndpoints()
            .forEach(endpoint -> {
                if ("endpoint_0".equals(endpoint.getName())) {
                    assertFalse(endpoint.isBackup());
                } else {
                    assertTrue(endpoint.isBackup());
                }
            });
    }

    @Test
    public void definition_multiTenant_enable() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multitenant.json", Api.class);

        assertEquals("europe", api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTenants().get(0));
    }

    @Test
    public void definition_multiTenants_enable() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multitenants.json", Api.class);

        List<String> tenants = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTenants();
        assertEquals("europe", tenants.get(0));
        assertEquals("asie", tenants.get(1));
    }

    @Test
    public void shouldFailWithSameEndpointNames() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-multiplesameendpoints.json", Api.class));
    }

    @Test
    public void shouldFailWithSameEndpointNamesInDifferentGroup() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () -> load("/io/gravitee/definition/jackson/api-multiplesameendpointsindifferentgroups.json", Api.class)
        );
    }

    @Test
    public void shouldFailWithSameGroupEndpointNames() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () -> load("/io/gravitee/definition/jackson/api-multiplesamegroupendpoints.json", Api.class)
        );
    }

    @Test
    public void shouldFailWithSameGroupEndpointNamesAndEndpointNames() throws Exception {
        assertThrows(
            JsonMappingException.class,
            () -> load("/io/gravitee/definition/jackson/api-multiplesamegroupendpointsandendpoints.json", Api.class)
        );
    }

    @Test
    public void definition_noCors() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-hostHeader.json", Api.class);

        Cors cors = api.getProxy().getCors();
        assertNull(cors);
    }

    @Test
    public void definition_old_headers() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/old-api-headers.json", Api.class);

        EndpointGroup endpointGroup = api.getProxy().getGroups().iterator().next();
        Endpoint endpoint = endpointGroup.getEndpoints().iterator().next();
        JsonNode endpointConfiguration = objectMapper().readTree(endpoint.getConfiguration());
        assertTrue(endpointConfiguration.has("headers"));
        assertEquals(
            endpointConfiguration.get("headers").toString(),
            "[{\"name\":\"x-header-1\",\"value\":\"header-1\"},{\"name\":\"Host\",\"value\":\"host\"}]"
        );
    }

    @Test
    public void definition_withDisabledCors() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-cors-disabled.json", Api.class);

        Cors cors = api.getProxy().getCors();
        assertNotNull(cors);
        assertFalse(cors.isEnabled());
        assertFalse(cors.isAccessControlAllowCredentials());
        assertFalse(cors.isRunPolicies());
        assertEquals(-1, cors.getAccessControlMaxAge());
        assertEquals(HttpStatusCode.BAD_REQUEST_400, cors.getErrorStatusCode());
        assertNull(cors.getAccessControlAllowOrigin());
        assertNull(cors.getAccessControlAllowOriginRegex());
        assertNull(cors.getAccessControlAllowHeaders());
        assertNull(cors.getAccessControlAllowMethods());
        assertNull(cors.getAccessControlExposeHeaders());
    }

    @Test
    public void definition_withCors_defaultValues() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-cors.json", Api.class);

        Cors cors = api.getProxy().getCors();
        assertNotNull(cors);
        assertTrue(cors.isEnabled());
        assertFalse(cors.isAccessControlAllowCredentials());
        assertFalse(cors.isRunPolicies());
        assertEquals(-1, cors.getAccessControlMaxAge());
        assertEquals(HttpStatusCode.BAD_REQUEST_400, cors.getErrorStatusCode());
        assertNotNull(cors.getAccessControlAllowOrigin());
        assertNotNull(cors.getAccessControlAllowOriginRegex());
        assertNotNull(cors.getAccessControlAllowHeaders());
        assertNotNull(cors.getAccessControlAllowMethods());
        assertNotNull(cors.getAccessControlExposeHeaders());
    }

    @Test
    public void definition_withLogging_defaultValues() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-logging.json", Api.class);

        Logging logging = api.getProxy().getLogging();
        assertNotNull(logging);
        assertEquals(LoggingMode.NONE, logging.getMode());
        assertEquals(LoggingScope.NONE, logging.getScope());
        assertEquals(LoggingContent.NONE, logging.getContent());
        assertEquals("my condition", logging.getCondition());
    }

    @Test
    public void definition_withLogging_clientMode() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-logging-client.json", Api.class);

        Logging logging = api.getProxy().getLogging();
        assertNotNull(logging);
        assertEquals(LoggingMode.CLIENT_PROXY, logging.getMode());
        assertEquals(LoggingScope.REQUEST, logging.getScope());
        assertEquals(LoggingContent.HEADERS, logging.getContent());
        assertEquals("my condition", logging.getCondition());
    }

    @Test
    public void definition_defaultgroup_withDiscovery() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-endpointgroup-discovery.json", Api.class);

        EndpointGroup group = api.getProxy().getGroups().iterator().next();
        assertNotNull(group);
    }

    @Test
    public void definition_withResponseTemplates() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-response-templates.json", Api.class);

        Map<String, Map<String, ResponseTemplate>> responseTemplates = api.getResponseTemplates();
        assertNotNull(responseTemplates);

        Map<String, ResponseTemplate> apiKeyResponseTemplates = responseTemplates.get("API_KEY_INVALID");
        assertNotNull(apiKeyResponseTemplates);

        assertEquals(3, apiKeyResponseTemplates.size());
        Iterator<String> responseTemplateIterator = apiKeyResponseTemplates.keySet().iterator();

        assertEquals("application/json", responseTemplateIterator.next());
        assertEquals("text/xml", responseTemplateIterator.next());
        assertEquals("*", responseTemplateIterator.next());

        ResponseTemplate responseTemplate = apiKeyResponseTemplates.get("application/json");
        assertEquals(403, responseTemplate.getStatusCode());
        assertEquals("{}", responseTemplate.getBody());
        assertEquals("header1", responseTemplate.getHeaders().get("x-header1"));
        assertEquals("header2", responseTemplate.getHeaders().get("x-header2"));
    }

    @Test
    public void definition_virtualhosts() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-virtualhosts.json", Api.class);

        assertNotNull(api.getProxy().getVirtualHosts());
        assertEquals(2, api.getProxy().getVirtualHosts().size());

        VirtualHost host1 = api.getProxy().getVirtualHosts().get(0);
        VirtualHost host2 = api.getProxy().getVirtualHosts().get(1);

        assertEquals("localhost", host1.getHost());
        assertEquals("/my-api", host1.getPath());
        assertTrue(host1.isOverrideEntrypoint());

        assertNull(host2.getHost());
        assertEquals("/my-api2", host2.getPath());
        assertFalse(host2.isOverrideEntrypoint());
    }

    @Test
    public void definition_http2_endpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-http2-endpoint.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        assertEquals("http", endpoint.getType());
    }

    @Test
    public void definition_grpc_endpoint_ssl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint-ssl.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        assertEquals("grpc", endpoint.getType());
    }

    @Test
    public void definition_grpc_endpoint_without_type() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint-without-type.json", Api.class);
        assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        assertEquals("http", endpoint.getType());
    }

    @Test
    public void definition_defaultFlow() throws Exception {
        assertNotNull(DefinitionVersion.valueOfLabel("2.0.0"));
        Api api = load("/io/gravitee/definition/jackson/api-defaultflow.json", Api.class);

        assertNotNull(api.getDefinitionVersion());
        assertEquals(DefinitionVersion.V2, api.getDefinitionVersion());
        assertNotNull(api.getFlows());
        List<Flow> flows = api.getFlows();
        assertEquals(1, flows.size());
        Flow flow = flows.get(0);
        assertEquals(3, flow.getPre().size());
        assertEquals(2, flow.getPost().size());
        assertEquals(3, flow.getMethods().size());
        assertTrue(flow.getMethods().containsAll(Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.GET)));
        assertEquals("/", flow.getPath());
        assertNotNull(flow.getConsumers());
        assertEquals(2, flow.getConsumers().size());
        final Consumer consumer = flow.getConsumers().get(0);
        assertNotNull(consumer);
        assertEquals("PUBLIC", consumer.getConsumerId());
        assertEquals(ConsumerType.TAG, consumer.getConsumerType());
        final Consumer consumer2 = flow.getConsumers().get(1);
        assertNotNull(consumer2);
        assertEquals("PRIVATE", consumer2.getConsumerId());
        assertEquals(ConsumerType.TAG, consumer2.getConsumerType());
        assertEquals(FlowStage.API, flow.getStage());

        Step rule = flow.getPre().get(0);
        assertNotNull(rule);
        assertEquals("Rate Limit", rule.getName());
        assertEquals("rate-limit", rule.getPolicy());
        assertNotNull(rule.getConfiguration());
        assertTrue(rule.isEnabled());
        assertNull(rule.getCondition());

        Step ruleApiKey = flow.getPre().get(1);
        assertNotNull(ruleApiKey);
        assertEquals("Check API Key", ruleApiKey.getName());
        assertEquals("api-key", ruleApiKey.getPolicy());
        assertNotNull(ruleApiKey.getConfiguration());
        assertTrue(ruleApiKey.isEnabled());
        assertNull(ruleApiKey.getCondition());

        Step ruleTransformHeaders = flow.getPre().get(2);
        assertNotNull(ruleTransformHeaders);
        assertEquals("Add HTTP headers", ruleTransformHeaders.getName());
        assertEquals("transform-headers", ruleTransformHeaders.getPolicy());
        assertNotNull(ruleTransformHeaders.getConfiguration());
        assertTrue(ruleTransformHeaders.isEnabled());
        assertEquals("a non empty condition", ruleTransformHeaders.getCondition());

        Collection<Plan> plans = api.getPlans();
        assertNotNull(plans);
        assertEquals(2, plans.size());
        assertNotNull(api.getPlan("plan-1"));
        assertEquals(2, api.getPlan("plan-1").getFlows().size());
        assertEquals("#context.attributes['jwt'].claims['iss'] == 'toto'", api.getPlan("plan-1").getSelectionRule());
        assertEquals("OAUTH2", api.getPlan("plan-1").getSecurity());
        assertEquals(2, api.getPlan("plan-1").getTags().size());
        assertEquals("PUBLISHED", api.getPlan("plan-1").getStatus());
        assertEquals(FlowStage.PLAN, api.getPlan("plan-1").getFlows().get(0).getStage());

        assertEquals(FlowMode.DEFAULT, api.getFlowMode());
    }

    @Test
    public void definition_v2_withPath() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-v2-withpath.json", Api.class));
    }

    @Test
    public void definition_v1_withFlow() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-v1-withflow.json", Api.class));
    }

    @Test
    public void shouldDefaultDefinitionExecutionModeEqualV3WhenJsonContainsNull() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-default-executionmode.json", Api.class);

        assertEquals(api.getExecutionMode(), ExecutionMode.V3);
    }

    @Test
    public void shouldDefinitionExecutionModeEqualV3WhenJsonContainsV3() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-executionmode-v3.json", Api.class);

        assertEquals(ExecutionMode.V3, api.getExecutionMode());
    }

    @Test
    public void shouldDefinitionExecutionModeEqualV4EmulationEngineWhenJsonContainsV4EmulationEngine() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-executionmode-v4-emulation-engine.json", Api.class);

        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, api.getExecutionMode());
    }

    @Test
    public void shouldDefinitionExecutionModeEqualV4EmulationEngineWhenJsonContainsJupiter() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-executionmode-jupiter.json", Api.class);

        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, api.getExecutionMode());
    }

    @Test
    public void definition_withclientoptions_propagateClientAcceptEncoding() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-propagateClientAcceptEncoding.json", Api.class);

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        JsonNode endpointConfiguration = objectMapper().readTree(endpoint.getConfiguration());
        assertTrue(endpointConfiguration.has("http"));
        assertEquals(endpointConfiguration.get("http").toString(), "{\"useCompression\":false,\"propagateClientAcceptEncoding\":true}");
    }

    @Test
    public void shouldHaveADefinitionContext() throws IOException {
        Api api = load("/io/gravitee/definition/jackson/api-with-definition-context.json", Api.class);
        DefinitionContext definitionContext = api.getDefinitionContext();
        assertNotNull(definitionContext);
        assertEquals(DefinitionContext.ORIGIN_KUBERNETES, definitionContext.getOrigin());
        assertEquals(DefinitionContext.MODE_FULLY_MANAGED, definitionContext.getMode());
    }

    @Test
    public void shouldSetPlanApiFromApiId() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-plan-without-apiId.json", Api.class);

        assertEquals(1, api.getPlans().size());
        assertEquals("my-api-id", api.getPlans().get(0).getApi());
    }

    @Test
    public void definition_federated() throws IOException {
        Api api = load("/io/gravitee/definition/jackson/api-federated.json", Api.class);

        assertThat(api.getProxy()).isNull();
        assertThat(api.getName()).isEqualTo("IrishUsagePlanAPI100");
    }
}
