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

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSerializerTest extends AbstractTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaulthttpconfig-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);

        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_bestMatchFlowMode() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-bestMatchFlowMode.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-bestMatchFlowMode-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertTrue(generatedJsonDefinition.contains("BEST_MATCH"));
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-overridedhttpconfig.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-overridedhttpconfig-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_noPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-nopath.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-nopath-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_defaultPath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaultpath-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_multiplePath() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multiplepath.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-multiplepath-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaultpath-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-nohttpmethod.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-path-nohttpmethod-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaultpath-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultpath.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaultpath-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-path-withoutpolicy.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withoutpolicy-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_apiWithoutProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withoutproperties.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withoutproperties-expected.json";

        Assert.assertNull(api.getProperties());

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );

        JsonNode node = objectMapper().readTree(generatedJsonDefinition.getBytes());
        Assert.assertNotNull(node.get("properties"));
    }

    @Test
    public void definition_apiWithEmptyProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withemptyproperties.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withemptyproperties-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_apiWithProperties() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withproperties.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withproperties-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withclientoptions() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withclientoptions-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withclientoptions_nossl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-nossl.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withclientoptions-nossl-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withclientoptions_nooptions() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-nooptions.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withclientoptions-nooptions-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withclientoptions_nooptions_defaultconfiguration() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-nooptions.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withclientoptions-nooptions-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_defaultLoadBalancer_roundRobin() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaulthttpconfig-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_no_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaulthttpconfig.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaulthttpconfig-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_default_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-default-failover.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-default-failover-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_override_failover() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-override-failover.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-override-failover-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_failover_singlecase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-failover-singlecase.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-failover-singlecase-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_hostHeader_empty() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-empty-hostHeader.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-empty-hostHeader-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_hostHeader() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-hostHeader.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-hostHeader-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_noCors() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-hostHeader.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-hostHeader-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withCors_defaultValues() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-cors.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-cors-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_multiTenants_enable() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-multitenants.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-multitenants-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withLogging_clientMode() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-logging-client.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-logging-client-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withclientoptions_truststore() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-truststore.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withclientoptions-truststore-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withclientoptions_keystore() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withclientoptions-keystore.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-withclientoptions-keystore-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withHeaders() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-headers.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-headers-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withEndpointGroup() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-endpointgroup.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-endpointgroup-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_withResponseTemplates() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-response-templates.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-response-templates-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_virtualhosts() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-virtualhosts.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-virtualhosts-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_http2_endpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-http2-endpoint.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-http2-endpoint-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_grpc_endpoint() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-grpc-endpoint-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_grpc_endpoint_ssl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint-ssl.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-grpc-endpoint-ssl-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_grpc_endpoint_without_type() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-grpc-endpoint-without-type.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-grpc-endpoint-without-type-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void definition_defaultflow() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-defaultflow.json", Api.class);
        String expectedDefinition = "/io/gravitee/definition/jackson/api-defaultflow-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        Assert.assertNotNull(generatedJsonDefinition);
        assertTrue(generatedJsonDefinition.contains("\"gravitee\" : \"2.0.0\","));
        Assert.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void testSerializeRule() throws Exception {
        Rule rule = new Rule();
        Policy policy = new Policy();
        policy.setName("test");
        policy.setConfiguration("{\"foo\":\"bar\"}");
        rule.setPolicy(policy);
        String generatedJsonDefinition = objectMapper().writeValueAsString(rule);
        JSONAssert.assertEquals(
            "{\n" +
            "  \"methods\" : [ \"CONNECT\", \"DELETE\", \"GET\", \"HEAD\", \"OPTIONS\", \"PATCH\", \"POST\", \"PUT\", \"TRACE\", \"OTHER\" ],\n" +
            "  \"enabled\" : true,\n" +
            "  \"test\" : {\n" +
            "     \"foo\":\"bar\"\n" +
            "  }\n" +
            "}",
            generatedJsonDefinition,
            JSONCompareMode.STRICT
        );
    }
}
