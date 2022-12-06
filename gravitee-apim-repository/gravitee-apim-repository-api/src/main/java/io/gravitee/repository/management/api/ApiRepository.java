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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Api;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiRepository extends CrudRepository<Api, String> {
    Page<Api> search(ApiCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldExclusionFilter apiFieldExclusionFilter);

    default List<Api> search(ApiCriteria apiCriteria) {
        return search(apiCriteria, ApiFieldExclusionFilter.noExclusion());
    }

    /**
     * Search for APIs matching the given criteria.
     * @param apiCriteria the criteria to match
     * @param apiFieldFilter the fields to exclude or include from the return APIs
     * @return the list of APIs matching the given criteria
     */
    List<Api> search(ApiCriteria apiCriteria, ApiFieldFilter apiFieldFilter);

    /**
     * Search APIs with a criteria and a field exclusion filter.
     * @param apiCriteria the criteria to search APIs
     * @param apiFieldExclusionFilter the field exclusion filter
     * @return the list of APIs matching the criteria, each API WILL NOT contain the excluded fields
     * @deprecated use {@link #search(ApiCriteria, ApiFieldFilter)} or {@link #search(ApiCriteria, Sortable, Pageable, ApiFieldExclusionFilter)} instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter);

    Page<String> searchIds(List<ApiCriteria> apiCriteria, Pageable pageable, Sortable sortable);

    Set<String> listCategories(ApiCriteria apiCriteria) throws TechnicalException;

    Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException;
}
