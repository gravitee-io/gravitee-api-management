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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import org.junit.jupiter.api.AssertionFailureBuilder;

abstract class AbstractMapperTest {

    private static final ObjectMapper jsonMapper = new GraviteeMapper();

    protected void assertConfigurationEquals(Object definition, Object other) {
        try {
            JsonNode definitionJsonNode = toJsonNode(definition);
            JsonNode otherJsonNode = toJsonNode(other);

            assertEquals(definitionJsonNode, otherJsonNode);
        } catch (Exception e) {
            AssertionFailureBuilder.assertionFailure()
                .message("Definitions are not equals")
                .expected(String.valueOf(definition))
                .actual(String.valueOf(other))
                .buildAndThrow();
        }
    }

    protected JsonNode toJsonNode(Object definition) throws Exception {
        if (definition instanceof String) {
            return jsonMapper.readTree((String) definition);
        }

        if (!(definition instanceof JsonNode)) {
            return jsonMapper.valueToTree(definition);
        }

        return (JsonNode) definition;
    }
}
