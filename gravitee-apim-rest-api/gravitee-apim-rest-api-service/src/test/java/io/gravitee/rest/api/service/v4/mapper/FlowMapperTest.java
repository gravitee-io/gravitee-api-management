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
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.step.StepV4;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
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
        return List.of(httpSelector);
    }

    private static Set<String> tags() {
        return Set.of("consumer");
    }

    private static List<StepV4> request() {
        StepV4 step = new StepV4();
        step.setEnabled(true);
        step.setName("IPFiltering");
        step.setPolicy("ip-filtering");
        step.setCondition(POLICY_CONDITION);
        step.setMessageCondition(MESSAGE_LEVEL_CONDITION);
        step.setConfiguration("{\"whitelistIps\":[\"0.0.0.0/0\"]}");
        return List.of(step);
    }

    private static List<StepV4> response() {
        StepV4 step = new StepV4();
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
        FlowV4Impl flowDefinition = new FlowV4Impl();
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
    public void toDefinitionShouldSetPathOperatorFromPathAndOperatorValues() {
        final List<Selector> selectors = selectors();

        var flow = new io.gravitee.repository.management.model.flow.Flow();
        FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
        flowHttpSelector.setPath("/");
        flowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        flow.setSelectors(List.of(flowHttpSelector));
        flow.setTags(Set.of());

        FlowV4Impl flowDefinition = flowMapper.toDefinition(flow);

        assertNotNull(flowDefinition.getSelectors());
        assertEquals(selectors, flowDefinition.getSelectors());
    }
}
