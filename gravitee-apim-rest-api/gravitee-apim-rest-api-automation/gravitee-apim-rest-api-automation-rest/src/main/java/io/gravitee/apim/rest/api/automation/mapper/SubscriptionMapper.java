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

import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.SubscriptionSpec;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.rest.api.management.v2.rest.mapper.DateMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OriginContextMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { DateMapper.class, OriginContextMapper.class, ServiceMapper.class })
public interface SubscriptionMapper {
    SubscriptionMapper INSTANCE = Mappers.getMapper(SubscriptionMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "environmentId", source = "environmentId")
    SubscriptionState subscriptionsSpecToSubscriptionsState(
        SubscriptionSpec subscriptionSpec,
        String id,
        String organizationId,
        String environmentId
    );

    @Mapping(target = "id", source = "status.id")
    @Mapping(target = "errors", source = "status.errors")
    @Mapping(target = "organizationId", source = "status.organizationId")
    @Mapping(target = "environmentId", source = "status.environmentId")
    @Mapping(target = "startingAt", source = "status.startingAt")
    @Mapping(target = "endingAt", source = "status.endingAt")
    SubscriptionState subscriptionSpecAndStatusToSubscriptionState(SubscriptionSpec spec, SubscriptionCRDStatus status);

    Errors toErrors(SubscriptionCRDStatus.Errors errors);
}
