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

import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec.ConsumerConfiguration;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.SubscriptionConsumerConfiguration;
import io.gravitee.apim.rest.api.automation.model.SubscriptionSpec;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.rest.api.management.v2.rest.mapper.ConfigurationSerializationMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.DateMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OriginContextMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { DateMapper.class, OriginContextMapper.class, ServiceMapper.class, ConfigurationSerializationMapper.class })
public interface SubscriptionMapper {
    SubscriptionMapper INSTANCE = Mappers.getMapper(SubscriptionMapper.class);

    default SubscriptionState subscriptionSpecAndStatusToSubscriptionState(
        String apiHrid,
        SubscriptionSpec spec,
        SubscriptionCRDStatus status
    ) {
        var state = new SubscriptionState(
            status.getId(),
            status.getEnvironmentId(),
            status.getOrganizationId(),
            toErrors(status.getErrors()),
            apiHrid,
            status.getStartingAt() != null ? status.getStartingAt().toOffsetDateTime() : null
        );
        mapSpecToState(spec, state);
        state.setEndingAt(status.getEndingAt() != null ? status.getEndingAt().toOffsetDateTime() : null);
        return state;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "apiHrid", ignore = true)
    @Mapping(target = "startingAt", ignore = true)
    @Mapping(target = "endingAt", ignore = true)
    void mapSpecToState(SubscriptionSpec spec, @MappingTarget SubscriptionState state);

    @Mapping(target = "entrypointConfiguration", qualifiedByName = "deserializeConfiguration")
    SubscriptionConsumerConfiguration toSubscriptionConsumerConfiguration(SubscriptionConfiguration configuration);

    ConsumerConfiguration toConsumerConfiguration(SubscriptionConsumerConfiguration consumerConfiguration);

    Errors toErrors(SubscriptionCRDStatus.Errors errors);
}
