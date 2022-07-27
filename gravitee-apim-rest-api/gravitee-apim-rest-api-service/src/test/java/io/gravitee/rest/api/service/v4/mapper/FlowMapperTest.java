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
package io.gravitee.rest.api.service.v4.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorHttp;
import io.gravitee.definition.model.v4.flow.step.Step;
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

    private final FlowMapper flowMapper = new FlowMapper();

    private static List<Selector> selectors() {
        SelectorHttp selectorHttp = new SelectorHttp();
        selectorHttp.setPath("/");
        selectorHttp.setPathOperator(Operator.STARTS_WITH);
        return List.of(selectorHttp);
    }

    private static Set<String> tags() {
        return Set.of("consumer");
    }

    private static List<Step> request() {
        Step step = new Step();
        step.setEnabled(true);
        step.setName("IPFiltering");
        step.setPolicy("ip-filtering");
        step.setConfiguration("{\"whitelistIps\":[\"0.0.0.0/0\"]}");
        return List.of(step);
    }

    private static List<Step> response() {
        Step step = new Step();
        step.setEnabled(true);
        step.setName("Transform Headers");
        step.setPolicy("transform-headers");
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

        Flow flowDefinition = flowMapper.toDefinition(flow);

        assertNotNull(flowDefinition.getSelectors());
        assertEquals(selectors, flowDefinition.getSelectors());
    }
}
