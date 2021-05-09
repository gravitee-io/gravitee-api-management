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
package io.gravitee.rest.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.gravitee.rest.api.service.impl.configuration.flow.FlowServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FlowServiceTest {

    private FlowServiceImpl flowService = new FlowServiceImpl();

    @Test
    public void shouldGetConfigurationSchemaForm() {
        String apiFlowSchemaForm = flowService.getApiFlowSchemaForm();
        assertNotNull(apiFlowSchemaForm);
        assertEquals(
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"id\": \"apim\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"title\": \"Name\",\n" +
            "      \"description\": \"The name of flow. If empty, the name will be generated with the path and methods\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"path-operator\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"operator\": {\n" +
            "          \"title\": \"Operator path\",\n" +
            "          \"description\": \"The operator path\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"enum\": [\n" +
            "            \"EQUALS\",\n" +
            "            \"STARTS_WITH\"\n" +
            "          ],\n" +
            "          \"default\": \"STARTS_WITH\",\n" +
            "          \"x-schema-form\": {\n" +
            "            \"titleMap\": {\n" +
            "              \"EQUALS\": \"Equals\",\n" +
            "              \"STARTS_WITH\": \"Starts with\"\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        \"path\": {\n" +
            "          \"title\": \"Path\",\n" +
            "          \"description\": \"The path of flow (must start by /)\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"pattern\": \"^/\",\n" +
            "          \"default\": \"/\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\n" +
            "        \"path\",\n" +
            "        \"operator\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"methods\": {\n" +
            "      \"title\": \"Methods\",\n" +
            "      \"description\": \"The HTTP methods of flow (ALL if empty)\",\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\" : {\n" +
            "        \"type\" : \"string\",\n" +
            "        \"enum\" : [ \"CONNECT\", \"DELETE\", \"GET\", \"HEAD\", \"OPTIONS\", \"PATCH\", \"POST\", \"PUT\", \"TRACE\" ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"condition\": {\n" +
            "      \"title\": \"Condition\",\n" +
            "      \"description\": \"The condition of the flow. Supports EL.\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"x-schema-form\": {\n" +
            "        \"expression-language\": true\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [],\n" +
            "  \"disabled\": []\n" +
            "}\n",
            apiFlowSchemaForm
        );
    }

    @Test
    public void shouldGetApiFlowSchemaForm() {
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
            flowService.getConfigurationSchemaForm()
        );
    }
}
