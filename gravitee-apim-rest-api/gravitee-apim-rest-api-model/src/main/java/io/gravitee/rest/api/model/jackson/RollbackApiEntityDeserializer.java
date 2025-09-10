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
package io.gravitee.rest.api.model.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.RollbackApiEntity;
import java.io.IOException;
import java.util.*;

/**
 * Custom deserializer for RollbackApiEntity that:
 * 1. Checks if the payload is a v4 API definition and rejects it early.
 * 2. Otherwise, deserializes the JSON into a temporary RollbackApiEntityRaw
 *    (a plain DTO without @JsonDeserialize, to avoid recursion),
 *    and then maps it to the proper RollbackApiEntity.
 */
public class RollbackApiEntityDeserializer extends StdDeserializer<RollbackApiEntity> {

    public RollbackApiEntityDeserializer() {
        super(RollbackApiEntity.class);
    }

    @Override
    public RollbackApiEntity deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        final ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        final JsonNode node = mapper.readTree(jp);

        if (isV4(text(node.get("definitionVersion")))) {
            throw JsonMappingException.from(ctxt, "Detected a v4 API definition. Please use the migration tool instead of rollback.");
        }

        final RollbackApiEntityRaw raw = mapper.treeToValue(node, RollbackApiEntityRaw.class);

        return toEntity(raw);
    }

    private static RollbackApiEntity toEntity(RollbackApiEntityRaw p) {
        final RollbackApiEntity out = new RollbackApiEntity();
        out.setId(p.id);
        out.setName(p.name);
        out.setVersion(p.version);
        out.setDescription(p.description);

        out.setProxy(p.proxy);

        out.setPaths(nullableOr(p.paths, new HashMap<>()));
        out.setFlows(nullableOr(p.flows, new ArrayList<>()));
        out.setPlans(nullableOr(p.plans, new ArrayList<>()));

        out.setServices(p.services);
        out.setResources(nullableOr(p.resources, new ArrayList<>()));

        if (p.propertiesList != null) {
            Properties props = new Properties();
            props.setProperties(p.propertiesList);
            out.setProperties(props);
        }

        out.setVisibility(p.visibility);
        out.setTags(p.tags);
        out.setPicture(p.picture);

        out.setGraviteeDefinitionVersion(p.graviteeDefinitionVersion);
        out.setFlowMode(p.flowMode);
        out.setPictureUrl(p.pictureUrl);

        out.setCategories(p.categories);
        out.setLabels(p.labels);
        out.setGroups(p.groups);
        out.setPathMappings(p.pathMappings);

        out.setExecutionMode(p.executionMode);
        out.setResponseTemplates(p.responseTemplates);

        out.setMetadata(p.metadata);
        out.setLifecycleState(p.lifecycleState);
        out.setDisableMembershipNotifications(p.disableMembershipNotifications);

        out.setBackground(p.background);
        out.setBackgroundUrl(p.backgroundUrl);

        return out;
    }

    private static String text(JsonNode n) {
        return (n != null && n.isTextual()) ? n.asText() : null;
    }

    private static boolean isV4(String v) {
        if (v == null) return false;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (Character.isDigit(c)) return c == '4';
        }
        return false;
    }

    private static <T> T nullableOr(T value, T fallback) {
        return value != null ? value : fallback;
    }

    /**
     * Raw DTO version of RollbackApiEntity.
     * Purpose: avoid infinite recursion caused by @JsonDeserialize on the main class.
     * It has the same fields but no custom annotations, so Jackson can map JSON into it safely.
     */

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RollbackApiEntityRaw {

        public String id;
        public String name;
        public String version;
        public String description;

        public Proxy proxy;
        public Map<String, List<Rule>> paths;
        public List<Flow> flows;
        public List<Plan> plans;

        public Services services;
        public List<Resource> resources;

        @JsonProperty("properties")
        public List<Property> propertiesList;

        public Visibility visibility;
        public Set<String> tags;
        public String picture;

        @JsonProperty("gravitee")
        public String graviteeDefinitionVersion;

        @JsonProperty("flow_mode")
        public FlowMode flowMode;

        @JsonProperty("picture_url")
        public String pictureUrl;

        @JsonProperty("categories")
        public Set<String> categories;

        public List<String> labels;
        public Set<String> groups;

        @JsonProperty("path_mappings")
        public Set<String> pathMappings;

        @JsonProperty("execution_mode")
        public ExecutionMode executionMode;

        @JsonProperty("response_templates")
        public Map<String, Map<String, ResponseTemplate>> responseTemplates;

        public List<ApiMetadataEntity> metadata;

        @JsonProperty("lifecycle_state")
        public ApiLifecycleState lifecycleState;

        @JsonProperty("disable_membership_notifications")
        public boolean disableMembershipNotifications;

        public String background;

        @JsonProperty("background_url")
        public String backgroundUrl;
    }
}
