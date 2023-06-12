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

import io.gravitee.rest.api.management.v2.rest.model.CreateSubscription;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionConsumerConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSubscription;
import io.gravitee.rest.api.model.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface SubscriptionMapper {
    SubscriptionMapper INSTANCE = Mappers.getMapper(SubscriptionMapper.class);

    @Mapping(target = "api.id", source = "api")
    @Mapping(target = "plan.id", source = "plan")
    @Mapping(target = "application.id", source = "application")
    @Mapping(target = "processedBy.id", source = "processedBy")
    @Mapping(target = "subscribedBy.id", source = "subscribedBy")
    @Mapping(target = "consumerMessage", source = "request")
    @Mapping(target = "publisherMessage", source = "reason")
    @Mapping(target = "consumerConfiguration", source = "configuration")
    Subscription map(SubscriptionEntity subscriptionEntity);

    List<Subscription> mapToList(List<SubscriptionEntity> subscriptionEntities);

    SubscriptionStatus map(io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus subscriptionStatus);

    Set<SubscriptionStatus> mapToStatusSet(Collection<io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus> subscriptionStatus);

    @Mapping(target = "entrypointConfiguration", qualifiedByName = "deserializeConfiguration")
    SubscriptionConsumerConfiguration map(SubscriptionConfigurationEntity subscriptionConfigurationEntity);

    @Mapping(target = "entrypointConfiguration", qualifiedByName = "serializeConfiguration")
    SubscriptionConfigurationEntity map(SubscriptionConsumerConfiguration subscriptionConsumerConfiguration);

    @Mapping(target = "application", source = "applicationId")
    @Mapping(target = "plan", source = "planId")
    @Mapping(target = "configuration", source = "consumerConfiguration")
    NewSubscriptionEntity map(CreateSubscription createSubscription);

    @Mapping(target = "configuration", source = "updateSubscription.consumerConfiguration")
    @Mapping(target = "id", expression = "java(subscriptionId)")
    UpdateSubscriptionEntity map(UpdateSubscription updateSubscription, String subscriptionId);
}
