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
package io.gravitee.apim.core.subscription.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchSubscriptionsUseCase {

    private final SubscriptionSearchQueryService subscriptionSearchQueryService;

    public Output execute(Input input) {
        Page<SubscriptionEntity> page = subscriptionSearchQueryService.search(
            input.executionContext,
            input.referenceId,
            input.referenceType,
            input.applicationIds,
            input.planIds,
            input.statuses,
            input.apiKey,
            input.pageable
        );
        return new Output(page);
    }

    public record Input(
        ExecutionContext executionContext,
        String referenceId,
        SubscriptionReferenceType referenceType,
        Set<String> applicationIds,
        Set<String> planIds,
        Set<SubscriptionStatus> statuses,
        String apiKey,
        Pageable pageable
    ) {}

    public record Output(Page<SubscriptionEntity> page) {}
}
