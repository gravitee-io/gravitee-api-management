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
package inmemory;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import java.util.Set;

public class SubscriptionSearchQueryServiceInMemory implements SubscriptionSearchQueryService {

    @Override
    public Page<SubscriptionEntity> search(
        ExecutionContext executionContext,
        String referenceId,
        SubscriptionReferenceType referenceType,
        Set<String> applicationIds,
        Set<String> planIds,
        Set<SubscriptionStatus> statuses,
        String apiKey,
        Pageable pageable
    ) {
        int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
        int pageSize = pageable != null ? pageable.getPageSize() : 10;
        return new Page<>(Collections.emptyList(), pageNumber + 1, pageSize, 0);
    }
}
