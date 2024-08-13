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
package io.gravitee.repository.mongodb.management.internal.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiMongoRepositoryCustom {
    Page<ApiMongo> search(ApiCriteria criteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter);

    /**
     * Find ids of APIs matching the given criteria.
     *
     * @param apiCriteria the search criteria
     * @param pageable    the pagination criteria
     * @param sortable    the sort criteria
     * @return the list of matching APIs' ids
     */
    Page<String> searchIds(List<ApiCriteria> apiCriteria, Pageable pageable, Sortable sortable);

    Map<String, Integer> listCategories(ApiCriteria apiCriteria);
}
