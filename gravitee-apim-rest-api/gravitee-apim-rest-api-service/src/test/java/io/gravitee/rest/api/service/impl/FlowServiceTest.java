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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.rest.api.service.converter.FlowConverter;
import io.gravitee.rest.api.service.impl.configuration.flow.FlowServiceImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FlowServiceTest {

    @InjectMocks
    private FlowServiceImpl flowService = new FlowServiceImpl();

    @Mock
    private FlowRepository flowRepository;

    @Spy
    private FlowConverter flowConverter = new FlowConverter(new ObjectMapper());

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
                "    \"flow_mode\": {\n" +
                "      \"title\": \"Flow Mode\",\n" +
                "      \"description\": \"The flow mode\",\n" +
                "      \"type\": \"string\",\n" +
                "      \"enum\": [ \"DEFAULT\", \"BEST_MATCH\" ],\n" +
                "      \"default\": \"DEFAULT\",\n" +
                "      \"x-schema-form\": {\n" +
                "        \"titleMap\": {\n" +
                "          \"DEFAULT\": \"Default\",\n" +
                "          \"BEST_MATCH\": \"Best match\"\n" +
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

    @Test
    public void shouldFindByReference() throws TechnicalException {
        List<io.gravitee.repository.management.model.flow.Flow> flows = new ArrayList<>();
        flows.addAll(generateRepoFlows());

        when(flowRepository.findByReference(FlowReferenceType.API, "api-id")).thenReturn(flows);

        List<Flow> byReference = flowService.findByReference(FlowReferenceType.API, "api-id");

        assertNotNull(byReference);
        assertEquals(
            byReference
                .stream()
                .map(f -> f.getId())
                .collect(Collectors.joining(", ", "[", "]")),
            "[flow-0, flow-1, flow-2]"
        );
    }

    @Test
    public void shouldSave() throws TechnicalException {
        List<io.gravitee.repository.management.model.flow.Flow> flows = new ArrayList<>();
        flows.addAll(generateRepoFlows());

        when(flowRepository.findByReference(FlowReferenceType.API, "api-id")).thenReturn(flows);

        io.gravitee.repository.management.model.flow.Flow updatedFlow = createRepoFlow(1);
        when(flowRepository.updateAll(any())).thenReturn(List.of(updatedFlow));

        io.gravitee.repository.management.model.flow.Flow createdFlow4 = createRepoFlow(4);
        io.gravitee.repository.management.model.flow.Flow createdFlow5 = createRepoFlow(5);
        when(flowRepository.createAll(any())).thenReturn(List.of(createdFlow4, createdFlow5));

        List<Flow> savedFlows = new ArrayList<>();
        savedFlows.add(createFlow("flow-1"));
        savedFlows.add(createFlow("ignore"));
        savedFlows.add(createFlow(null));

        List<Flow> byReference = flowService.save(FlowReferenceType.API, "api-id", savedFlows);

        verify(flowRepository, times(1)).delete("flow-0");
        verify(flowRepository, times(1)).delete("flow-2");
        verify(flowRepository, times(1)).updateAll(any());
        verify(flowRepository, times(1)).createAll(any());
        assertNotNull(byReference);
        assertEquals(3, byReference.size());
    }

    @Test
    public void shouldSaveWhenNoFlowInDb() throws TechnicalException {
        when(flowRepository.findByReference(FlowReferenceType.API, "api-id")).thenReturn(List.of());

        io.gravitee.repository.management.model.flow.Flow createdFlow1 = createRepoFlow(1);
        io.gravitee.repository.management.model.flow.Flow createdFlow2 = createRepoFlow(2);
        when(flowRepository.createAll(any())).thenReturn(List.of(createdFlow1, createdFlow2));

        List<Flow> savedFlows = new ArrayList<>();
        savedFlows.add(createFlow("flow-1"));
        savedFlows.add(createFlow(null));

        List<Flow> byReference = flowService.save(FlowReferenceType.API, "api-id", savedFlows);

        verify(flowRepository, never()).delete(any());
        verify(flowRepository, never()).updateAll(any());
        verify(flowRepository, times(1)).createAll(any());
        assertNotNull(byReference);
        assertEquals(2, byReference.size());
    }

    private Flow createFlow(String id) {
        Flow flow = new Flow();
        if (id != null) {
            flow.setId(id);
        }
        return flow;
    }

    private Collection<? extends io.gravitee.repository.management.model.flow.Flow> generateRepoFlows() {
        return List.of(createRepoFlow(1), createRepoFlow(2), createRepoFlow(0));
    }

    private io.gravitee.repository.management.model.flow.Flow createRepoFlow(int order) {
        io.gravitee.repository.management.model.flow.Flow flow = mock(io.gravitee.repository.management.model.flow.Flow.class);
        when(flow.getId()).thenReturn("flow-" + order);
        when(flow.getOrder()).thenReturn(order);
        when(flow.getOperator()).thenReturn(FlowOperator.STARTS_WITH);
        return flow;
    }
}
