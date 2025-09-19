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

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.definition.model.Property;
import io.gravitee.rest.api.model.api.RollbackApiEntity;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RollbackApiEntityDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(RollbackApiEntity.class, new RollbackApiEntityDeserializer());
        mapper.registerModule(m);
    }

    @Test
    void should_deserialize_v2_payload_and_map_gravitee_and_snake_case_fields() throws Exception {
        String json = """
            {
              "id": "api-123",
              "name": "Test API",
              "version": "1.0.0",
              "description": "desc",
              "gravitee": "2.0.0",
              "flow_mode": "DEFAULT",
              "execution_mode": "v4-emulation-engine",
              "picture_url": "http://example/logo.png",
              "path_mappings": ["/a/**"],
              "categories": ["cat1","cat2"],
              "labels": ["l1","l2"],
              "groups": ["g1","g2"],
              "lifecycle_state": "CREATED",
              "disable_membership_notifications": true,
              "background_url": "http://example/bg.png",
              "response_templates": { "DEFAULT": { "en": { "status": 418 } } },
              "properties": [
                { "key": "k1", "value": "v1" },
                { "key": "k2", "value": "v2" }
              ]
            }
            """;

        RollbackApiEntity e = mapper.readValue(json, RollbackApiEntity.class);

        assertThat(e.getId()).isEqualTo("api-123");
        assertThat(e.getName()).isEqualTo("Test API");
        assertThat(e.getVersion()).isEqualTo("1.0.0");
        assertThat(e.getDescription()).isEqualTo("desc");

        assertThat(e.getGraviteeDefinitionVersion()).isEqualTo("2.0.0");

        assertThat(e.getFlowMode()).hasToString("DEFAULT");
        assertThat(e.getExecutionMode().name()).isEqualTo("V4_EMULATION_ENGINE");
        assertThat(e.getPictureUrl()).isEqualTo("http://example/logo.png");
        assertThat(e.getPathMappings()).containsExactly("/a/**");
        assertThat(e.getCategories()).containsExactlyInAnyOrder("cat1", "cat2");
        assertThat(e.getLabels()).containsExactly("l1", "l2");
        assertThat(e.getGroups()).containsExactlyInAnyOrder("g1", "g2");
        assertThat(e.getLifecycleState()).hasToString("CREATED");
        assertThat(e.isDisableMembershipNotifications()).isTrue();
        assertThat(e.getBackgroundUrl()).isEqualTo("http://example/bg.png");

        assertThat(e.getResponseTemplates()).isInstanceOf(Map.class);
        assertThat(e.getResponseTemplates()).containsKey("DEFAULT");

        assertThat(e.getProperties()).isNotNull();
        assertThat(e.getPropertyList()).extracting(Property::getKey).containsExactly("k1", "k2");
        assertThat(e.getPropertyList()).extracting(Property::getValue).containsExactly("v1", "v2");
    }

    @Test
    void should_throw_on_v4_definitionVersion() {
        String json = """
            {
              "id": "api-456",
              "name": "v4 API",
              "definitionVersion": "4.0.0"
            }
            """;

        assertThatThrownBy(() -> mapper.readValue(json, RollbackApiEntity.class))
            .isInstanceOf(JsonMappingException.class)
            .hasMessageContaining("Detected a v4 API definition. Please use the migration tool instead of rollback.");
    }

    @Test
    void should_deserialize_minimal_payload_and_apply_fallbacks() throws Exception {
        String json = """
            {
              "id": "api-min",
              "name": "Minimal API"
            }
            """;

        RollbackApiEntity e = mapper.readValue(json, RollbackApiEntity.class);

        assertThat(e.getId()).isEqualTo("api-min");
        assertThat(e.getName()).isEqualTo("Minimal API");

        assertThat(e.getPlans()).isEmpty();
        assertThat(e.getPaths()).isEmpty();
        assertThat(e.getFlows()).isEmpty();
        assertThat(e.getResources()).isEmpty();

        assertThat(e.getGraviteeDefinitionVersion()).isNull();
    }

    @Test
    void should_ignore_unknown_fields_and_allow_non_v4_definitionVersion() throws Exception {
        String json = """
            {
              "id": "api-789",
              "name": "Unknowns API",
              "definitionVersion": "3.0.0",
              "some_unknown_field": "whatever"
            }
            """;

        RollbackApiEntity e = mapper.readValue(json, RollbackApiEntity.class);

        assertThat(e.getId()).isEqualTo("api-789");
        assertThat(e.getName()).isEqualTo("Unknowns API");
        assertThat(e.getGraviteeDefinitionVersion()).isNull();
    }
}
