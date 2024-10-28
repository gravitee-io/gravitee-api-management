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

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec;
import io.gravitee.rest.api.management.v2.rest.model.ApiLifecycleState;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.PageCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
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

    @Mapping(target = "listeners", qualifiedByName = "fromHttpListeners")
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
}
