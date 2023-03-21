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

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.jackson.datatype.DeploymentRequiredMapper;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DeploymentRequiredApiSerializerTest extends AbstractTest {

    private final DeploymentRequiredMapper deploymentRequiredMapper = new DeploymentRequiredMapper();

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
            Arguments.of("/io/gravitee/definition/jackson/api-executionmode-jupiter-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/api-withclientoptions-propagateClientAcceptEncoding.json")
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    public void shouldWriteValidJson(final String json) throws IOException {
        DeploymentRequiredApiEntity deploymentRequiredApiEntity = load(json, DeploymentRequiredApiEntity.class);
        String generatedJsonDefinition = deploymentRequiredMapper.writeValueAsString(deploymentRequiredApiEntity);
        JsonNode jsonNode = objectMapper().readTree(generatedJsonDefinition);
        assertNotNull(generatedJsonDefinition);

        assertNotNull(jsonNode);
        assertFalse(jsonNode.has("id"));
        assertTrue(jsonNode.has("proxy"));
    }

    @JsonFilter("deploymentRequiredFilter")
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class DeploymentRequiredApiEntity {

        @EqualsAndHashCode.Include
        private String id;

        private String crossId;

        private String name;

        @EqualsAndHashCode.Include
        private String version;

        private String description;

        @DeploymentRequired
        @JsonProperty(value = "execution_mode")
        private ExecutionMode executionMode;

        private Set<String> groups;

        @JsonProperty(value = "context_path")
        private String contextPath;

        @NotNull
        @DeploymentRequired
        @JsonProperty(value = "proxy", required = true)
        private Proxy proxy;

        @DeploymentRequired
        @JsonProperty(value = "flow_mode")
        private FlowMode flowMode;

        @DeploymentRequired
        @JsonProperty(value = "flows")
        private List<Flow> flows = new ArrayList<>();

        @DeploymentRequired
        @JsonProperty(value = "gravitee")
        private String graviteeDefinitionVersion;

        @JsonProperty(value = "definition_context")
        private DefinitionContext definitionContext;

        @JsonProperty("deployed_at")
        private Date deployedAt;

        @JsonProperty("created_at")
        private Date createdAt;

        @JsonProperty("updated_at")
        private Date updatedAt;

        private Lifecycle.State state;

        @DeploymentRequired
        @JsonProperty(value = "properties")
        private io.gravitee.definition.model.Properties properties;

        @DeploymentRequired
        @JsonProperty(value = "services")
        private Services services;

        @DeploymentRequired
        private Set<String> tags;

        private String picture;

        @JsonProperty(value = "picture_url")
        private String pictureUrl;

        @DeploymentRequired
        @JsonProperty(value = "resources")
        private List<Resource> resources = new ArrayList<>();

        private Set<String> categories;

        private List<String> labels;

        @DeploymentRequired
        @JsonProperty(value = "path_mappings")
        private Set<String> pathMappings = new HashSet<>();

        @JsonIgnore
        private Map<String, Object> metadata = new HashMap<>();

        @DeploymentRequired
        @JsonProperty(value = "response_templates")
        private Map<String, Map<String, ResponseTemplate>> responseTemplates;

        @JsonProperty("disable_membership_notifications")
        private boolean disableMembershipNotifications;

        private String background;

        @JsonProperty(value = "background_url")
        private String backgroundUrl;

        @JsonIgnore
        private String referenceType;

        @JsonIgnore
        private String referenceId;

        @JsonIgnore
        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        @JsonGetter("properties")
        public List<Property> getPropertyList() {
            if (properties != null) {
                return properties.getProperties();
            }
            return Collections.emptyList();
        }

        @JsonSetter("properties")
        public void setPropertyList(List<Property> properties) {
            this.properties = new Properties();
            this.properties.setProperties(properties);
        }

        @JsonIgnore
        public String getApiVersion() {
            return version;
        }

        @JsonIgnore
        public DefinitionVersion getDefinitionVersion() {
            if (graviteeDefinitionVersion != null) {
                return DefinitionVersion.valueOfLabel(graviteeDefinitionVersion);
            }
            return null;
        }
    }
}
