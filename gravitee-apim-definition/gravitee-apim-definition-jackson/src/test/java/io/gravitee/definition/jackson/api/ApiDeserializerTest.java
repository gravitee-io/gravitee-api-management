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
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeserializerTest extends AbstractTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        Assertions.assertTrue(api.getProxy().isPreserveHost());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        Assertions.assertEquals("http://localhost:1234", endpoint.getTarget());
    }

    @Test
    public void definition_BestMatchFlowMode() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-bestMatchFlowMode.json", Api.class);
        Assertions.assertEquals(FlowMode.BEST_MATCH, api.getFlowMode());
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-overridedhttpconfig.json", Api.class);

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        Assertions.assertEquals("http://localhost:1234", endpoint.getTarget());
    }

    @Test
    public void definition_noProxyPart() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-noproxy-part.json", Api.class));
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-nopath.json", Api.class);

        Assertions.assertNull(api.getPaths());
    }

    @Test
    public void definition_reformatContextPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-reformat-contextpath.json", Api.class);

        Assertions.assertNotNull(api.getProxy().getVirtualHosts());
        Assertions.assertFalse(api.getProxy().getVirtualHosts().isEmpty());
        Assertions.assertEquals("/my-api/team", api.getProxy().getVirtualHosts().iterator().next().getPath());
        Assertions.assertNull(api.getProxy().getVirtualHosts().iterator().next().getHost());
        Assertions.assertFalse(api.getProxy().getVirtualHosts().iterator().next().isOverrideEntrypoint());
    }

    @Test
    public void definition_contextPathExpected() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-no-contextpath.json", Api.class));
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

        Assertions.assertNotNull(api.getPaths());
        Assertions.assertEquals(1, api.getPaths().size());

        Map<String, List<Rule>> paths = api.getPaths();
        Assertions.assertEquals("/*", paths.keySet().iterator().next());

        List<Rule> rules = paths.get("/*");
        Assertions.assertEquals(4, rules.size());
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multiplepath.json", Api.class);

        Assertions.assertNotNull(api.getPaths());
        Assertions.assertEquals(2, api.getPaths().size());
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);

        Assertions.assertNotNull(api.getPaths());
        Assertions.assertEquals(1, api.getPaths().size());

        Map<String, List<Rule>> paths = api.getPaths();

        List<Rule> rules = paths.get("/*");
        Assertions.assertEquals(4, rules.size());

        Set<HttpMethod> methods = rules.iterator().next().getMethods();
        Assertions.assertEquals(2, methods.size());
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-nohttpmethod.json", Api.class);

        Assertions.assertNotNull(api.getPaths());
        Assertions.assertEquals(1, api.getPaths().size());

        Map<String, List<Rule>> paths = api.getPaths();

        List<Rule> rules = paths.get("/*");
        Assertions.assertEquals(1, rules.size());

        Set<HttpMethod> methods = rules.iterator().next().getMethods();
        Assertions.assertEquals(10, methods.size());
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        Map<String, List<Rule>> paths = api.getPaths();
        List<Rule> rules = paths.get("/*");

        Policy policy = rules.iterator().next().getPolicy();
        Assertions.assertNotNull(policy);
        Assertions.assertEquals("access-control", policy.getName());
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        Map<String, List<Rule>> paths = api.getPaths();
        List<Rule> rules = paths.get("/*");

        Rule accessControlRule = rules.get(0);
        Policy policy = accessControlRule.getPolicy();
        Assertions.assertNotNull(policy);
        Assertions.assertEquals("access-control", policy.getName());
        Assertions.assertFalse(accessControlRule.isEnabled());

        Rule corsRule = rules.get(1);
        policy = corsRule.getPolicy();
        Assertions.assertNotNull(policy);
        Assertions.assertEquals("cors", policy.getName());
        Assertions.assertTrue(corsRule.isEnabled());
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-withoutpolicy.json", Api.class);
        Map<String, List<Rule>> paths = api.getPaths();
        List<Rule> rules = paths.get("/*");

        Assertions.assertEquals(0, rules.size());
    }

    @Test
    public void definition_apiWithoutProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withoutproperties.json", Api.class);
        Properties properties = api.getProperties();

        Assertions.assertNull(properties);
    }

    @Test
    public void definition_apiWithEmptyProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withemptyproperties.json", Api.class);
        Properties properties = api.getProperties();

        Assertions.assertNotNull(properties);
        Assertions.assertTrue(properties.getValues().isEmpty());
    }

    @Test
    public void definition_apiWithProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withproperties.json", Api.class);
        Properties properties = api.getProperties();

        Assertions.assertNotNull(properties);
        Assertions.assertEquals(4, properties.getValues().size());
        Assertions.assertEquals("true", properties.getValues().get("my_property"));
        Assertions.assertEquals("123", properties.getValues().get("my_property2"));
        Assertions.assertEquals("text", properties.getValues().get("my_property3"));
        Assertions.assertEquals("text", properties.getValues().get("my_property4"));
    }

    @Test
    public void definition_withoutID() throws Exception {
        assertThrows(JsonMappingException.class, () -> load("/io/gravitee/definition/jackson/api-withoutid.json", Api.class));
    }

    @Test
    public void definition_withoutTags() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withouttags.json", Api.class);
        Assertions.assertNotNull(api.getTags());
        Assertions.assertEquals(0, api.getTags().size());
    }

    @Test
    public void definition_withTags() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withtags.json", Api.class);
        Assertions.assertNotNull(api.getTags());
        Assertions.assertEquals(2, api.getTags().size());
        Assertions.assertEquals("tag1", api.getTags().iterator().next());
    }

    @Test
    public void definition_withServers() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withservers.json", Api.class);
        Assertions.assertNotNull(api.getProxy().getServers());
        Assertions.assertEquals(2, api.getProxy().getServers().size());
        final Iterator<String> servers = api.getProxy().getServers().iterator();
        Assertions.assertEquals("server1", servers.next());
        Assertions.assertEquals("server2", servers.next());
    }

    @Test
    public void definition_singleEndpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
        Assertions.assertFalse(api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().isBackup());
    }

    @Test
    public void definition_singleEndpoint_inArray() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
    }

    @Test
    public void definition_multipleEndpoints() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multipleendpoints.json", Api.class);
        Assertions.assertEquals(2, api.getProxy().getGroups().iterator().next().getEndpoints().size());
    }

    @Test
    public void definition_singleEndpoint_inArray_backup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-singleendpoint-inarray.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
        Assertions.assertFalse(api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().isBackup());
    }

    @Test
    public void definition_multipleEndpoints_inSingleEndpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multipleendpoints-insingleendpoint.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());
        Assertions.assertEquals(
            "http://host1:8083/myapi",
            api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTarget()
        );
    }

    @Test
    public void definition_defaultLoadBalancer_roundRobin() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        Assertions.assertNotNull(api.getProxy().getGroups().iterator().next().getLoadBalancer());
        Assertions.assertEquals(LoadBalancerType.ROUND_ROBIN, api.getProxy().getGroups().iterator().next().getLoadBalancer().getType());
    }

    @Test
    public void definition_no_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);

        Assertions.assertNull(api.getProxy().getFailover());
        Assertions.assertFalse(api.getProxy().failoverEnabled());
    }

    @Test
    public void definition_default_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-default-failover.json", Api.class);

        Assertions.assertNotNull(api.getProxy().getFailover());
        Assertions.assertTrue(api.getProxy().failoverEnabled());

        Assertions.assertEquals(Failover.DEFAULT_MAX_ATTEMPTS, api.getProxy().getFailover().getMaxAttempts());
        Assertions.assertEquals(Failover.DEFAULT_RETRY_TIMEOUT, api.getProxy().getFailover().getRetryTimeout());
        Assertions.assertEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_override_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-override-failover.json", Api.class);

        Assertions.assertNotNull(api.getProxy().getFailover());
        Assertions.assertTrue(api.getProxy().failoverEnabled());

        Assertions.assertEquals(3, api.getProxy().getFailover().getMaxAttempts());
        Assertions.assertEquals(3000, api.getProxy().getFailover().getRetryTimeout());
        Assertions.assertArrayEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
    }

    @Test
    public void definition_failover_singlecase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-failover-singlecase.json", Api.class);

        Assertions.assertNotNull(api.getProxy().getFailover());
        Assertions.assertTrue(api.getProxy().failoverEnabled());

        Assertions.assertEquals(3, api.getProxy().getFailover().getMaxAttempts());
        Assertions.assertArrayEquals(Failover.DEFAULT_FAILOVER_CASES, api.getProxy().getFailover().getCases());
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
                    Assertions.assertFalse(endpoint.isBackup());
                } else {
                    Assertions.assertTrue(endpoint.isBackup());
                }
            });
    }

    @Test
    public void definition_multiTenant_enable() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multitenant.json", Api.class);

        Assertions.assertEquals(
            "europe",
            api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTenants().get(0)
        );
    }

    @Test
    public void definition_multiTenants_enable() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multitenants.json", Api.class);

        List<String> tenants = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().getTenants();
        Assertions.assertEquals("europe", tenants.get(0));
        Assertions.assertEquals("asie", tenants.get(1));
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
        Assertions.assertNull(cors);
    }

    @Test
    public void definition_old_headers() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/old-api-headers.json", Api.class);

        EndpointGroup endpointGroup = api.getProxy().getGroups().iterator().next();
        Endpoint endpoint = endpointGroup.getEndpoints().iterator().next();
        JsonNode endpointConfiguration = objectMapper().readTree(endpoint.getConfiguration());
        Assertions.assertTrue(endpointConfiguration.has("headers"));
        Assertions.assertEquals(
            endpointConfiguration.get("headers").toString(),
            "[{\"name\":\"x-header-1\",\"value\":\"header-1\"},{\"name\":\"Host\",\"value\":\"host\"}]"
        );
    }

    @Test
    public void definition_withDisabledCors() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-cors-disabled.json", Api.class);

        Cors cors = api.getProxy().getCors();
        Assertions.assertNotNull(cors);
        Assertions.assertFalse(cors.isEnabled());
        Assertions.assertFalse(cors.isAccessControlAllowCredentials());
        Assertions.assertFalse(cors.isRunPolicies());
        Assertions.assertEquals(-1, cors.getAccessControlMaxAge());
        Assertions.assertEquals(HttpStatusCode.BAD_REQUEST_400, cors.getErrorStatusCode());
        Assertions.assertNull(cors.getAccessControlAllowOrigin());
        Assertions.assertNull(cors.getAccessControlAllowOriginRegex());
        Assertions.assertNull(cors.getAccessControlAllowHeaders());
        Assertions.assertNull(cors.getAccessControlAllowMethods());
        Assertions.assertNull(cors.getAccessControlExposeHeaders());
    }

    @Test
    public void definition_withCors_defaultValues() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-cors.json", Api.class);

        Cors cors = api.getProxy().getCors();
        Assertions.assertNotNull(cors);
        Assertions.assertTrue(cors.isEnabled());
        Assertions.assertFalse(cors.isAccessControlAllowCredentials());
        Assertions.assertFalse(cors.isRunPolicies());
        Assertions.assertEquals(-1, cors.getAccessControlMaxAge());
        Assertions.assertEquals(HttpStatusCode.BAD_REQUEST_400, cors.getErrorStatusCode());
        Assertions.assertNotNull(cors.getAccessControlAllowOrigin());
        Assertions.assertNotNull(cors.getAccessControlAllowOriginRegex());
        Assertions.assertNotNull(cors.getAccessControlAllowHeaders());
        Assertions.assertNotNull(cors.getAccessControlAllowMethods());
        Assertions.assertNotNull(cors.getAccessControlExposeHeaders());
    }

    @Test
    public void definition_withLogging_defaultValues() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-logging.json", Api.class);

        Logging logging = api.getProxy().getLogging();
        Assertions.assertNotNull(logging);
        Assertions.assertEquals(LoggingMode.NONE, logging.getMode());
        Assertions.assertEquals(LoggingScope.NONE, logging.getScope());
        Assertions.assertEquals(LoggingContent.NONE, logging.getContent());
        Assertions.assertEquals("my condition", logging.getCondition());
    }

    @Test
    public void definition_withLogging_clientMode() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-logging-client.json", Api.class);

        Logging logging = api.getProxy().getLogging();
        Assertions.assertNotNull(logging);
        Assertions.assertEquals(LoggingMode.CLIENT_PROXY, logging.getMode());
        Assertions.assertEquals(LoggingScope.REQUEST, logging.getScope());
        Assertions.assertEquals(LoggingContent.HEADERS, logging.getContent());
        Assertions.assertEquals("my condition", logging.getCondition());
    }

    @Test
    public void definition_defaultgroup_withDiscovery() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-endpointgroup-discovery.json", Api.class);

        EndpointGroup group = api.getProxy().getGroups().iterator().next();
        Assertions.assertNotNull(group);
    }

    @Test
    public void definition_withResponseTemplates() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-response-templates.json", Api.class);

        Map<String, Map<String, ResponseTemplate>> responseTemplates = api.getResponseTemplates();
        Assertions.assertNotNull(responseTemplates);

        Map<String, ResponseTemplate> apiKeyResponseTemplates = responseTemplates.get("API_KEY_INVALID");
        Assertions.assertNotNull(apiKeyResponseTemplates);

        Assertions.assertEquals(3, apiKeyResponseTemplates.size());
        Iterator<String> responseTemplateIterator = apiKeyResponseTemplates.keySet().iterator();

        Assertions.assertEquals("application/json", responseTemplateIterator.next());
        Assertions.assertEquals("text/xml", responseTemplateIterator.next());
        Assertions.assertEquals("*", responseTemplateIterator.next());

        ResponseTemplate responseTemplate = apiKeyResponseTemplates.get("application/json");
        Assertions.assertEquals(403, responseTemplate.getStatusCode());
        Assertions.assertEquals("{}", responseTemplate.getBody());
        Assertions.assertEquals("header1", responseTemplate.getHeaders().get("x-header1"));
        Assertions.assertEquals("header2", responseTemplate.getHeaders().get("x-header2"));
    }

    @Test
    public void definition_virtualhosts() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-virtualhosts.json", Api.class);

        Assertions.assertNotNull(api.getProxy().getVirtualHosts());
        Assertions.assertEquals(2, api.getProxy().getVirtualHosts().size());

        VirtualHost host1 = api.getProxy().getVirtualHosts().get(0);
        VirtualHost host2 = api.getProxy().getVirtualHosts().get(1);

        Assertions.assertEquals("localhost", host1.getHost());
        Assertions.assertEquals("/my-api", host1.getPath());
        Assertions.assertTrue(host1.isOverrideEntrypoint());

        Assertions.assertNull(host2.getHost());
        Assertions.assertEquals("/my-api2", host2.getPath());
        Assertions.assertFalse(host2.isOverrideEntrypoint());
    }

    @Test
    public void definition_http2_endpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-http2-endpoint.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        Assertions.assertEquals("http", endpoint.getType());
    }

    @Test
    public void definition_grpc_endpoint_ssl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint-ssl.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        Assertions.assertEquals("grpc", endpoint.getType());
    }

    @Test
    public void definition_grpc_endpoint_without_type() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint-without-type.json", Api.class);
        Assertions.assertEquals(1, api.getProxy().getGroups().iterator().next().getEndpoints().size());

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        Assertions.assertEquals("http", endpoint.getType());
    }

    @Test
    public void definition_defaultFlow() throws Exception {
        Assertions.assertNotNull(DefinitionVersion.valueOfLabel("2.0.0"));
        Api api = load("/io/gravitee/definition/jackson/api-defaultflow.json", Api.class);

        Assertions.assertNotNull(api.getDefinitionVersion());
        Assertions.assertEquals(DefinitionVersion.V2, api.getDefinitionVersion());
        Assertions.assertNotNull(api.getFlows());
        List<Flow> flows = api.getFlows();
        Assertions.assertEquals(1, flows.size());
        Flow flow = flows.get(0);
        Assertions.assertEquals(3, flow.getPre().size());
        Assertions.assertEquals(2, flow.getPost().size());
        Assertions.assertEquals(3, flow.getMethods().size());
        Assertions.assertTrue(flow.getMethods().containsAll(Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.GET)));
        Assertions.assertEquals("/", flow.getPath());
        Assertions.assertNotNull(flow.getConsumers());
        Assertions.assertEquals(2, flow.getConsumers().size());
        final Consumer consumer = flow.getConsumers().get(0);
        Assertions.assertNotNull(consumer);
        Assertions.assertEquals("PUBLIC", consumer.getConsumerId());
        Assertions.assertEquals(ConsumerType.TAG, consumer.getConsumerType());
        final Consumer consumer2 = flow.getConsumers().get(1);
        Assertions.assertNotNull(consumer2);
        Assertions.assertEquals("PRIVATE", consumer2.getConsumerId());
        Assertions.assertEquals(ConsumerType.TAG, consumer2.getConsumerType());
        Assertions.assertEquals(FlowStage.API, flow.getStage());

        Step rule = flow.getPre().get(0);
        Assertions.assertNotNull(rule);
        Assertions.assertEquals("Rate Limit", rule.getName());
        Assertions.assertEquals("rate-limit", rule.getPolicy());
        Assertions.assertNotNull(rule.getConfiguration());
        Assertions.assertTrue(rule.isEnabled());
        Assertions.assertNull(rule.getCondition());

        Step ruleApiKey = flow.getPre().get(1);
        Assertions.assertNotNull(ruleApiKey);
        Assertions.assertEquals("Check API Key", ruleApiKey.getName());
        Assertions.assertEquals("api-key", ruleApiKey.getPolicy());
        Assertions.assertNotNull(ruleApiKey.getConfiguration());
        Assertions.assertTrue(ruleApiKey.isEnabled());
        Assertions.assertNull(ruleApiKey.getCondition());

        Step ruleTransformHeaders = flow.getPre().get(2);
        Assertions.assertNotNull(ruleTransformHeaders);
        Assertions.assertEquals("Add HTTP headers", ruleTransformHeaders.getName());
        Assertions.assertEquals("transform-headers", ruleTransformHeaders.getPolicy());
        Assertions.assertNotNull(ruleTransformHeaders.getConfiguration());
        Assertions.assertTrue(ruleTransformHeaders.isEnabled());
        Assertions.assertEquals("a non empty condition", ruleTransformHeaders.getCondition());

        Collection<Plan> plans = api.getPlans();
        Assertions.assertNotNull(plans);
        Assertions.assertEquals(2, plans.size());
        Assertions.assertNotNull(api.getPlan("plan-1"));
        Assertions.assertEquals(2, api.getPlan("plan-1").getFlows().size());
        Assertions.assertEquals("#context.attributes['jwt'].claims['iss'] == 'toto'", api.getPlan("plan-1").getSelectionRule());
        Assertions.assertEquals("OAUTH2", api.getPlan("plan-1").getSecurity());
        Assertions.assertEquals(2, api.getPlan("plan-1").getTags().size());
        Assertions.assertEquals("PUBLISHED", api.getPlan("plan-1").getStatus());
        Assertions.assertEquals(FlowStage.PLAN, api.getPlan("plan-1").getFlows().get(0).getStage());

        Assertions.assertEquals(FlowMode.DEFAULT, api.getFlowMode());
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

        Assertions.assertEquals(api.getExecutionMode(), ExecutionMode.V3);
    }

    @Test
    public void shouldDefinitionExecutionModeEqualV3WhenJsonContainsV3() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-executionmode-v3.json", Api.class);

        Assertions.assertEquals(ExecutionMode.V3, api.getExecutionMode());
    }

    @Test
    public void shouldDefinitionExecutionModeEqualV4EmulationEngineWhenJsonContainsV4EmulationEngine() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-executionmode-v4-emulation-engine.json", Api.class);

        Assertions.assertEquals(ExecutionMode.V4_EMULATION_ENGINE, api.getExecutionMode());
    }

    @Test
    public void shouldDefinitionExecutionModeEqualV4EmulationEngineWhenJsonContainsJupiter() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-executionmode-jupiter.json", Api.class);

        Assertions.assertEquals(ExecutionMode.V4_EMULATION_ENGINE, api.getExecutionMode());
    }

    @Test
    public void definition_withclientoptions_propagateClientAcceptEncoding() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-propagateClientAcceptEncoding.json", Api.class);

        Endpoint endpoint = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
        JsonNode endpointConfiguration = objectMapper().readTree(endpoint.getConfiguration());
        Assertions.assertTrue(endpointConfiguration.has("http"));
        Assertions.assertEquals(
            endpointConfiguration.get("http").toString(),
            "{\"useCompression\":false,\"propagateClientAcceptEncoding\":true}"
        );
    }

    @Test
    public void shouldHaveADefinitionContext() throws IOException {
        Api api = load("/io/gravitee/definition/jackson/api-with-definition-context.json", Api.class);
        DefinitionContext definitionContext = api.getDefinitionContext();
        Assertions.assertNotNull(definitionContext);
        Assertions.assertEquals(DefinitionContext.ORIGIN_KUBERNETES, definitionContext.getOrigin());
        Assertions.assertEquals(DefinitionContext.MODE_FULLY_MANAGED, definitionContext.getMode());
    }

    @Test
    public void shouldSetPlanApiFromApiId() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-plan-without-apiId.json", Api.class);

        Assertions.assertEquals(1, api.getPlans().size());
        Assertions.assertEquals("my-api-id", api.getPlans().get(0).getApi());
    }
}
