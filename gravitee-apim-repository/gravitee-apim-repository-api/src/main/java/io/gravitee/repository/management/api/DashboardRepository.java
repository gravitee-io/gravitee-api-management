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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardReferenceType;
import java.util.List;
import java.util.Optional;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DashboardRepository extends CrudRepository<Dashboard, String> {
    List<Dashboard> findByReference(String referenceType, String referenceId) throws TechnicalException;
    List<Dashboard> findByReferenceAndType(String referenceType, String referenceId, String type) throws TechnicalException;
    Optional<Dashboard> findByReferenceAndId(String referenceType, String referenceId, String id) throws TechnicalException;

    /**
     * Delete dashboards by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted dashboards
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, DashboardReferenceType referenceType) throws TechnicalException;
}
