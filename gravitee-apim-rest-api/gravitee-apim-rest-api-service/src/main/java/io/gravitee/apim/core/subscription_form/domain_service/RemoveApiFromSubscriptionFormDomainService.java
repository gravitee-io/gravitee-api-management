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
package io.gravitee.apim.core.subscription_form.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Removes a deleted API from the subscription form it is dedicated to, so the form keeps no mapping to an API
 * that no longer exists.
 *
 * @author Gravitee.io Team
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class RemoveApiFromSubscriptionFormDomainService {

    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormCrudService subscriptionFormCrudService;

    public void removeApi(String environmentId, String apiId) {
        subscriptionFormQueryService
            .findByApiId(environmentId, apiId)
            .ifPresent(form -> {
                form.unassignApi(apiId);
                subscriptionFormCrudService.update(form);
                log.debug("Removed API [{}] from subscription form [{}] of environment [{}]", apiId, form.getId(), environmentId);
            });
    }
}
