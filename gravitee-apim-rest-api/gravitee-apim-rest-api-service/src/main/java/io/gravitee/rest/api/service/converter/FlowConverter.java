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

import io.gravitee.definition.model.flow.*;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.flow.*;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class FlowConverter {

    public Flow toDefinition(io.gravitee.repository.management.model.flow.Flow model) {
        Flow flow = new Flow();
        flow.setCondition(model.getCondition());
        flow.setEnabled(model.isEnabled());
        flow.setMethods(model.getMethods());
        flow.setName(model.getName());
        final PathOperator pathOperator = new PathOperator();
        pathOperator.setPath(model.getPath());
        pathOperator.setOperator(Operator.valueOf(model.getOperator().name()));
        flow.setPathOperator(pathOperator);
        flow.setPre(model.getPre().stream().map(this::toDefinitionFlow).collect(Collectors.toList()));
        flow.setPost(model.getPost().stream().map(this::toDefinitionFlow).collect(Collectors.toList()));
        flow.setConsumers(model.getConsumers().stream().map(this::toDefinitionFlow).collect(Collectors.toList()));
        return flow;
    }

    public io.gravitee.repository.management.model.flow.Flow toModel(
        Flow flowDefinition,
        FlowReferenceType referenceType,
        String referenceId,
        int order
    ) {
        io.gravitee.repository.management.model.flow.Flow flow = new io.gravitee.repository.management.model.flow.Flow();
        flow.setId(UuidString.generateRandom());
        flow.setCreatedAt(new Date());
        flow.setUpdatedAt(flow.getCreatedAt());
        flow.setOrder(order);
        flow.setReferenceType(referenceType);
        flow.setReferenceId(referenceId);
        flow.setPost(flowDefinition.getPost().stream().map(this::convertStep).collect(Collectors.toList()));
        flow.setPre(flowDefinition.getPre().stream().map(this::convertStep).collect(Collectors.toList()));
        flow.setPath(flowDefinition.getPath());
        flow.setOperator(FlowOperator.valueOf(flowDefinition.getOperator().name()));
        flow.setName(flowDefinition.getName());
        flow.setMethods(flowDefinition.getMethods());
        flow.setEnabled(flowDefinition.isEnabled());
        flow.setCondition(flowDefinition.getCondition());
        flow.setConsumers(flowDefinition.getConsumers().stream().map(this::convertConsumer).collect(Collectors.toList()));
        flow.setConsumers(
            flow.getConsumers() != null
                ? flowDefinition.getConsumers().stream().map(this::convertConsumer).collect(Collectors.toList())
                : Collections.emptyList()
        );
        return flow;
    }

    private FlowConsumer convertConsumer(Consumer consumer) {
        FlowConsumer flowConsumer = new FlowConsumer();
        flowConsumer.setConsumerId(consumer.getConsumerId());
        flowConsumer.setConsumerType(FlowConsumerType.valueOf(consumer.getConsumerType().name()));
        return flowConsumer;
    }

    private Consumer toDefinitionFlow(FlowConsumer flowConsumer) {
        Consumer consumer = new Consumer();
        consumer.setConsumerId(flowConsumer.getConsumerId());
        consumer.setConsumerType(ConsumerType.valueOf(flowConsumer.getConsumerType().name()));
        return consumer;
    }

    private FlowStep convertStep(Step step) {
        FlowStep flowStep = new FlowStep();
        flowStep.setPolicy(step.getPolicy());
        flowStep.setName(step.getName());
        flowStep.setEnabled(step.isEnabled());
        flowStep.setConfiguration(step.getConfiguration());
        flowStep.setDescription(step.getDescription());
        flowStep.setCondition(step.getCondition());
        return flowStep;
    }

    private Step toDefinitionFlow(FlowStep flowStep) {
        Step step = new Step();
        step.setPolicy(flowStep.getPolicy());
        step.setName(flowStep.getName());
        step.setEnabled(flowStep.isEnabled());
        step.setConfiguration(flowStep.getConfiguration());
        step.setDescription(flowStep.getDescription());
        step.setCondition(flowStep.getCondition());
        return step;
    }
}
