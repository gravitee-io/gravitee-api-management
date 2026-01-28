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

import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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

    @Mapping(target = "api.id", source = "apiId")
    @Mapping(target = "plan.id", source = "planId")
    @Mapping(target = "application.id", source = "applicationId")
    @Mapping(target = "processedBy.id", source = "processedBy")
    @Mapping(target = "subscribedBy.id", source = "subscribedBy")
    @Mapping(target = "consumerMessage", source = "requestMessage")
    @Mapping(target = "publisherMessage", source = "reasonMessage")
    @Mapping(target = "consumerConfiguration", source = "configuration")
    Subscription map(io.gravitee.apim.core.subscription.model.SubscriptionEntity subscriptionEntity);

    List<Subscription> mapToList(List<SubscriptionEntity> subscriptionEntities);

    SubscriptionStatus map(io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus subscriptionStatus);

    Set<SubscriptionStatus> mapToStatusSet(Collection<io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus> subscriptionStatus);

    @Mapping(target = "entrypointConfiguration", qualifiedByName = "deserializeConfiguration")
    SubscriptionConsumerConfiguration map(SubscriptionConfigurationEntity subscriptionConfigurationEntity);

    @Mapping(target = "entrypointConfiguration", qualifiedByName = "serializeConfiguration")
    SubscriptionConfigurationEntity map(SubscriptionConsumerConfiguration subscriptionConsumerConfiguration);

    @Mapping(target = "application", source = "applicationId")
    @Mapping(target = "plan", source = "planId")
    @Mapping(target = "configuration", source = "consumerConfiguration", qualifiedByName = "mapConsumerConfigurationToEntity")
    NewSubscriptionEntity map(CreateSubscription createSubscription);

    @Mapping(
        target = "configuration",
        source = "updateSubscription.consumerConfiguration",
        qualifiedByName = "mapConsumerConfigurationToEntity"
    )
    @Mapping(target = "id", expression = "java(subscriptionId)")
    UpdateSubscriptionEntity map(UpdateSubscription updateSubscription, String subscriptionId);

    @Mapping(target = "id", expression = "java(subscriptionId)")
    @Mapping(target = "accepted", constant = "false")
    ProcessSubscriptionEntity map(RejectSubscription rejectSubscription, String subscriptionId);

    @Mapping(target = "id", expression = "java(subscriptionId)")
    @Mapping(target = "plan", source = "transferSubscription.planId")
    TransferSubscriptionEntity map(TransferSubscription transferSubscription, String subscriptionId);

    ApiKey mapToApiKey(ApiKeyEntity apiKeyEntity);

    List<ApiKey> mapToApiKeyList(List<ApiKeyEntity> apiKeyEntities);

    @Named("toBaseSubscriptionsList")
    default List<BaseSubscription> mapToBaseSubscriptionList(List<String> subscriptionIds) {
        return subscriptionIds
            .stream()
            .map(id -> new BaseSubscription().id(id))
            .toList();
    }

    @Mapping(source = "subscriptions", target = "subscriptions", qualifiedByName = "toBaseSubscriptionsList")
    ApiKey mapToApiKey(io.gravitee.apim.core.api_key.model.ApiKeyEntity apiKeyEntity);

    @Named("mapConsumerConfigurationToEntity")
    default SubscriptionConfigurationEntity mapConsumerConfigurationToEntity(Object consumerConfiguration) {
        if (consumerConfiguration == null) {
            return null;
        }
        if (consumerConfiguration instanceof SubscriptionConsumerConfiguration) {
            return map((SubscriptionConsumerConfiguration) consumerConfiguration);
        }
        // If it's already a SubscriptionConfigurationEntity, return as-is
        if (consumerConfiguration instanceof SubscriptionConfigurationEntity) {
            return (SubscriptionConfigurationEntity) consumerConfiguration;
        }
        // For other types, return null (shouldn't happen in normal flow)
        return null;
    }

    default Subscription.OriginEnum mapOrigin(OriginContext.Origin origin) {
        return origin == null
            ? null
            : OriginContext.Origin.KUBERNETES == origin
                ? Subscription.OriginEnum.KUBERNETES
                : Subscription.OriginEnum.MANAGEMENT;
    }
}
