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
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class FlowConverterTest {

    private final FlowConverter converter = new FlowConverter(new ServiceConfiguration().objectMapper());

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

        Step step2 = new Step();
        step2.setEnabled(true);
        step2.setName("HTTP Callout");
        step2.setPolicy("http-callout");
        step2.setConfiguration("{\"url\":\"http://localhost\"}");
        return List.of(step, step2);
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

        var model = converter.toRepository(flowDefinition, FlowReferenceType.ORGANIZATION, "DEFAULT", 0);

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
    public void toModelShouldKeepTheStepOrder() {
        Flow flowDefinition = new Flow();
        flowDefinition.setPre(pre());

        var model = converter.toRepository(flowDefinition, FlowReferenceType.ORGANIZATION, "DEFAULT", 0);

        assertEquals(2, model.getPre().size());
        assertEquals("IPFiltering", model.getPre().get(0).getName());
        assertEquals(0, model.getPre().get(0).getOrder());
        assertEquals("HTTP Callout", model.getPre().get(1).getName());
        assertEquals(1, model.getPre().get(1).getOrder());
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

    @Test
    public void toDefinitionStepShouldMapAllStepData() {
        FlowStep flowStep = new FlowStep();
        flowStep.setPolicy("test-policy");
        flowStep.setDescription("test-description");
        flowStep.setCondition("test-condition");
        flowStep.setEnabled(false);
        flowStep.setName("test-name");
        flowStep.setConfiguration("{}");

        Step step = converter.toDefinitionStep(flowStep);

        assertEquals(flowStep.getPolicy(), step.getPolicy());
        assertEquals(flowStep.getDescription(), step.getDescription());
        assertEquals(flowStep.getCondition(), step.getCondition());
        assertEquals(flowStep.getName(), step.getName());
        assertEquals(flowStep.isEnabled(), step.isEnabled());
        assertEquals(flowStep.getConfiguration(), step.getConfiguration());
    }

    @Test
    public void toDefinitionStepShouldMapConfiguration() {
        FlowStep flowStep = new FlowStep();
        flowStep.setPolicy("test-policy");
        flowStep.setConfiguration("{\"key\": \"value\"}");

        Step step = converter.toDefinitionStep(flowStep);
        assertEquals("{\"key\":\"value\"}", step.getConfiguration());
    }

    @Test
    public void toDefinitionStepShouldMapConfigurationWithEscapedValues() {
        FlowStep flowStep = new FlowStep();
        flowStep.setPolicy("test-policy");
        flowStep.setConfiguration("{\"key\": \"<\\/value\\nvalue>\"}");

        Step step = converter.toDefinitionStep(flowStep);
        assertEquals("{\"key\":\"</value\\nvalue>\"}", step.getConfiguration());
    }

    @Test
    public void toDefinitionShouldKeepTheStepOrder() {
        final PathOperator expectedOperator = pathOperator();

        var flow = new io.gravitee.repository.management.model.flow.Flow();
        flow.setPath("/");
        flow.setOperator(FlowOperator.STARTS_WITH);
        flow.setConsumers(List.of());
        flow.setPre(definitionPre());

        Flow flowDefinition = converter.toDefinition(flow);

        assertEquals(2, flowDefinition.getPre().size());
        assertEquals("IPFiltering", flowDefinition.getPre().get(0).getName());
        assertEquals("HTTP Callout", flowDefinition.getPre().get(1).getName());
    }

    private static List<FlowStep> definitionPre() {
        FlowStep flowStep = new FlowStep();
        flowStep.setEnabled(true);
        flowStep.setName("IPFiltering");
        flowStep.setPolicy("ip-filtering");
        flowStep.setConfiguration("{\"whitelistIps\":[\"0.0.0.0/0\"]}");
        flowStep.setOrder(0);

        FlowStep flowStep2 = new FlowStep();
        flowStep2.setEnabled(true);
        flowStep2.setName("HTTP Callout");
        flowStep2.setPolicy("http-callout");
        flowStep2.setConfiguration("{\"url\":\"http://localhost\"}");
        flowStep2.setOrder(1);

        return List.of(flowStep, flowStep2);
    }
}
