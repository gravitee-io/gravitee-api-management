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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.flow.FlowV2Impl;
import java.io.IOException;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class FlowV2SerializerTest extends AbstractTest {

    @Test
    public void should_serialize_methods_in_the_same_order() throws JsonProcessingException {
        FlowV2Impl flow1 = new FlowV2Impl();
        flow1.setMethods(Set.of(HttpMethod.POST, HttpMethod.PUT));

        FlowV2Impl flow2 = new FlowV2Impl();
        flow2.setMethods(Set.of(HttpMethod.PUT, HttpMethod.POST));

        String flow1json = objectMapper().writeValueAsString(flow1);
        String flow2json = objectMapper().writeValueAsString(flow2);

        assertEquals(flow1json, flow2json);
    }

    @Test
    public void should_handle_null_values() throws IOException {
        final String rawDefinitionToSerialize = IOUtils.toString(read("/io/gravitee/definition/jackson/flow-nullvalue.json"));

        final FlowV2Impl flow = objectMapper().readValue(rawDefinitionToSerialize, FlowV2Impl.class);
        assertNull(flow.getName());
        assertNull(flow.getCondition());
        assertNull(flow.getPre().get(0).getDescription());
    }

    @Test
    public void should_map_missing_fields_to_null() throws IOException {
        final String rawDefinitionToSerialize = IOUtils.toString(read("/io/gravitee/definition/jackson/flow-missingfields.json"));

        final FlowV2Impl flow = objectMapper().readValue(rawDefinitionToSerialize, FlowV2Impl.class);
        assertNull(flow.getName());
        assertNull(flow.getCondition());
        assertNull(flow.getPre().get(0).getDescription());
    }
}
