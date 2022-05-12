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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSerializerTest extends AbstractTest {

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("/io/gravitee/definition/jackson/api-defaulthttpconfig-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-bestMatchFlowMode-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-overridedhttpconfig-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-nopath-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-defaultpath-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-multiplepath-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-path-nohttpmethod-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withoutpolicy-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withoutproperties-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withemptyproperties-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withproperties-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withclientoptions-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withclientoptions-nossl-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withclientoptions-nooptions-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-defaulthttpconfig-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-default-failover-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-override-failover-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-failover-singlecase-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-hostHeader-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-cors-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-multitenants-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-logging-client-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withclientoptions-truststore-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withclientoptions-keystore-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-endpointgroup-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-response-templates-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-virtualhosts-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-http2-endpoint-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-grpc-endpoint-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-grpc-endpoint-ssl-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-grpc-endpoint-without-type-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-defaultflow-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-kafka-endpoint-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-default-executionmode-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-executionmode-v3-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-executionmode-jupiter-expected.json")
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    public void shouldWriteValidJson(final String json) throws IOException {
        JsonNode jsonNode = loadJson(json);
        String generatedJsonDefinition = objectMapper().writeValueAsString(jsonNode);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(json));

        assertNotNull(generatedJsonDefinition);
        assertEquals(
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
