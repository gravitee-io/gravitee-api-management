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
package io.gravitee.rest.api.service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.repository.management.model.flow.FlowConsumer;
import io.gravitee.repository.management.model.flow.FlowConsumerType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class FlowConverter {

    private ObjectMapper objectMapper;

    public FlowConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Flow toDefinition(io.gravitee.repository.management.model.flow.Flow model) {
        Flow flow = new Flow();
        flow.setId(model.getId());

        flow.setCondition(model.getCondition());
        flow.setEnabled(model.isEnabled());
        flow.setMethods(model.getMethods());
        flow.setName(model.getName());
        final PathOperator pathOperator = new PathOperator();
        pathOperator.setPath(model.getPath());
        pathOperator.setOperator(Operator.valueOf(model.getOperator().name()));
        flow.setPathOperator(pathOperator);
        flow.setPre(model.getPre().stream().map(this::toDefinitionStep).filter(Objects::nonNull).collect(Collectors.toList()));
        flow.setPost(model.getPost().stream().map(this::toDefinitionStep).filter(Objects::nonNull).collect(Collectors.toList()));
        flow.setConsumers(model.getConsumers().stream().map(this::toDefinitionConsumer).collect(Collectors.toList()));
        return flow;
    }

    public io.gravitee.repository.management.model.flow.Flow toRepository(
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
        flow.setPost(toRepositoryFlowSteps(flowDefinition.getPost()));
        flow.setPre(toRepositoryFlowSteps(flowDefinition.getPre()));
        flow.setPath(flowDefinition.getPath());
        flow.setOperator(FlowOperator.valueOf(flowDefinition.getOperator().name()));
        flow.setName(flowDefinition.getName());
        flow.setMethods(flowDefinition.getMethods());
        flow.setEnabled(flowDefinition.isEnabled());
        flow.setCondition(flowDefinition.getCondition());
        flow.setConsumers(
            flowDefinition.getConsumers() != null
                ? flowDefinition.getConsumers().stream().map(this::toRepositoryConsumer).collect(Collectors.toList())
                : Collections.emptyList()
        );
        return flow;
    }

    @NotNull
    private List<FlowStep> toRepositoryFlowSteps(List<Step> steps) {
        if (steps == null) {
            return Collections.emptyList();
        }

        return IntStream.range(0, steps.size())
            .mapToObj(index -> this.toRepositoryStep(steps.get(index), index))
            .collect(Collectors.toList());
    }

    private FlowConsumer toRepositoryConsumer(Consumer consumer) {
        FlowConsumer flowConsumer = new FlowConsumer();
        flowConsumer.setConsumerId(consumer.getConsumerId());
        flowConsumer.setConsumerType(FlowConsumerType.valueOf(consumer.getConsumerType().name()));
        return flowConsumer;
    }

    private Consumer toDefinitionConsumer(FlowConsumer flowConsumer) {
        Consumer consumer = new Consumer();
        consumer.setConsumerId(flowConsumer.getConsumerId());
        consumer.setConsumerType(ConsumerType.valueOf(flowConsumer.getConsumerType().name()));
        return consumer;
    }

    @NotNull
    private FlowStep toRepositoryStep(Step step, int order) {
        FlowStep flowStep = new FlowStep();
        flowStep.setPolicy(step.getPolicy());
        flowStep.setName(step.getName());
        flowStep.setEnabled(step.isEnabled());
        flowStep.setConfiguration(step.getConfiguration());
        flowStep.setDescription(step.getDescription());
        flowStep.setCondition(step.getCondition());
        flowStep.setOrder(order);
        return flowStep;
    }

    protected Step toDefinitionStep(FlowStep flowStep) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(flowStep), Step.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert repository flow step to model", e);
            return null;
        }
    }

    public io.gravitee.repository.management.model.flow.Flow toRepositoryUpdate(
        io.gravitee.repository.management.model.flow.Flow dbFlow,
        Flow flowDefinition,
        int order
    ) {
        io.gravitee.repository.management.model.flow.Flow flow = toRepository(
            flowDefinition,
            dbFlow.getReferenceType(),
            dbFlow.getReferenceId(),
            order
        );
        flow.setId(dbFlow.getId());
        flow.setCreatedAt(dbFlow.getCreatedAt());
        flow.setUpdatedAt(new Date());
        return flow;
    }
}
