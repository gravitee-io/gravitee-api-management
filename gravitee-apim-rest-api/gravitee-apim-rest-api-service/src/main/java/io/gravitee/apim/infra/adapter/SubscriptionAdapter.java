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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.repository.management.model.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SubscriptionAdapter {
    SubscriptionAdapter INSTANCE = Mappers.getMapper(SubscriptionAdapter.class);

    @Mapping(target = "apiId", source = "api")
    @Mapping(target = "planId", source = "plan")
    @Mapping(target = "applicationId", source = "application")
    @Mapping(target = "requestMessage", source = "request")
    @Mapping(target = "reasonMessage", source = "reason")
    SubscriptionEntity toEntity(Subscription subscription);

    @Mapping(source = "apiId", target = "api")
    @Mapping(source = "planId", target = "plan")
    @Mapping(source = "applicationId", target = "application")
    @Mapping(source = "requestMessage", target = "request")
    @Mapping(source = "reasonMessage", target = "reason")
    Subscription fromEntity(SubscriptionEntity subscription);
}
