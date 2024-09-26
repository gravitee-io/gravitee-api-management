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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.*;
import io.gravitee.repository.management.model.Api;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiRepository extends CrudRepository<Api, String> {
    /* Default batch size for streamed search. Indeed, the streamed search calls the paginated search*/
    int DEFAULT_STREAM_BATCH_SIZE = 100;

    Page<Api> search(ApiCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter);

    List<Api> search(ApiCriteria apiCriteria, ApiFieldFilter apiFieldFilter);

    default Stream<Api> search(ApiCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter) {
        return search(apiCriteria, sortable, apiFieldFilter, DEFAULT_STREAM_BATCH_SIZE);
    }

    Stream<Api> search(ApiCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter, int batchSize);

    Page<String> searchIds(List<ApiCriteria> apiCriteria, Pageable pageable, Sortable sortable);

    Set<String> listCategories(ApiCriteria apiCriteria) throws TechnicalException;

    Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException;

    Optional<String> findIdByEnvironmentIdAndCrossId(final String environmentId, final String crossId) throws TechnicalException;

    boolean existById(final String appId) throws TechnicalException;

    /**
     * Delete api by environment ID
     *
     * @param environmentId The environment ID
     * @return List of deleted IDs for api
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    @Override
    default Optional<Api> findById(String id) throws TechnicalException {
        return find(List.of(id)).stream().findFirst();
    }

    Collection<Api> find(Iterable<String> ids) throws TechnicalException;
}
