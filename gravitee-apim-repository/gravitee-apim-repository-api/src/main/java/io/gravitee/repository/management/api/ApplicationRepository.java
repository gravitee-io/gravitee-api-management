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
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApplicationRepository extends CrudRepository<Application, String> {
    /**
     * List all applications.
     *
     * @return All public applications.
     */
    Set<Application> findAll(ApplicationStatus... statuses) throws TechnicalException;

    /**
     * List all applications for a given environment.
     * @param environmentId
     * @return All public applications.
     * @throws TechnicalException
     */
    Set<Application> findAllByEnvironment(String environmentId, ApplicationStatus... statuses) throws TechnicalException;

    /**
     * find a list of Applications via their ids.
     * @param ids a list of applications id
     * @return List Applications
     */
    Set<Application> findByIds(Collection<String> ids) throws TechnicalException;

    /**
     * find a list of Applications via their ids.
     * @param ids a list of applications id
     * @param sortable a sortable
     * @return List Applications sort with sortable parameter.
     */
    Set<Application> findByIds(Collection<String> ids, Sortable sortable) throws TechnicalException;

    /**
     * find applications by their groups
     * @param groupIds a list of group ids
     * @return applications
     */
    Set<Application> findByGroups(List<String> groupIds, ApplicationStatus... statuses) throws TechnicalException;

    /**
     * find applications by name. Support partial name (works like `contains`)
     * @param partialName
     * @return applications
     * @throws TechnicalException
     */
    Set<Application> findByNameAndStatuses(String partialName, ApplicationStatus... statuses) throws TechnicalException;

    /**
     * find applications by criteria. criteria.name supports partial name (works like `contains`)
     * @param applicationCriteria
     * @return applications
     * @throws TechnicalException
     */
    default Page<Application> search(ApplicationCriteria applicationCriteria, Pageable pageable) throws TechnicalException {
        return search(applicationCriteria, pageable, null);
    }

    Page<Application> search(ApplicationCriteria applicationCriteria, Pageable pageable, Sortable sortable) throws TechnicalException;

    Set<String> searchIds(ApplicationCriteria applicationCriteria, Sortable sortable) throws TechnicalException;

    /**
     * Delete application by environment ID
     *
     * @param environmentId The environment ID
     * @return List of deleted IDs for application
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
