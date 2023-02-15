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
package io.gravitee.definition.model.kubernetes.v1alpha1;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URL;
import org.junit.Test;

public class ApiDefinitionResourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void shouldRemoveUnsupportedFields() throws Exception {
        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        assertFalse(resource.getSpec().has("createdAt"));
        assertFalse(resource.getSpec().has("definition_context"));
        assertFalse(resource.getSpec().has("execution_context"));
        assertFalse(resource.getSpec().has("primaryOwner"));
        assertFalse(resource.getSpec().has("pages"));
        assertFalse(resource.getSpec().has("members"));
        assertFalse(resource.getSpec().has("picture"));
        assertFalse(resource.getSpec().has("apiMedia"));

        ArrayNode plans = (ArrayNode) resource.getSpec().get("plans");

        JsonNode plan = plans.iterator().next();
        assertFalse(plan.has("created_at"));
        assertFalse(plan.has("updated_at"));
        assertFalse(resource.getSpec().has("published_at"));

        ArrayNode metadata = (ArrayNode) resource.getSpec().get("metadata");

        JsonNode meta = metadata.iterator().next();
        assertFalse(meta.has("apiId"));
    }

    @Test
    public void shouldRemoveIds() throws Exception {
        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.removeIds();

        assertFalse(resource.getSpec().has("id"));
        assertFalse(resource.getSpec().has("crossId"));

        ArrayNode plans = (ArrayNode) resource.getSpec().get("plans");

        JsonNode plan = plans.iterator().next();

        assertFalse(plan.has("id"));
        assertFalse(plan.has("crossId"));
        assertFalse(plan.has("api"));
    }

    @Test
    public void ShouldSetContextRef() throws Exception {
        String contextRefName = "apim-dev-ctx";
        String contextRefNamespace = "default";

        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.setContextRef(contextRefName, contextRefNamespace);

        assertTrue(resource.getSpec().has("contextRef"));

        ObjectNode contextRef = ((ObjectNode) resource.getSpec().get("contextRef"));

        assertTrue(contextRef.has("name"));
        assertTrue(contextRef.has("namespace"));
        assertEquals(contextRefName, contextRef.get("name").asText());
        assertEquals(contextRefNamespace, contextRef.get("namespace").asText());
    }

    @Test
    public void ShouldSetContextPath() throws Exception {
        String contextPath = "/new-context-path";

        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.setContextPath(contextPath);

        assertTrue(resource.getSpec().has("proxy"));

        ObjectNode proxy = (ObjectNode) resource.getSpec().get("proxy");

        assertTrue(proxy.has("virtual_hosts"));

        ArrayNode virtualHosts = (ArrayNode) proxy.get("virtual_hosts");
        ObjectNode virtualHost = (ObjectNode) virtualHosts.iterator().next();

        assertEquals(contextPath, virtualHost.get("path").asText());
    }

    @Test
    public void ShouldSetVersion() throws Exception {
        String version = "1.0.0-alpha";

        ApiDefinitionResource resource = new ApiDefinitionResource("api-definition", readDefinition());

        resource.setVersion(version);

        assertTrue(resource.getSpec().has("version"));
        assertEquals(version, resource.getSpec().get("version").asText());
    }

    private ObjectNode readDefinition() throws Exception {
        String path = "io/gravitee/definition/model/kubernetes/v1alpha1/api-definition.json";
        URL resource = getClass().getClassLoader().getResource(path);
        return (ObjectNode) MAPPER.readTree(resource);
    }
}
