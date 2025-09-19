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

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.PolicyScope;
import io.gravitee.definition.model.debug.DebugApiV2;
import io.gravitee.definition.model.debug.DebugStep;
import io.gravitee.definition.model.debug.DebugStepStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DebugApiV2DeserializerTest extends AbstractTest {

    @Test
    public void debugApi_withRequest() throws Exception {
        DebugApiV2 debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-request.json", DebugApiV2.class);

        Assertions.assertEquals(debugApi.getRequest().getPath(), "/");
        Assertions.assertEquals(debugApi.getRequest().getMethod(), "GET");
        Assertions.assertEquals(debugApi.getRequest().getBody(), "request-body");
        Assertions.assertEquals(debugApi.getRequest().getHeaders().size(), 4);
        Assertions.assertEquals(debugApi.getRequest().getHeaders().get("X-Gravitee-Transaction-Id"), List.of("transaction-id"));
        Assertions.assertEquals(debugApi.getRequest().getHeaders().get("content-type"), List.of("application/json"));
        Assertions.assertEquals(debugApi.getRequest().getHeaders().get("X-Gravitee-Request-Id"), List.of("request-id"));
        Assertions.assertEquals(debugApi.getRequest().getHeaders().get("accept-encoding"), List.of("deflate", "gzip", "compress"));
    }

    @Test
    public void debugApi_withResponse() throws Exception {
        DebugApiV2 debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-response.json", DebugApiV2.class);

        Assertions.assertEquals(debugApi.getResponse().getStatusCode(), 200);
        Assertions.assertEquals(debugApi.getResponse().getBody(), "response-body");
        Assertions.assertEquals(debugApi.getResponse().getHeaders().size(), 2);
        Assertions.assertEquals(debugApi.getResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        Assertions.assertEquals(debugApi.getResponse().getHeaders().get("accept-encoding"), List.of("deflate", "gzip", "compress"));
    }

    @Test
    public void debugApi_withDebugSteps() throws Exception {
        DebugApiV2 debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps.json", DebugApiV2.class);

        Assertions.assertEquals(debugApi.getDebugSteps().size(), 10);
        Assertions.assertEquals(
            debugApi.getPreprocessorStep().getAttributes(),
            Map.of("gravitee.attribute.application", "1", "gravitee.attribute.user-id", "127.0.0.1")
        );
        final Map<String, List<String>> preprocessorHeaders = debugApi.getPreprocessorStep().getHeaders();
        Assertions.assertEquals(preprocessorHeaders.size(), 2);

        Assertions.assertEquals(preprocessorHeaders.get("X-Gravitee-Transaction-Id"), List.of("e467b739-f921-4b9e-a7b7-39f921fb9ee9"));
        Assertions.assertEquals(preprocessorHeaders.get("X-Gravitee-Request-Id"), List.of("e467b739-f921-4b9e-a7b7-39f921fb9ee9"));

        DebugStep step1 = debugApi.getDebugSteps().get(0);
        Assertions.assertEquals(step1.getPolicyInstanceId(), "24b22176-e4fd-488e-b221-76e4fd388e30");
        Assertions.assertEquals(step1.getPolicyId(), "key-less");
        Assertions.assertEquals(step1.getDuration().longValue(), 1102529);
        Assertions.assertEquals(step1.getStatus(), DebugStepStatus.COMPLETED);
        Assertions.assertNull(step1.getCondition());
        Assertions.assertEquals(step1.getScope(), PolicyScope.ON_REQUEST);
        Assertions.assertEquals(step1.getResult().size(), 1);
        Assertions.assertEquals(step1.getStage(), "SECURITY");

        final Map<String, Object> attributes = (Map<String, Object>) step1.getResult().get("attributes");
        Assertions.assertEquals(attributes.size(), 6);

        Assertions.assertEquals(attributes.get("gravitee.attribute.application"), "1");
        Assertions.assertEquals(attributes.get("gravitee.attribute.api.deployed-at"), "1644242411908");
        Assertions.assertEquals(attributes.get("gravitee.attribute.user-id"), "127.0.0.1");
        Assertions.assertEquals(attributes.get("gravitee.attribute.plan"), "7bc7c418-056b-4876-87c4-18056b08763d");
        Assertions.assertEquals(attributes.get("gravitee.attribute.api"), "62710ef2-83bc-4007-b10e-f283bce00763");
        Assertions.assertEquals(attributes.get("gravitee.attribute.gravitee.attribute.plan.selection.rule.based"), "false");

        DebugStep step2 = debugApi.getDebugSteps().get(1);
        Assertions.assertEquals(step2.getPolicyInstanceId(), "23ab1ad0-bff0-43a4-ab1a-d0bff013a41e");
        Assertions.assertEquals(step2.getPolicyId(), "transform-headers");
        Assertions.assertEquals(step2.getDuration().longValue(), 3123247);
        Assertions.assertEquals(step2.getStatus(), DebugStepStatus.COMPLETED);
        Assertions.assertNull(step2.getCondition());
        Assertions.assertEquals(step2.getScope(), PolicyScope.ON_REQUEST);
        Assertions.assertEquals(step2.getResult().size(), 1);
        Assertions.assertEquals(step2.getStage(), "PLAN");

        final Map<String, String> headers = (Map<String, String>) step2.getResult().get("headers");
        Assertions.assertEquals(headers.size(), 5);

        Assertions.assertEquals(headers.get("transfer-encoding"), List.of("chunked"));
        Assertions.assertEquals(headers.get("host"), List.of("localhost:8482"));
        Assertions.assertEquals(headers.get("X-Gravitee-Transaction-Id"), List.of("e467b739-f921-4b9e-a7b7-39f921fb9ee9"));
        Assertions.assertEquals(headers.get("firstpolicy"), List.of("firstvalue", "secondvalue"));
        Assertions.assertEquals(headers.get("X-Gravitee-Request-Id"), List.of("e467b739-f921-4b9e-a7b7-39f921fb9ee9"));

        DebugStep skippedStep = debugApi.getDebugSteps().get(3);
        Assertions.assertEquals(skippedStep.getStatus(), DebugStepStatus.SKIPPED);
        Assertions.assertEquals(skippedStep.getCondition(), "{#request.headers.name == \"joe\"}");
    }

    @Test
    public void debugApi_withDebugStepsError() throws Exception {
        DebugApiV2 debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps-error.json", DebugApiV2.class);

        Assertions.assertEquals(debugApi.getDebugSteps().size(), 2);
        DebugStep errorStep = debugApi.getDebugSteps().get(1);
        Assertions.assertEquals(errorStep.getStatus(), DebugStepStatus.ERROR);
        Assertions.assertEquals(errorStep.getError().getMessage(), "Error message");
        Assertions.assertEquals(errorStep.getError().getStatus(), 400);
        Assertions.assertEquals(errorStep.getError().getKey(), "POLICY_ERROR");
        Assertions.assertEquals(errorStep.getError().getContentType(), "aplication/json");
    }

    @Test
    public void debugApi_withBackendResponse() throws Exception {
        DebugApiV2 debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-backend-response.json", DebugApiV2.class);

        Assertions.assertEquals(debugApi.getBackendResponse().getStatusCode(), 200);
        Assertions.assertEquals(debugApi.getBackendResponse().getBody(), "{\"message\": \"mock backend response\"}");
        Assertions.assertEquals(debugApi.getBackendResponse().getHeaders().size(), 4);
        Assertions.assertEquals(debugApi.getBackendResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        Assertions.assertEquals(debugApi.getBackendResponse().getHeaders().get("content-type"), List.of("application/json"));
        Assertions.assertEquals(debugApi.getBackendResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        Assertions.assertEquals(debugApi.getBackendResponse().getHeaders().get("content-length"), List.of("42"));
    }

    @Test
    public void debugApi_withoutRequest() throws Exception {
        assertThrows(JsonMappingException.class, () ->
            load("/io/gravitee/definition/jackson/debug/debug-api-without-request.json", DebugApiV2.class)
        );
    }
}
