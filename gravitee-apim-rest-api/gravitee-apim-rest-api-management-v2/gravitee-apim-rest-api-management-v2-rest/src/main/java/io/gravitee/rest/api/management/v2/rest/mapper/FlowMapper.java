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
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.flow.step.StepV4;
import io.gravitee.definition.model.v4.nativeapi.NativeFlowImpl;
import io.gravitee.rest.api.management.v2.rest.model.ChannelSelector;
import io.gravitee.rest.api.management.v2.rest.model.ConditionSelector;
import io.gravitee.rest.api.management.v2.rest.model.FlowV2;
import io.gravitee.rest.api.management.v2.rest.model.HttpSelector;
import io.gravitee.rest.api.management.v2.rest.model.Selector;
import io.gravitee.rest.api.management.v2.rest.model.StepV2;
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
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class }, nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface FlowMapper {
    FlowMapper INSTANCE = Mappers.getMapper(FlowMapper.class);
    GraviteeMapper JSON_MAPPER = new GraviteeMapper();

    // Flow V4
    @Mapping(target = "selectors", qualifiedByName = "mapToSelectorApiModelList")
    io.gravitee.rest.api.management.v2.rest.model.FlowV4 mapFromHttpV4(FlowV4Impl flow);

    @Mapping(target = "selectors", qualifiedByName = "mapToSelectorEntityList")
    @Mapping(target = "request", source = "request", qualifiedByName = "mapStepV4")
    @Mapping(target = "response", source = "response", qualifiedByName = "mapStepV4")
    @Mapping(target = "subscribe", source = "subscribe", qualifiedByName = "mapStepV4")
    @Mapping(target = "publish", source = "publish", qualifiedByName = "mapStepV4")
    FlowV4Impl mapToHttpV4(io.gravitee.rest.api.management.v2.rest.model.FlowV4 flow);

    io.gravitee.rest.api.management.v2.rest.model.FlowV4 mapFromNativeV4(NativeFlowImpl flow);

    @Mapping(target = "connect", source = "connect", qualifiedByName = "mapStepV4")
    @Mapping(target = "interact", source = "interact", qualifiedByName = "mapStepV4")
    @Mapping(target = "subscribe", source = "subscribe", qualifiedByName = "mapStepV4")
    @Mapping(target = "publish", source = "publish", qualifiedByName = "mapStepV4")
    NativeFlowImpl mapToNativeV4(io.gravitee.rest.api.management.v2.rest.model.FlowV4 flow);

    @Named("mapListToFlowHttpV4")
    List<FlowV4Impl> mapToHttpV4(List<io.gravitee.rest.api.management.v2.rest.model.FlowV4> flows);

    @Named("mapListToFlowNativeV4")
    List<NativeFlowImpl> mapToNativeV4(List<io.gravitee.rest.api.management.v2.rest.model.FlowV4> flows);

    List<io.gravitee.rest.api.management.v2.rest.model.FlowV4> mapFromHttpV4(List<FlowV4Impl> flow);
    List<io.gravitee.rest.api.management.v2.rest.model.FlowV4> mapFromNativeV4(List<NativeFlowImpl> flow);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.rest.api.management.v2.rest.model.StepV4 mapStep(StepV4 step);

    @Named("mapStepV4")
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    StepV4 mapStep(io.gravitee.rest.api.management.v2.rest.model.StepV4 stepV4);

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
    io.gravitee.definition.model.flow.StepV2 mapStep(StepV2 stepV2);

    FlowV2Impl map(FlowV2 flowV2);

    @Named("mapStepV2")
    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    StepV2 mapStepV2(io.gravitee.definition.model.flow.Step source);

    @Mapping(target = "pre", source = "pre", qualifiedByName = "mapStepV2")
    @Mapping(target = "post", source = "post", qualifiedByName = "mapStepV2")
    FlowV2 map(FlowV2Impl flowV2);

    default List<? extends Flow> map(@Valid List<io.gravitee.rest.api.management.v2.rest.model.FlowV4> flows, Api api) {
        return ofNullable(api.isNative() ? mapToNativeV4(flows) : mapToHttpV4(flows)).orElseGet(List::of);
    }

    default <T extends Flow> io.gravitee.rest.api.management.v2.rest.model.FlowV4 map(T src) {
        return switch (src) {
            case FlowV4Impl flow -> map(flow);
            case NativeFlowImpl nativeFlow -> map(nativeFlow);
            default -> throw new IllegalStateException("Unexpected value: " + src);
        };
    }

    io.gravitee.rest.api.management.v2.rest.model.FlowV4 map(FlowV4Impl src);

    io.gravitee.rest.api.management.v2.rest.model.FlowV4 map(NativeFlowImpl src);

    @SneakyThrows
    @AfterMapping
    default void mapConfiguration(Object ignored, @MappingTarget io.gravitee.rest.api.management.v2.rest.model.StepV4 target) {
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
