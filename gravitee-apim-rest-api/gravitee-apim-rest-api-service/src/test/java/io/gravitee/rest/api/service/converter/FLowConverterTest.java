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
package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.*;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class FLowConverterTest {

    private final FlowConverter converter = new FlowConverter();

    private static PathOperator pathOperator() {
        PathOperator pathOperator = new PathOperator();
        pathOperator.setPath("/");
        return pathOperator;
    }

    private static List<Consumer> consumers() {
        Consumer consumer = new Consumer();
        consumer.setConsumerId("consumer");
        consumer.setConsumerType(ConsumerType.TAG);
        return List.of(consumer);
    }

    private static List<Step> pre() {
        Step step = new Step();
        step.setEnabled(true);
        step.setName("IPFiltering");
        step.setPolicy("ip-filtering");
        step.setConfiguration("{\"whitelistIps\":[\"0.0.0.0/0\"]}");
        return List.of(step);
    }

    private static List<Step> post() {
        Step step = new Step();
        step.setEnabled(true);
        step.setName("Transform Headers");
        step.setPolicy("transform-headers");
        step.setConfiguration("{\"scope\":\"RESPONSE\",\"addHeaders\":[{\"name\":\"x-platform\",\"value\":\"true\"}]}");
        return List.of(step);
    }

    @Test
    public void toModelShouldInitializeNonNullableFields() {
        Flow flowDefinition = new Flow();
        flowDefinition.setName("platform");
        flowDefinition.setPathOperator(pathOperator());
        flowDefinition.setConsumers(consumers());
        flowDefinition.setEnabled(true);
        flowDefinition.setMethods(Set.of(HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.PATCH));
        flowDefinition.setCondition("true");
        flowDefinition.setPre(pre());
        flowDefinition.setPost(post());

        var model = converter.toModel(flowDefinition, FlowReferenceType.ORGANIZATION, "DEFAULT", 0);

        assertNotNull(model.getId());
        assertNotNull(model.getCreatedAt());
        assertNotNull(model.getUpdatedAt());
        assertFalse(model.getPre().isEmpty());
        assertFalse(model.getPost().isEmpty());
        assertFalse(model.getConsumers().isEmpty());
        assertEquals(FlowReferenceType.ORGANIZATION, model.getReferenceType());
        assertEquals("DEFAULT", model.getReferenceId());
    }

    @Test
    public void toDefinitionShouldSetPathOperatorFromPathAndOperatorValues() {
        final PathOperator expectedOperator = pathOperator();

        var flow = new io.gravitee.repository.management.model.flow.Flow();
        flow.setPath("/");
        flow.setOperator(FlowOperator.STARTS_WITH);
        flow.setConsumers(List.of());

        Flow flowDefinition = converter.toDefinition(flow);

        assertNotNull(flowDefinition.getPathOperator());
        assertEquals(expectedOperator.getOperator(), flowDefinition.getPathOperator().getOperator());
        assertEquals(expectedOperator.getPath(), flowDefinition.getPathOperator().getPath());
    }
}
