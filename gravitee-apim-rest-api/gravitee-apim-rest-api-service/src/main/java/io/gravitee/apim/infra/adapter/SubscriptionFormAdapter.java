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

import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct adapter for converting between SubscriptionForm (domain) and repository model.
 *
 * @author Gravitee.io Team
 */
@Mapper
public interface SubscriptionFormAdapter {
    SubscriptionFormAdapter INSTANCE = Mappers.getMapper(SubscriptionFormAdapter.class);

    @Mapping(target = "id", source = "id", qualifiedByName = "idToSubscriptionFormId")
    SubscriptionForm toEntity(io.gravitee.repository.management.model.SubscriptionForm subscriptionForm);

    @Mapping(target = "id", source = "id", qualifiedByName = "subscriptionFormIdToId")
    io.gravitee.repository.management.model.SubscriptionForm toRepository(SubscriptionForm subscriptionForm);

    @Named("idToSubscriptionFormId")
    default SubscriptionFormId idToSubscriptionFormId(String id) {
        return id != null ? SubscriptionFormId.of(id) : null;
    }

    @Named("subscriptionFormIdToId")
    default String subscriptionFormIdToId(SubscriptionFormId id) {
        return id != null ? id.toString() : null;
    }
}
