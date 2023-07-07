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
package io.gravitee.definition.model.kubernetes.v1alpha1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiDefinitionResourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldRemoveUnsupportedFields() throws Exception {
        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        Assertions.assertFalse(resource.getSpec().has("createdAt"));
        Assertions.assertFalse(resource.getSpec().has("definition_context"));
        Assertions.assertFalse(resource.getSpec().has("execution_context"));
        Assertions.assertFalse(resource.getSpec().has("primaryOwner"));
        Assertions.assertFalse(resource.getSpec().has("pages"));
        Assertions.assertFalse(resource.getSpec().has("members"));
        Assertions.assertFalse(resource.getSpec().has("picture"));
        Assertions.assertFalse(resource.getSpec().has("apiMedia"));

        ArrayNode plans = (ArrayNode) resource.getSpec().get("plans");

        JsonNode plan = plans.iterator().next();
        Assertions.assertFalse(plan.has("created_at"));
        Assertions.assertFalse(plan.has("updated_at"));
        Assertions.assertFalse(resource.getSpec().has("published_at"));

        ArrayNode metadata = (ArrayNode) resource.getSpec().get("metadata");

        JsonNode meta = metadata.iterator().next();
        Assertions.assertFalse(meta.has("apiId"));
    }

    @Test
    void shouldRemoveIds() throws Exception {
        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.removeIds();

        Assertions.assertFalse(resource.getSpec().has("id"));
        Assertions.assertFalse(resource.getSpec().has("crossId"));

        ArrayNode plans = (ArrayNode) resource.getSpec().get("plans");

        JsonNode plan = plans.iterator().next();

        Assertions.assertFalse(plan.has("id"));
        Assertions.assertFalse(plan.has("crossId"));
        Assertions.assertFalse(plan.has("api"));
    }

    @Test
    void shouldSetContextRef() throws Exception {
        String contextRefName = "apim-dev-ctx";
        String contextRefNamespace = "default";

        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.setContextRef(contextRefName, contextRefNamespace);

        Assertions.assertTrue(resource.getSpec().has("contextRef"));

        ObjectNode contextRef = ((ObjectNode) resource.getSpec().get("contextRef"));

        Assertions.assertTrue(contextRef.has("name"));
        Assertions.assertTrue(contextRef.has("namespace"));
        Assertions.assertEquals(contextRefName, contextRef.get("name").asText());
        Assertions.assertEquals(contextRefNamespace, contextRef.get("namespace").asText());
    }

    @Test
    void shouldSetContextPath() throws Exception {
        String contextPath = "/new-context-path";

        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.setContextPath(contextPath);

        Assertions.assertTrue(resource.getSpec().has("proxy"));

        ObjectNode proxy = (ObjectNode) resource.getSpec().get("proxy");

        Assertions.assertTrue(proxy.has("virtual_hosts"));

        ArrayNode virtualHosts = (ArrayNode) proxy.get("virtual_hosts");
        ObjectNode virtualHost = (ObjectNode) virtualHosts.iterator().next();

        Assertions.assertEquals(contextPath, virtualHost.get("path").asText());
    }

    @Test
    public void shouldSetVersion() throws Exception {
        String version = "1.0.0-alpha";

        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.setVersion(version);

        Assertions.assertTrue(resource.getSpec().has("version"));
        Assertions.assertEquals(version, resource.getSpec().get("version").asText());
    }

    private ObjectNode readDefinition() throws Exception {
        String path = "io/gravitee/definition/model/kubernetes/v1alpha1/api-definition.json";
        URL resource = getClass().getClassLoader().getResource(path);
        return (ObjectNode) MAPPER.readTree(resource);
    }
}
