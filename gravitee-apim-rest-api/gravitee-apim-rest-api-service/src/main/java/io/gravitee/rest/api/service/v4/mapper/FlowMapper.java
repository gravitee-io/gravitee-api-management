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

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorChannel;
import io.gravitee.definition.model.v4.flow.selector.SelectorCondition;
import io.gravitee.definition.model.v4.flow.selector.SelectorHttp;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
<<<<<<< HEAD
@Component("FlowMapperV4")
=======
@Component
>>>>>>> 2a8318ccf0 (feat(definition): add api definition v4 classes)
public class FlowMapper {

    public Flow toDefinition(io.gravitee.repository.management.model.flow.Flow repositoryFlow) {
        Flow definitionFlow = new Flow();
        definitionFlow.setName(repositoryFlow.getName());
        definitionFlow.setEnabled(repositoryFlow.isEnabled());
        definitionFlow.setRequest(repositoryFlow.getRequest().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setResponse(repositoryFlow.getResponse().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setPublish(repositoryFlow.getPublish().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setSubscribe(repositoryFlow.getSubscribe().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setSelectors(repositoryFlow.getSelectors().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setTags(repositoryFlow.getTags());
        return definitionFlow;
    }

    public io.gravitee.repository.management.model.flow.Flow toRepository(
        final Flow definitionFlow,
        final FlowReferenceType referenceType,
        final String referenceId,
        final int order
    ) {
        io.gravitee.repository.management.model.flow.Flow repositoryFlow = new io.gravitee.repository.management.model.flow.Flow();
        repositoryFlow.setId(UuidString.generateRandom());
        repositoryFlow.setName(definitionFlow.getName());
        repositoryFlow.setEnabled(definitionFlow.isEnabled());
        repositoryFlow.setCreatedAt(new Date());
        repositoryFlow.setUpdatedAt(repositoryFlow.getCreatedAt());
        repositoryFlow.setOrder(order);
        repositoryFlow.setReferenceType(referenceType);
        repositoryFlow.setReferenceId(referenceId);

        repositoryFlow.setRequest(definitionFlow.getRequest().stream().map(this::toRepository).collect(Collectors.toList()));
        repositoryFlow.setResponse(definitionFlow.getResponse().stream().map(this::toRepository).collect(Collectors.toList()));
        repositoryFlow.setPublish(definitionFlow.getPublish().stream().map(this::toRepository).collect(Collectors.toList()));
        repositoryFlow.setSubscribe(definitionFlow.getSubscribe().stream().map(this::toRepository).collect(Collectors.toList()));
        repositoryFlow.setSelectors(definitionFlow.getSelectors().stream().map(this::toRepository).collect(Collectors.toList()));
        definitionFlow.setTags(repositoryFlow.getTags());

        return repositoryFlow;
    }

    private FlowStep toRepository(final Step definitionStep) {
        FlowStep repositoryStep = new FlowStep();
        repositoryStep.setPolicy(definitionStep.getPolicy());
        repositoryStep.setName(definitionStep.getName());
        repositoryStep.setEnabled(definitionStep.isEnabled());
        repositoryStep.setConfiguration(definitionStep.getConfiguration());
        repositoryStep.setDescription(definitionStep.getDescription());
        repositoryStep.setCondition(definitionStep.getCondition());
        return repositoryStep;
    }

    private Step toDefinition(final FlowStep repositoryStep) {
        Step definitionStep = new Step();
        definitionStep.setPolicy(repositoryStep.getPolicy());
        definitionStep.setName(repositoryStep.getName());
        definitionStep.setEnabled(repositoryStep.isEnabled());
        definitionStep.setConfiguration(repositoryStep.getConfiguration());
        definitionStep.setDescription(repositoryStep.getDescription());
        definitionStep.setCondition(repositoryStep.getCondition());
        return definitionStep;
    }

    private FlowSelector toRepository(final Selector definitionSelector) {
        if (definitionSelector instanceof SelectorHttp) {
            SelectorHttp definitionSelectorHttp = (SelectorHttp) definitionSelector;
            FlowHttpSelector repositoryFlowHttpSelector = new FlowHttpSelector();
            repositoryFlowHttpSelector.setMethods(definitionSelectorHttp.getMethods());
            repositoryFlowHttpSelector.setPath(definitionSelectorHttp.getPath());
            repositoryFlowHttpSelector.setPathOperator(FlowOperator.valueOf(definitionSelectorHttp.getPathOperator().name()));
            return repositoryFlowHttpSelector;
        } else if (definitionSelector instanceof SelectorChannel) {
            SelectorChannel definitionSelectorChannel = (SelectorChannel) definitionSelector;
            FlowChannelSelector repositoryFlowChannelSelector = new FlowChannelSelector();
            repositoryFlowChannelSelector.setChannel(definitionSelectorChannel.getChannel());
            repositoryFlowChannelSelector.setChannelOperator(FlowOperator.valueOf(definitionSelectorChannel.getChannelOperator().name()));
            repositoryFlowChannelSelector.setOperations(
                definitionSelectorChannel
                    .getOperations()
                    .stream()
                    .map(operation -> FlowChannelSelector.Operation.valueOf(operation.name()))
                    .collect(Collectors.toSet())
            );
            return repositoryFlowChannelSelector;
        } else if (definitionSelector instanceof SelectorCondition) {
            SelectorCondition definitionSelectorCondition = (SelectorCondition) definitionSelector;
            FlowConditionSelector repositoryFlowConditionSelector = new FlowConditionSelector();
            repositoryFlowConditionSelector.setCondition(definitionSelectorCondition.getCondition());
            return repositoryFlowConditionSelector;
        }
        throw new IllegalArgumentException(String.format("Unsupported definitionSelector %s", definitionSelector));
    }

    private Selector toDefinition(final FlowSelector repositoryFlowSelector) {
        if (repositoryFlowSelector instanceof FlowHttpSelector) {
            FlowHttpSelector repositoryFlowHttpSelector = (FlowHttpSelector) repositoryFlowSelector;
            SelectorHttp definitionSelectorHttp = new SelectorHttp();
            definitionSelectorHttp.setMethods(repositoryFlowHttpSelector.getMethods());
            definitionSelectorHttp.setPath(repositoryFlowHttpSelector.getPath());
            definitionSelectorHttp.setPathOperator(Operator.valueOf(repositoryFlowHttpSelector.getPathOperator().name()));
            return definitionSelectorHttp;
        } else if (repositoryFlowSelector instanceof FlowChannelSelector) {
            FlowChannelSelector repositoryFlowChannelSelector = (FlowChannelSelector) repositoryFlowSelector;
            SelectorChannel definitionSelectorChannel = new SelectorChannel();
            definitionSelectorChannel.setChannel(repositoryFlowChannelSelector.getChannel());
            definitionSelectorChannel.setChannelOperator(Operator.valueOf(repositoryFlowChannelSelector.getChannelOperator().name()));
            definitionSelectorChannel.setOperations(
                repositoryFlowChannelSelector
                    .getOperations()
                    .stream()
                    .map(operation -> SelectorChannel.Operation.valueOf(operation.name()))
                    .collect(Collectors.toSet())
            );
            return definitionSelectorChannel;
        } else if (repositoryFlowSelector instanceof FlowConditionSelector) {
            FlowConditionSelector repositoryFlowConditionSelector = (FlowConditionSelector) repositoryFlowSelector;
            SelectorCondition definitionSelectorCondition = new SelectorCondition();
            definitionSelectorCondition.setCondition(repositoryFlowConditionSelector.getCondition());
            return definitionSelectorCondition;
        }
        throw new IllegalArgumentException(String.format("Unsupported flow selector %s", repositoryFlowSelector));
    }
}
