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
     * Every Guardian of this environment whose name matches, ignoring case.
     *
     * <p>Ignoring case because that is the rule the domain enforces: two Guardians of one environment
     * may not share a name, and "Credential leakage" and "credential leakage" are the same name to a
     * person reading a list.
     *
     * <p>All of them, not the first. Whether the unique index catches two case variants depends on the
     * store: PostgreSQL and MongoDB compare exactly, so "Foo" and "foo" can sit side by side, while
     * MySQL, MariaDB and SQL Server refuse the second under their default case-insensitive collations.
     * Either way a store can hold several matches — data that predates the rule, or two creates that
     * raced past it. A caller asking whether a name is free has to exclude itself from the answer, and
     * with one row it could not tell "the only match is me" from "one of the matches is me", which
     * would make the answer depend on which row the store happened to return.
     *
     * @param environmentId the environment to look in, never {@code null}
     * @param name the name to match, compared without regard to case
     * @return the Guardians with that name, empty when the environment has none
     */
    List<Guardian> findByEnvironmentIdAndNameIgnoreCase(String environmentId, String name) throws TechnicalException;

    /**
     * Delete every Guardian of an environment.
     *
     * @param environmentId the environment to purge
     * @return the identifiers of the deleted Guardians
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
