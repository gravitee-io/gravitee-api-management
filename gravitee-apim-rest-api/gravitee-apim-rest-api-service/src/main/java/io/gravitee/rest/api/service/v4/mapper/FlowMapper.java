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

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("FlowMapperV4")
public class FlowMapper {

    private static final String FLOW_TO_MAP_CANNOT_BE_NULL = "Flow to map cannot be null";

    public Flow toDefinition(io.gravitee.repository.management.model.flow.Flow repositoryFlow) {
        if (repositoryFlow == null) {
            throw new IllegalArgumentException(FLOW_TO_MAP_CANNOT_BE_NULL);
        }
        Flow definitionFlow = new Flow();
        definitionFlow.setId(repositoryFlow.getId());
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

    public NativeFlow toNativeDefinition(io.gravitee.repository.management.model.flow.Flow repositoryFlow) {
        if (repositoryFlow == null) {
            throw new IllegalArgumentException(FLOW_TO_MAP_CANNOT_BE_NULL);
        }
        NativeFlow definitionFlow = new NativeFlow();
        definitionFlow.setId(repositoryFlow.getId());
        definitionFlow.setName(repositoryFlow.getName());
        definitionFlow.setEnabled(repositoryFlow.isEnabled());
        definitionFlow.setPublish(repositoryFlow.getPublish().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setSubscribe(repositoryFlow.getSubscribe().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setInteract(repositoryFlow.getInteract().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setConnect(repositoryFlow.getConnect().stream().map(this::toDefinition).collect(Collectors.toList()));
        definitionFlow.setTags(repositoryFlow.getTags());
        return definitionFlow;
    }

    public io.gravitee.repository.management.model.flow.Flow toRepository(
        final Flow definitionFlow,
        final FlowReferenceType referenceType,
        final String referenceId,
        final int order
    ) {
        if (definitionFlow == null) {
            throw new IllegalArgumentException(FLOW_TO_MAP_CANNOT_BE_NULL);
        }
        io.gravitee.repository.management.model.flow.Flow repositoryFlow = new io.gravitee.repository.management.model.flow.Flow();
        repositoryFlow.setId(UuidString.generateRandom());
        repositoryFlow.setName(definitionFlow.getName());
        repositoryFlow.setEnabled(definitionFlow.isEnabled());
        repositoryFlow.setCreatedAt(new Date());
        repositoryFlow.setUpdatedAt(repositoryFlow.getCreatedAt());
        repositoryFlow.setOrder(order);
        repositoryFlow.setReferenceType(referenceType);
        repositoryFlow.setReferenceId(referenceId);

        if (definitionFlow.getRequest() != null) {
            repositoryFlow.setRequest(definitionFlow.getRequest().stream().map(this::toRepository).collect(Collectors.toList()));
        }
        if (definitionFlow.getResponse() != null) {
            repositoryFlow.setResponse(definitionFlow.getResponse().stream().map(this::toRepository).collect(Collectors.toList()));
        }
        if (definitionFlow.getPublish() != null) {
            repositoryFlow.setPublish(definitionFlow.getPublish().stream().map(this::toRepository).collect(Collectors.toList()));
        }
        if (definitionFlow.getSubscribe() != null) {
            repositoryFlow.setSubscribe(definitionFlow.getSubscribe().stream().map(this::toRepository).collect(Collectors.toList()));
        }
        if (definitionFlow.getSelectors() != null) {
            repositoryFlow.setSelectors(definitionFlow.getSelectors().stream().map(this::toRepository).collect(Collectors.toList()));
        }
        repositoryFlow.setTags(definitionFlow.getTags() != null ? definitionFlow.getTags() : Set.of());

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
        repositoryStep.setMessageCondition(definitionStep.getMessageCondition());
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
        definitionStep.setMessageCondition(repositoryStep.getMessageCondition());
        return definitionStep;
    }

    private FlowSelector toRepository(final Selector definitionSelector) {
        if (definitionSelector instanceof HttpSelector) {
            HttpSelector definitionHttpSelector = (HttpSelector) definitionSelector;
            FlowHttpSelector repositoryFlowHttpSelector = new FlowHttpSelector();
            repositoryFlowHttpSelector.setMethods(definitionHttpSelector.getMethods());
            repositoryFlowHttpSelector.setPath(definitionHttpSelector.getPath());
            repositoryFlowHttpSelector.setPathOperator(FlowOperator.valueOf(definitionHttpSelector.getPathOperator().name()));
            return repositoryFlowHttpSelector;
        } else if (definitionSelector instanceof ChannelSelector) {
            ChannelSelector definitionChannelSelector = (ChannelSelector) definitionSelector;
            FlowChannelSelector repositoryFlowChannelSelector = new FlowChannelSelector();
            repositoryFlowChannelSelector.setChannel(definitionChannelSelector.getChannel());
            repositoryFlowChannelSelector.setChannelOperator(FlowOperator.valueOf(definitionChannelSelector.getChannelOperator().name()));
            if (definitionChannelSelector.getOperations() != null) {
                repositoryFlowChannelSelector.setOperations(
                    definitionChannelSelector
                        .getOperations()
                        .stream()
                        .map(operation -> FlowChannelSelector.Operation.valueOf(operation.name()))
                        .collect(Collectors.toSet())
                );
            }
            repositoryFlowChannelSelector.setEntrypoints(definitionChannelSelector.getEntrypoints());
            return repositoryFlowChannelSelector;
        } else if (definitionSelector instanceof ConditionSelector) {
            ConditionSelector definitionConditionChannel = (ConditionSelector) definitionSelector;
            FlowConditionSelector repositoryFlowConditionSelector = new FlowConditionSelector();
            repositoryFlowConditionSelector.setCondition(definitionConditionChannel.getCondition());
            return repositoryFlowConditionSelector;
        }
        throw new IllegalArgumentException(String.format("Unsupported definitionSelector %s", definitionSelector));
    }

    private Selector toDefinition(final FlowSelector repositoryFlowSelector) {
        if (repositoryFlowSelector instanceof FlowHttpSelector) {
            FlowHttpSelector repositoryFlowHttpSelector = (FlowHttpSelector) repositoryFlowSelector;
            HttpSelector definitionHttpSelector = new HttpSelector();
            definitionHttpSelector.setMethods(repositoryFlowHttpSelector.getMethods());
            definitionHttpSelector.setPath(repositoryFlowHttpSelector.getPath());
            definitionHttpSelector.setPathOperator(Operator.valueOf(repositoryFlowHttpSelector.getPathOperator().name()));
            return definitionHttpSelector;
        } else if (repositoryFlowSelector instanceof FlowChannelSelector) {
            FlowChannelSelector repositoryFlowChannelSelector = (FlowChannelSelector) repositoryFlowSelector;
            ChannelSelector definitionChannelSelector = new ChannelSelector();
            definitionChannelSelector.setChannel(repositoryFlowChannelSelector.getChannel());
            definitionChannelSelector.setChannelOperator(Operator.valueOf(repositoryFlowChannelSelector.getChannelOperator().name()));
            if (repositoryFlowChannelSelector.getOperations() != null) {
                definitionChannelSelector.setOperations(
                    repositoryFlowChannelSelector
                        .getOperations()
                        .stream()
                        .map(operation -> ChannelSelector.Operation.valueOf(operation.name()))
                        .collect(Collectors.toSet())
                );
            }
            definitionChannelSelector.setEntrypoints(repositoryFlowChannelSelector.getEntrypoints());
            return definitionChannelSelector;
        } else if (repositoryFlowSelector instanceof FlowConditionSelector) {
            FlowConditionSelector repositoryFlowConditionSelector = (FlowConditionSelector) repositoryFlowSelector;
            ConditionSelector definitionConditionChannel = new ConditionSelector();
            definitionConditionChannel.setCondition(repositoryFlowConditionSelector.getCondition());
            return definitionConditionChannel;
        }
        throw new IllegalArgumentException(String.format("Unsupported flow selector %s", repositoryFlowSelector));
    }
}
