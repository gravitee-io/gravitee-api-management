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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface FlowMapper {
    FlowMapper INSTANCE = Mappers.getMapper(FlowMapper.class);

    // Flow V4
    @Mapping(target = "selectors", qualifiedByName = "fromSelectors")
    FlowV4 mapFromFlowEntityV4(io.gravitee.definition.model.v4.flow.Flow flow);

    List<FlowV4> mapFromFlowEntityV4List(List<io.gravitee.definition.model.v4.flow.Flow> flow);

    @Mapping(target = "selectors", qualifiedByName = "toSelectors")
    io.gravitee.definition.model.v4.flow.Flow mapToFlowEntityV4(FlowV4 flow);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    StepV4 mapFromStepEntityV4(Step step);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    Step mapToStepEntityV4(StepV4 stepV4);

    // Selectors
    HttpSelector map(io.gravitee.definition.model.v4.flow.selector.HttpSelector selector);
    ConditionSelector map(io.gravitee.definition.model.v4.flow.selector.ConditionSelector selector);
    ChannelSelector map(io.gravitee.definition.model.v4.flow.selector.ChannelSelector selector);

    io.gravitee.definition.model.v4.flow.selector.HttpSelector map(HttpSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ConditionSelector map(ConditionSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ChannelSelector map(ChannelSelector selector);

    @Named("toSelectors")
    default List<io.gravitee.definition.model.v4.flow.selector.Selector> mapToSelectorEntityV4List(List<Selector> selectors) {
        if (Objects.isNull(selectors)) {
            return new ArrayList<>();
        }
        return selectors
            .stream()
            .map(selector -> {
                if (selector == null) {
                    return null;
                }
                if (selector.getActualInstance() instanceof HttpSelector) {
                    return this.map(selector.getHttpSelector());
                }
                if (selector.getActualInstance() instanceof ConditionSelector) {
                    return this.map(selector.getConditionSelector());
                }
                if (selector.getActualInstance() instanceof ChannelSelector) {
                    return this.map(selector.getChannelSelector());
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("fromSelectors")
    default List<Selector> mapFromSelectorEntityV4List(List<io.gravitee.definition.model.v4.flow.selector.Selector> selectors) {
        if (Objects.isNull(selectors)) {
            return new ArrayList<>();
        }
        return selectors
            .stream()
            .map(selector -> {
                if (selector == null) {
                    return null;
                }
                if (selector.getType() == SelectorType.HTTP) {
                    return new Selector(this.map((io.gravitee.definition.model.v4.flow.selector.HttpSelector) selector));
                }
                if (selector.getType() == SelectorType.CONDITION) {
                    return new Selector(this.map((io.gravitee.definition.model.v4.flow.selector.ConditionSelector) selector));
                }
                if (selector.getType() == SelectorType.CHANNEL) {
                    return new Selector(this.map((io.gravitee.definition.model.v4.flow.selector.ChannelSelector) selector));
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    // Flow V2
    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.flow.Step mapToStepEntityV2(StepV2 stepV2);

    io.gravitee.definition.model.flow.Flow mapToFlowEntityV2(FlowV2 flowV2);
    List<io.gravitee.definition.model.flow.Flow> mapToFlowEntityV2List(List<FlowV2> flowV2List);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    StepV2 mapFromStepEntityV2(io.gravitee.definition.model.flow.Step stepV2);

    FlowV2 mapFromFlowEntityV2(io.gravitee.definition.model.flow.Flow flowV2);
    List<FlowV2> mapFromFlowEntityV2List(List<io.gravitee.definition.model.flow.Flow> flowV2List);
}
