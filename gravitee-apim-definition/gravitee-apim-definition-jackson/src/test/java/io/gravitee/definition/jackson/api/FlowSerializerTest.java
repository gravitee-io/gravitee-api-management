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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.flow.Flow;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class FlowSerializerTest extends AbstractTest {

    @Test
    public void should_serialize_methods_in_the_same_order() throws JsonProcessingException {
        Flow flow1 = new Flow();
        flow1.setMethods(Set.of(HttpMethod.POST, HttpMethod.PUT));

        Flow flow2 = new Flow();
        flow2.setMethods(Set.of(HttpMethod.PUT, HttpMethod.POST));

        String flow1json = objectMapper().writeValueAsString(flow1);
        String flow2json = objectMapper().writeValueAsString(flow2);

        assertEquals(flow1json, flow2json);
    }
}
