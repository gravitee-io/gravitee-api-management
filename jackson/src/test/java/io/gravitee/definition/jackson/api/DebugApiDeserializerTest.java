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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.PolicyScope;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.definition.model.debug.DebugStep;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DebugApiDeserializerTest extends AbstractTest {

    @Test
    public void debugApi_withRequest() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-request.json", DebugApi.class);

        assertEquals(debugApi.getRequest().getPath(), "/");
        assertEquals(debugApi.getRequest().getMethod(), "GET");
        assertEquals(debugApi.getRequest().getBody(), "request-body");
        assertEquals(debugApi.getRequest().getHeaders().size(), 4);
        assertEquals(debugApi.getRequest().getHeaders().get("X-Gravitee-Transaction-Id"), List.of("transaction-id"));
        assertEquals(debugApi.getRequest().getHeaders().get("content-type"), List.of("application/json"));
        assertEquals(debugApi.getRequest().getHeaders().get("X-Gravitee-Request-Id"), List.of("request-id"));
        assertEquals(debugApi.getRequest().getHeaders().get("accept-encoding"), List.of("deflate", "gzip", "compress"));
    }

    @Test
    public void debugApi_withResponse() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-response.json", DebugApi.class);

        assertEquals(debugApi.getResponse().getStatusCode(), 200);
        assertEquals(debugApi.getResponse().getBody(), "response-body");
        assertEquals(debugApi.getResponse().getHeaders().size(), 2);
        assertEquals(debugApi.getResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        assertEquals(debugApi.getResponse().getHeaders().get("accept-encoding"), List.of("deflate", "gzip", "compress"));
    }

    @Test
    public void debugApi_withDebugSteps() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps.json", DebugApi.class);

        assertEquals(debugApi.getDebugSteps().size(), 9);
        assertEquals(
            debugApi.getInitialAttributes(),
            Map.of("gravitee.attribute.application", "1", "gravitee.attribute.user-id", "127.0.0.1")
        );

        DebugStep step1 = debugApi.getDebugSteps().get(0);
        assertEquals(step1.getPolicyInstanceId(), "24b22176-e4fd-488e-b221-76e4fd388e30");
        assertEquals(step1.getPolicyId(), "key-less");
        assertEquals(step1.getDuration().longValue(), 1102529);
        assertEquals(step1.getStatus(), DebugStep.Status.COMPLETED);
        assertEquals(step1.getScope(), PolicyScope.ON_REQUEST);
        assertEquals(step1.getResult().size(), 1);

        final Map<String, Object> attributes = (Map<String, Object>) step1.getResult().get("attributes");
        assertEquals(attributes.size(), 6);

        assertEquals(attributes.get("gravitee.attribute.application"), "1");
        assertEquals(attributes.get("gravitee.attribute.api.deployed-at"), "1644242411908");
        assertEquals(attributes.get("gravitee.attribute.user-id"), "127.0.0.1");
        assertEquals(attributes.get("gravitee.attribute.plan"), "7bc7c418-056b-4876-87c4-18056b08763d");
        assertEquals(attributes.get("gravitee.attribute.api"), "62710ef2-83bc-4007-b10e-f283bce00763");
        assertEquals(attributes.get("gravitee.attribute.gravitee.attribute.plan.selection.rule.based"), "false");

        DebugStep step2 = debugApi.getDebugSteps().get(1);
        assertEquals(step2.getPolicyInstanceId(), "23ab1ad0-bff0-43a4-ab1a-d0bff013a41e");
        assertEquals(step2.getPolicyId(), "transform-headers");
        assertEquals(step2.getDuration().longValue(), 3123247);
        assertEquals(step2.getStatus(), DebugStep.Status.COMPLETED);
        assertEquals(step2.getScope(), PolicyScope.ON_REQUEST);
        assertEquals(step2.getResult().size(), 1);

        final Map<String, String> headers = (Map<String, String>) step2.getResult().get("headers");
        assertEquals(headers.size(), 5);

        assertEquals(headers.get("transfer-encoding"), List.of("chunked"));
        assertEquals(headers.get("host"), List.of("localhost:8482"));
        assertEquals(headers.get("X-Gravitee-Transaction-Id"), List.of("e467b739-f921-4b9e-a7b7-39f921fb9ee9"));
        assertEquals(headers.get("firstpolicy"), List.of("firstvalue", "secondvalue"));
        assertEquals(headers.get("X-Gravitee-Request-Id"), List.of("e467b739-f921-4b9e-a7b7-39f921fb9ee9"));
    }

    @Test
    public void debugApi_withBackendResponse() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-backend-response.json", DebugApi.class);

        assertEquals(debugApi.getBackendResponse().getStatusCode(), 200);
        assertEquals(debugApi.getBackendResponse().getBody(), "{\"message\": \"mock backend response\"}");
        assertEquals(debugApi.getBackendResponse().getHeaders().size(), 4);
        assertEquals(debugApi.getBackendResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        assertEquals(debugApi.getBackendResponse().getHeaders().get("content-type"), List.of("application/json"));
        assertEquals(debugApi.getBackendResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        assertEquals(debugApi.getBackendResponse().getHeaders().get("content-length"), List.of("42"));
    }

    @Test(expected = JsonMappingException.class)
    public void debugApi_withoutRequest() throws Exception {
        load("/io/gravitee/definition/jackson/debug/debug-api-without-request.json", DebugApi.class);
    }
}
