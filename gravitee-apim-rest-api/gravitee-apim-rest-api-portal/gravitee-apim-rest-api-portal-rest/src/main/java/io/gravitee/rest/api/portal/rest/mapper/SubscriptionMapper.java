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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionConsumerConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface SubscriptionMapper {
    SubscriptionMapper INSTANCE = Mappers.getMapper(SubscriptionMapper.class);

    Logger log = LoggerFactory.getLogger(SubscriptionMapper.class);

    @Mapping(target = "keys", ignore = true)
    @Mapping(target = "endAt", source = "endingAt")
    @Mapping(target = "startAt", source = "startingAt")
    Subscription map(SubscriptionEntity subscriptionEntity);

    @Mapping(target = "entrypointConfiguration", qualifiedByName = "deserializeConfiguration")
    SubscriptionConsumerConfiguration map(SubscriptionConfigurationEntity subscriptionConfigurationEntity);

    default Subscription.StatusEnum map(SubscriptionStatus status) {
        return Subscription.StatusEnum.fromValue(status.name());
    }

    default Subscription.OriginEnum convert(String origin) {
        try {
            return Subscription.OriginEnum.valueOf(origin);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Unable to serialize Subscription Origin: [{}]", origin);
            return null;
        }
    }
}
