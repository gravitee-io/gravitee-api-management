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
package io.gravitee.rest.api.service.v4.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowMcpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowMapperTest {

    protected static final String POLICY_CONDITION = "Policy condition";
    protected static final String MESSAGE_LEVEL_CONDITION = "Message level condition";
    private final FlowMapper flowMapper = new FlowMapper();

    private static List<Selector> selectors() {
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        McpSelector mcpSelector = new McpSelector();
        mcpSelector.setMethods(Set.of("mcp"));
        return List.of(httpSelector, mcpSelector);
    }

    private static Set<String> tags() {
        return Set.of("consumer");
    }

    private static List<Step> request() {
        Step step = new Step();
        step.setEnabled(true);
        step.setName("IPFiltering");
        step.setPolicy("ip-filtering");
        step.setCondition(POLICY_CONDITION);
        step.setMessageCondition(MESSAGE_LEVEL_CONDITION);
        step.setConfiguration("{\"whitelistIps\":[\"0.0.0.0/0\"]}");
        return List.of(step);
    }

    private static List<Step> response() {
        Step step = new Step();
        step.setEnabled(true);
        step.setName("Transform Headers");
        step.setPolicy("transform-headers");
        step.setCondition(POLICY_CONDITION);
        step.setMessageCondition(MESSAGE_LEVEL_CONDITION);
        step.setConfiguration("{\"scope\":\"RESPONSE\",\"addHeaders\":[{\"name\":\"x-platform\",\"value\":\"true\"}]}");
        return List.of(step);
    }

    @Test
    public void toRepositoryShouldInitializeNonNullableFields() {
        Flow flowDefinition = new Flow();
        flowDefinition.setName("platform");
        flowDefinition.setSelectors(selectors());
        flowDefinition.setTags(tags());
        flowDefinition.setEnabled(true);
        flowDefinition.setRequest(request());
        flowDefinition.setResponse(response());
        flowDefinition.setSubscribe(request());
        flowDefinition.setPublish(response());

        var model = flowMapper.toRepository(flowDefinition, FlowReferenceType.ORGANIZATION, "DEFAULT", 0);

        assertNotNull(model.getId());
        assertNotNull(model.getCreatedAt());
        assertNotNull(model.getUpdatedAt());
        assertFalse(model.getResponse().isEmpty());
        assertFalse(model.getRequest().isEmpty());
        assertFalse(model.getSubscribe().isEmpty());
        assertFalse(model.getPublish().isEmpty());
        assertFalse(model.getTags().isEmpty());
        assertEquals(FlowReferenceType.ORGANIZATION, model.getReferenceType());
        assertEquals("DEFAULT", model.getReferenceId());

        assertEquals(POLICY_CONDITION, model.getPublish().get(0).getCondition());
        assertEquals(MESSAGE_LEVEL_CONDITION, model.getPublish().get(0).getMessageCondition());
        assertEquals(POLICY_CONDITION, model.getSubscribe().get(0).getCondition());
        assertEquals(MESSAGE_LEVEL_CONDITION, model.getSubscribe().get(0).getMessageCondition());
    }

    @Test
    public void should_map_entrypoint_connect_steps() {
        var repositoryFlow = new io.gravitee.repository.management.model.flow.Flow();
        repositoryFlow.setName("test-flow");
        repositoryFlow.setEnabled(true);

        io.gravitee.repository.management.model.flow.FlowStep entrypointConnectStep =
            new io.gravitee.repository.management.model.flow.FlowStep();
        entrypointConnectStep.setEnabled(true);
        entrypointConnectStep.setName("IPFiltering");
        entrypointConnectStep.setPolicy("ip-filtering");
        entrypointConnectStep.setCondition(POLICY_CONDITION);
        entrypointConnectStep.setConfiguration("{\"blacklistIps\":[\"127.0.0.1\"]}");
        repositoryFlow.setEntrypointConnect(List.of(entrypointConnectStep));

        io.gravitee.definition.model.v4.nativeapi.NativeFlow nativeFlow = flowMapper.toNativeDefinition(repositoryFlow);

        assertNotNull(nativeFlow.getEntrypointConnect());
        assertFalse(nativeFlow.getEntrypointConnect().isEmpty());
        assertEquals(1, nativeFlow.getEntrypointConnect().size());

        Step mappedStep = nativeFlow.getEntrypointConnect().get(0);
        assertEquals("ip-filtering", mappedStep.getPolicy());
        assertEquals(POLICY_CONDITION, mappedStep.getCondition());
        assertEquals("{\"blacklistIps\":[\"127.0.0.1\"]}", mappedStep.getConfiguration());
    }

    @Test
    public void toDefinitionShouldSetPathOperatorFromPathAndOperatorValues() {
        final List<Selector> selectors = selectors();

        var flow = new io.gravitee.repository.management.model.flow.Flow();
        FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
        flowHttpSelector.setPath("/");
        flowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        FlowMcpSelector flowMcpSelector = new FlowMcpSelector();
        flowMcpSelector.setMethods(Set.of("mcp"));
        flow.setSelectors(List.of(flowHttpSelector, flowMcpSelector));
        flow.setTags(Set.of());

        Flow flowDefinition = flowMapper.toDefinition(flow);

        assertNotNull(flowDefinition.getSelectors());
        assertEquals(selectors, flowDefinition.getSelectors());
    }
}
