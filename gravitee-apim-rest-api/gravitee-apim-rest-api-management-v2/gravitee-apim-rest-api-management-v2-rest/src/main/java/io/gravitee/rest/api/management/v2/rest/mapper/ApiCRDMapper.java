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

import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec;
import io.gravitee.rest.api.management.v2.rest.model.ApiLifecycleState;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.PageCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = {
        AnalyticsMapper.class,
        DateMapper.class,
        ConfigurationSerializationMapper.class,
        EndpointMapper.class,
        EntrypointMapper.class,
        FlowMapper.class,
        ListenerMapper.class,
        PropertiesMapper.class,
        ResourceMapper.class,
        ResponseTemplateMapper.class,
        RuleMapper.class,
        ServiceMapper.class,
        CorsMapper.class,
    }
)
public interface ApiCRDMapper {
    ApiCRDMapper INSTANCE = Mappers.getMapper(ApiCRDMapper.class);

    @Mapping(target = "lifecycleState", qualifiedByName = "mapLifecycleState")
    ApiCRDSpec map(io.gravitee.apim.core.api.model.crd.ApiCRDSpec coreSpec);

    @Mapping(target = "security.type", qualifiedByName = "mapSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    PlanCRD map(io.gravitee.apim.core.api.model.crd.PlanCRD plan);

    @Mapping(target = "source.configuration", qualifiedByName = "deserializeConfiguration")
    PageCRD map(io.gravitee.apim.core.api.model.crd.PageCRD page);

    Listener map(io.gravitee.definition.model.v4.listener.Listener listener);

    @Named("mapSecurityType")
    default PlanSecurityType mapSecurityType(String securityType) {
        return PlanSecurityType.valueOf(io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOfLabel(securityType).name());
    }

    @Named("mapLifecycleState")
    default ApiLifecycleState mapLifecycleState(String lifecycleState) {
        return ApiLifecycleState.PUBLISHED.name().equals(lifecycleState) ? ApiLifecycleState.PUBLISHED : ApiLifecycleState.UNPUBLISHED;
    }

    default List<Listener> mapAbstractListeners(List<? extends AbstractListener<? extends AbstractEntrypoint>> listeners) {
        if (CollectionUtils.isEmpty(listeners)) {
            return List.of();
        }

        if (listeners.getFirst() instanceof NativeListener) {
            return ListenerMapper.INSTANCE.mapFromNativeListenerV4List((List<NativeListener>) listeners);
        } else if (listeners.getFirst() instanceof io.gravitee.definition.model.v4.listener.Listener) {
            return ListenerMapper.INSTANCE.mapFromListenerEntityV4List((List<io.gravitee.definition.model.v4.listener.Listener>) listeners);
        }

        return List.of();
    }

    default EndpointV4 mapAbstractEndpoint(AbstractEndpoint abstractEndpoint) {
        return switch (abstractEndpoint) {
            case NativeEndpoint nativeEndpoint -> EndpointMapper.INSTANCE.mapFromNativeV4(nativeEndpoint);
            case Endpoint endpoint -> EndpointMapper.INSTANCE.mapFromHttpV4(endpoint);
            case null, default -> null;
        };
    }

    default EndpointGroupV4 mapAbstractEndpointGroups(AbstractEndpointGroup<? extends AbstractEndpoint> abstractEndpointGroup) {
        return switch (abstractEndpointGroup) {
            case NativeEndpointGroup nativeEndpointGroup -> EndpointMapper.INSTANCE.mapEndpointGroupNativeV4(nativeEndpointGroup);
            case EndpointGroup endpointGroup -> EndpointMapper.INSTANCE.mapEndpointGroupHttpV4(endpointGroup);
            case null, default -> null;
        };
    }

    default io.gravitee.rest.api.management.v2.rest.model.FlowV4 mapAbstractFlow(AbstractFlow abstractFlow) {
        return switch (abstractFlow) {
            case NativeFlow nativeFlow -> FlowMapper.INSTANCE.mapFromNativeV4(nativeFlow);
            case FlowV4Impl flow -> FlowMapper.INSTANCE.mapFromHttpV4(flow);
            case null, default -> null;
        };
    }
}
