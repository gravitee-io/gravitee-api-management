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
package io.gravitee.apim.rest.api.automation.mapper;

import static java.util.Comparator.comparingInt;

import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.model.ChannelSelector;
import io.gravitee.apim.rest.api.automation.model.ConditionSelector;
import io.gravitee.apim.rest.api.automation.model.Cors;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.FlowV4;
import io.gravitee.apim.rest.api.automation.model.HttpListener;
import io.gravitee.apim.rest.api.automation.model.HttpSelector;
import io.gravitee.apim.rest.api.automation.model.KafkaListener;
import io.gravitee.apim.rest.api.automation.model.LegacyAPIV4Spec;
import io.gravitee.apim.rest.api.automation.model.Listener;
import io.gravitee.apim.rest.api.automation.model.McpSelector;
import io.gravitee.apim.rest.api.automation.model.PageV4;
import io.gravitee.apim.rest.api.automation.model.PlanV4;
import io.gravitee.apim.rest.api.automation.model.ResponseTemplate;
import io.gravitee.apim.rest.api.automation.model.Selector;
import io.gravitee.apim.rest.api.automation.model.SubscriptionListener;
import io.gravitee.apim.rest.api.automation.model.TcpListener;
import io.gravitee.rest.api.management.v2.rest.mapper.DateMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OriginContextMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec;
import io.gravitee.rest.api.management.v2.rest.model.PageCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanCRD;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { DateMapper.class, OriginContextMapper.class, ServiceMapper.class })
public interface ApiMapper {
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    @Mapping(target = "listeners", expression = "java(mapApiV4SpecListeners(apiV4Spec))")
    @Mapping(target = "plans", expression = "java(mapApiV4SpecPlans(apiV4Spec))")
    @Mapping(target = "pages", expression = "java(mapApiV4SpecPages(apiV4Spec))")
    ApiCRDSpec apiV4SpecToApiCRDSpec(LegacyAPIV4Spec apiV4Spec);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "crossId", source = "crossId")
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "environmentId", source = "environmentId")
    ApiV4State apiV4SpecToApiV4State(ApiV4Spec apiV4Spec, String id, String crossId, String organizationId, String environmentId);

    @Mapping(target = "selectors", expression = "java(mapApiV4SpecSelectors(flowV4))")
    io.gravitee.rest.api.management.v2.rest.model.FlowV4 map(FlowV4 flowV4);

    @Mapping(target = "selectors", expression = "java(mapApiCRDSpecSelectors(flowV4))")
    FlowV4 map(io.gravitee.rest.api.management.v2.rest.model.FlowV4 flowV4);

    @Mapping(target = "id", source = "status.id")
    @Mapping(target = "crossId", source = "status.crossId")
    @Mapping(target = "errors", source = "status.errors")
    @Mapping(target = "plans", source = "spec.plans")
    @Mapping(target = "state", source = "spec.state")
    @Mapping(target = "organizationId", source = "status.organizationId")
    @Mapping(target = "environmentId", source = "status.environmentId")
    ApiV4State apiV4SpecAndStatusToApiV4State(ApiV4Spec spec, ApiCRDStatus status);

    Errors toErrors(ApiCRDStatus.Errors apiV4State);

    @Mapping(target = "listeners", expression = "java(mapApiCRDSpecListeners(apiCRD))")
    @Mapping(target = "plans", expression = "java(mapApiCRDSpecPlans(apiCRD))")
    @Mapping(target = "pages", expression = "java(mapApiCRDSpecPages(apiCRD))")
    ApiV4Spec apiCRDSpecToApiV4Spec(ApiCRDSpec apiCRD);

    @Mapping(target = "statusCode", source = "status")
    io.gravitee.definition.model.ResponseTemplate mapResponseTemplate(ResponseTemplate src);

    @Mapping(target = "status", source = "statusCode")
    ResponseTemplate mapResponseTemplate(io.gravitee.definition.model.ResponseTemplate src);

    Map<String, io.gravitee.definition.model.ResponseTemplate> mapToModelResponseTemplate(Map<String, ResponseTemplate> value);

    Map<String, ResponseTemplate> mapToAutomationResponseTemplate(Map<String, io.gravitee.definition.model.ResponseTemplate> value);

    io.gravitee.rest.api.management.v2.rest.model.HttpListener map(HttpListener listener);

    default Cors.AllowMethodsEnum map(String value) {
        if ("*".equals(value)) {
            return Cors.AllowMethodsEnum.STAR;
        }
        return Cors.AllowMethodsEnum.fromValue(value);
    }

    default String map(Cors.AllowMethodsEnum value) {
        if (value == Cors.AllowMethodsEnum.STAR) {
            return "*";
        }
        return value.getValue();
    }

    io.gravitee.rest.api.management.v2.rest.model.TcpListener map(TcpListener listener);

    io.gravitee.rest.api.management.v2.rest.model.KafkaListener map(KafkaListener listener);

    io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener map(SubscriptionListener listener);

    HttpListener map(io.gravitee.rest.api.management.v2.rest.model.HttpListener listener);

    TcpListener map(io.gravitee.rest.api.management.v2.rest.model.TcpListener listener);

    KafkaListener map(io.gravitee.rest.api.management.v2.rest.model.KafkaListener listener);

    SubscriptionListener map(io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener listener);

    io.gravitee.rest.api.management.v2.rest.model.HttpSelector map(HttpSelector selector);

    io.gravitee.rest.api.management.v2.rest.model.ChannelSelector map(ChannelSelector selector);

    io.gravitee.rest.api.management.v2.rest.model.ConditionSelector map(ConditionSelector selector);

    io.gravitee.rest.api.management.v2.rest.model.McpSelector map(McpSelector selector);

    HttpSelector map(io.gravitee.rest.api.management.v2.rest.model.HttpSelector selector);

    ChannelSelector map(io.gravitee.rest.api.management.v2.rest.model.ChannelSelector selector);

    ConditionSelector map(io.gravitee.rest.api.management.v2.rest.model.ConditionSelector selector);

    McpSelector map(io.gravitee.rest.api.management.v2.rest.model.McpSelector selector);

    PlanCRD map(PlanV4 planV4, int order);

    PageCRD map(PageV4 pageV4, int order);

    PlanV4 map(PlanCRD planCRD);

    PageV4 map(PageCRD pageCRD);

    default io.gravitee.apim.rest.api.automation.model.PlanSecurityType map(
        io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType type
    ) {
        if (type == null) {
            return null;
        }
        return io.gravitee.apim.rest.api.automation.model.PlanSecurityType.valueOf(type.name());
    }

    default List<io.gravitee.rest.api.management.v2.rest.model.Selector> mapApiV4SpecSelectors(FlowV4 flowV4) {
        if (flowV4 == null || flowV4.getSelectors() == null) {
            return List.of();
        }

        return flowV4
            .getSelectors()
            .stream()
            .map(selector -> {
                if (selector.getActualInstance() instanceof HttpSelector httpSelector) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Selector(map(httpSelector));
                } else if (selector.getActualInstance() instanceof ChannelSelector channelSelector) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Selector(map(channelSelector));
                } else if (selector.getActualInstance() instanceof ConditionSelector conditionSelector) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Selector(map(conditionSelector));
                }

                return null;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    default List<Selector> mapApiCRDSpecSelectors(io.gravitee.rest.api.management.v2.rest.model.FlowV4 flowV4) {
        if (flowV4 == null || flowV4.getSelectors() == null) {
            return List.of();
        }

        return flowV4
            .getSelectors()
            .stream()
            .map(selector -> {
                if (selector.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.HttpSelector httpSelector) {
                    return new Selector(map(httpSelector));
                } else if (
                    selector.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.ChannelSelector channelSelector
                ) {
                    return new Selector(map(channelSelector));
                } else if (
                    selector.getActualInstance() instanceof
                        io.gravitee.rest.api.management.v2.rest.model.ConditionSelector conditionSelector
                ) {
                    return new Selector(map(conditionSelector));
                }

                return null;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    default List<io.gravitee.rest.api.management.v2.rest.model.Listener> mapApiV4SpecListeners(ApiV4Spec apiV4Spec) {
        return apiV4Spec
            .getListeners()
            .stream()
            .map(listener -> {
                if (listener.getActualInstance() instanceof HttpListener) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(map(listener.getHttpListener()));
                } else if (listener.getActualInstance() instanceof TcpListener) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(map(listener.getTcpListener()));
                } else if (listener.getActualInstance() instanceof KafkaListener) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(map(listener.getKafkaListener()));
                } else if (listener.getActualInstance() instanceof SubscriptionListener) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(map(listener.getSubscriptionListener()));
                }

                return null;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    default Map<String, PlanCRD> mapApiV4SpecPlans(ApiV4Spec apiV4Spec) {
        if (apiV4Spec == null || apiV4Spec.getPlans() == null) {
            return Map.of();
        }

        Map<String, PlanCRD> plans = new LinkedHashMap<>();
        for (int i = 0; i < apiV4Spec.getPlans().size(); i++) {
            PlanV4 planV4 = apiV4Spec.getPlans().get(i);
            plans.put(planV4.getHrid(), map(planV4, i));
        }
        return plans;
    }

    default List<PlanV4> mapApiCRDSpecPlans(ApiCRDSpec apiCRD) {
        if (apiCRD == null || apiCRD.getPlans() == null) {
            return List.of();
        }

        Comparator<PlanCRD> comparator = comparingInt(PlanCRD::getOrder).thenComparing(PlanCRD::getStatus);
        return apiCRD.getPlans().values().stream().sorted(comparator).map(this::map).toList();
    }

    default Map<String, PageCRD> mapApiV4SpecPages(ApiV4Spec apiV4Spec) {
        if (apiV4Spec == null || apiV4Spec.getPages() == null) {
            return Map.of();
        }

        Map<String, PageCRD> pages = new LinkedHashMap<>();
        for (int i = 0; i < apiV4Spec.getPages().size(); i++) {
            PageV4 pageV4 = apiV4Spec.getPages().get(i);
            pages.put(pageV4.getHrid(), map(pageV4, i));
        }
        return pages;
    }

    default List<PageV4> mapApiCRDSpecPages(ApiCRDSpec apiCRD) {
        if (apiCRD == null || apiCRD.getPages() == null) {
            return List.of();
        }

        return apiCRD
            .getPages()
            .entrySet()
            .stream()
            .map(entry -> {
                PageV4 pageV4 = map(entry.getValue());
                pageV4.setHrid(entry.getKey());
                return pageV4;
            })
            .toList();
    }

    default List<Listener> mapApiCRDSpecListeners(ApiCRDSpec apiCRDSpec) {
        return apiCRDSpec
            .getListeners()
            .stream()
            .map(listener -> {
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.HttpListener) {
                    return new Listener(map(listener.getHttpListener()));
                } else if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.TcpListener) {
                    return new Listener(map(listener.getTcpListener()));
                } else if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.KafkaListener) {
                    return new Listener(map(listener.getKafkaListener()));
                } else if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener) {
                    return new Listener(map(listener.getSubscriptionListener()));
                }

                return null;
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
