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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class ApiServiceImplTest {

    private ApiServiceImpl cut = new ApiServiceImpl();

    @Test
    public void shouldGetConfigurationSchema() {
        assertEquals(
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"id\": \"apim\",\n" +
            "  \"properties\": {\n" +
            "    \"flow-mode\": {\n" +
            "      \"title\": \"Flow Mode\",\n" +
            "      \"description\": \"The flow mode\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [ \"default\", \"best_match\" ],\n" +
            "      \"default\": \"default\",\n" +
            "      \"x-schema-form\": {\n" +
            "        \"titleMap\": {\n" +
            "          \"default\": \"Default\",\n" +
            "          \"best_match\": \"Best match\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [],\n" +
            "  \"disabled\": []\n" +
            "}\n",
            cut.getConfigurationSchema()
        );
    }
}
