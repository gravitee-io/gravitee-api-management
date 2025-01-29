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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.management.v2.rest.model.ChannelSelector;
import io.gravitee.rest.api.management.v2.rest.model.ConditionSelector;
import io.gravitee.rest.api.management.v2.rest.model.FlowV2;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpSelector;
import io.gravitee.rest.api.management.v2.rest.model.Selector;
import io.gravitee.rest.api.management.v2.rest.model.StepV2;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface FlowMapper {
    FlowMapper INSTANCE = Mappers.getMapper(FlowMapper.class);
    GraviteeMapper JSON_MAPPER = new GraviteeMapper();

    // Flow V4
    @Mapping(target = "selectors", qualifiedByName = "mapToSelectorApiModelList")
    FlowV4 mapFromHttpV4(io.gravitee.definition.model.v4.flow.Flow flow);

    @Mapping(target = "selectors", qualifiedByName = "mapToSelectorEntityList")
    io.gravitee.definition.model.v4.flow.Flow mapToHttpV4(FlowV4 flow);

    FlowV4 mapFromNativeV4(io.gravitee.definition.model.v4.nativeapi.NativeFlow flow);

    io.gravitee.definition.model.v4.nativeapi.NativeFlow mapToNativeV4(FlowV4 flow);

    @Named("mapListToFlowHttpV4")
    List<io.gravitee.definition.model.v4.flow.Flow> mapToHttpV4(List<FlowV4> flows);

    @Named("mapListToFlowNativeV4")
    List<io.gravitee.definition.model.v4.nativeapi.NativeFlow> mapToNativeV4(List<FlowV4> flows);

    List<FlowV4> mapFromHttpV4(List<io.gravitee.definition.model.v4.flow.Flow> flow);
    List<FlowV4> mapFromNativeV4(List<io.gravitee.definition.model.v4.nativeapi.NativeFlow> flow);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV4 mapStep(Step step);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Step mapStep(StepV4 stepV4);

    // Selectors
    HttpSelector mapSelector(io.gravitee.definition.model.v4.flow.selector.HttpSelector selector);
    ConditionSelector mapSelector(io.gravitee.definition.model.v4.flow.selector.ConditionSelector selector);
    ChannelSelector mapSelector(io.gravitee.definition.model.v4.flow.selector.ChannelSelector selector);

    io.gravitee.definition.model.v4.flow.selector.HttpSelector mapSelector(HttpSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ConditionSelector mapSelector(ConditionSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ChannelSelector mapSelector(ChannelSelector selector);

    @Named("mapToSelectorEntityList")
    default List<io.gravitee.definition.model.v4.flow.selector.Selector> mapToSelectorEntityList(List<Selector> selectors) {
        return stream(selectors)
            .filter(Objects::nonNull)
            .flatMap(selector ->
                switch (selector.getActualInstance()) {
                    case HttpSelector ignored -> Stream.ofNullable(mapSelector(selector.getHttpSelector()));
                    case ConditionSelector ignored -> Stream.ofNullable(mapSelector(selector.getConditionSelector()));
                    case ChannelSelector ignored -> Stream.ofNullable(mapSelector(selector.getChannelSelector()));
                    default -> Stream.empty();
                }
            )
            .toList();
    }

    @Named("mapToSelectorApiModelList")
    default List<Selector> mapToSelectorApiModelList(List<io.gravitee.definition.model.v4.flow.selector.Selector> selectors) {
        return stream(selectors)
            .flatMap(selector ->
                switch (selector) {
                    case io.gravitee.definition.model.v4.flow.selector.HttpSelector http -> Stream.ofNullable(
                        new Selector(mapSelector(http))
                    );
                    case io.gravitee.definition.model.v4.flow.selector.ConditionSelector condition -> Stream.ofNullable(
                        new Selector(mapSelector(condition))
                    );
                    case io.gravitee.definition.model.v4.flow.selector.ChannelSelector channel -> Stream.ofNullable(
                        new Selector(mapSelector(channel))
                    );
                    case null, default -> Stream.empty();
                }
            )
            .toList();
    }

    // Flow V2
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.flow.Step mapStep(StepV2 stepV2);

    io.gravitee.definition.model.flow.Flow map(FlowV2 flowV2);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV2 mapStep(io.gravitee.definition.model.flow.Step stepV2);

    FlowV2 map(io.gravitee.definition.model.flow.Flow flowV2);

    default List<? extends AbstractFlow> map(@Valid List<FlowV4> flows, Api api) {
        return ofNullable(api.isNative() ? mapToNativeV4(flows) : mapToHttpV4(flows)).orElseGet(List::of);
    }

    default <T extends AbstractFlow> FlowV4 map(T src) {
        return switch (src) {
            case Flow flow -> map(flow);
            case NativeFlow nativeFlow -> map(nativeFlow);
            default -> throw new IllegalStateException("Unexpected value: " + src);
        };
    }

    FlowV4 map(Flow src);

    FlowV4 map(NativeFlow src);

    @SneakyThrows
    @AfterMapping
    default void mapConfiguration(Object ignored, @MappingTarget StepV4 target) {
        if (target.getConfiguration() instanceof String conf) {
            target.setConfiguration(JSON_MAPPER.readTree(conf));
        }
    }

    default Selector map(io.gravitee.definition.model.v4.flow.selector.Selector src) {
        return switch (src) {
            case null -> null;
            case io.gravitee.definition.model.v4.flow.selector.ChannelSelector channel -> new Selector(map(channel));
            case io.gravitee.definition.model.v4.flow.selector.ConditionSelector condition -> new Selector(map(condition));
            case io.gravitee.definition.model.v4.flow.selector.HttpSelector http -> new Selector(map(http));
            default -> throw new IllegalStateException("Unexpected value: " + src);
        };
    }

    io.gravitee.rest.api.management.v2.rest.model.ChannelSelector map(
        io.gravitee.definition.model.v4.flow.selector.ChannelSelector channel
    );
    io.gravitee.rest.api.management.v2.rest.model.ConditionSelector map(
        io.gravitee.definition.model.v4.flow.selector.ConditionSelector condition
    );
    io.gravitee.rest.api.management.v2.rest.model.HttpSelector map(io.gravitee.definition.model.v4.flow.selector.HttpSelector http);
}
