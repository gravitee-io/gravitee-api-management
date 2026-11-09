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
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Guardian;
import java.util.List;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public interface GuardianRepository extends CrudRepository<Guardian, String> {
    /**
     * Search for Guardians matching the given criteria.
     *
     * @param criteria the criteria to match, never {@code null}
     * @param pageable the page to return
     * @param sortable the sort to apply
     * @return a page of matching Guardians
     */
    Page<Guardian> search(GuardianCriteria criteria, Pageable pageable, Sortable sortable) throws TechnicalException;

    /**
     * The Guardian of this environment whose name matches, ignoring case.
     *
     * <p>Ignoring case because that is the rule the domain enforces: two Guardians of one environment
     * may not share a name, and "Credential leakage" and "credential leakage" are the same name to a
     * person reading a list.
     *
     * @param environmentId the environment to look in, never {@code null}
     * @param name the name to match, compared without regard to case
     * @return the Guardian with that name, or empty when the environment has none
     */
    Optional<Guardian> findByEnvironmentIdAndNameIgnoreCase(String environmentId, String name) throws TechnicalException;

    /**
     * Delete every Guardian of an environment.
     *
     * @param environmentId the environment to purge
     * @return the identifiers of the deleted Guardians
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
