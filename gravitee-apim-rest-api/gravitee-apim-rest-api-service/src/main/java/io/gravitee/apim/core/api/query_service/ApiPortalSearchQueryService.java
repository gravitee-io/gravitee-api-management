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
package io.gravitee.apim.core.api.query_service;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ApiPortalSearchQueryService {
    Page<Api> search(Query query);

    default Page<Api> search(
        String environmentId,
        String organizationId,
        @Nullable String query,
        Set<String> allowedApiIds,
        Pageable pageable,
        Sortable sortable
    ) {
        return search(
            new Query(
                environmentId,
                organizationId,
                Optional.ofNullable(query),
                allowedApiIds,
                Optional.ofNullable(pageable),
                Optional.ofNullable(sortable)
            )
        );
    }

    default List<Api> search(String environmentId, String organizationId, String query, Set<String> allowedApiIds) {
        return search(new Query(environmentId, organizationId, query, allowedApiIds)).getContent();
    }

    default List<Api> search(String environmentId, String organizationId, Set<String> allowedApiIds) {
        return search(new Query(environmentId, organizationId, allowedApiIds)).getContent();
    }

    record Query(
        String environmentId,
        String organizationId,
        Optional<String> query,
        Set<String> allowedApiIds,
        Optional<Pageable> pageable,
        Optional<Sortable> sortable
    ) {
        public Query(String environmentId, String organizationId, String query, Set<String> allowedApiIds) {
            this(environmentId, organizationId, Optional.of(query), allowedApiIds, Optional.empty(), Optional.empty());
        }

        public Query(String environmentId, String organizationId, Set<String> allowedApiIds) {
            this(environmentId, organizationId, Optional.empty(), allowedApiIds, Optional.empty(), Optional.empty());
        }
    }
}
