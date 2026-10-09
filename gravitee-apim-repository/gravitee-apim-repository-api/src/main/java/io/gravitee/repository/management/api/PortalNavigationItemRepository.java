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
import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.List;

public interface PortalNavigationItemRepository extends CrudRepository<PortalNavigationItem, String> {
    List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId)
        throws TechnicalException;

    List<PortalNavigationItem> findByAutomationReference(
        String environmentId,
        AutomationTargetReferenceType referenceType,
        String referenceId
    ) throws TechnicalException;

    List<PortalNavigationItem> findAllByParentIdAndEnvironmentId(String parentId, String environmentId) throws TechnicalException;

    List<PortalNavigationItem> findAllByAreaAndEnvironmentIdAndParentIdIsNull(PortalNavigationItem.Area area, String environmentId)
        throws TechnicalException;

    List<PortalNavigationItem> findAllTopLevelByAreaAndEnvironmentAndReference(
        PortalNavigationItem.Area area,
        String environmentId,
        PortalNavigationReferenceType referenceType,
        String referenceId
    ) throws TechnicalException;

    List<PortalNavigationItem> findAllByAreaAndEnvironmentId(PortalNavigationItem.Area area, String environmentId)
        throws TechnicalException;

    List<PortalNavigationItem> searchByCriteria(PortalNavigationItemCriteria criteria) throws TechnicalException;

    List<PortalNavigationItem> findAllByRootId(String rootId, String environmentId) throws TechnicalException;

    /**
     * Replaces the configuration of one item, and nothing else, provided the stored configuration is
     * still {@code expectedConfiguration}: a compare-and-swap that lets a caller persist a partial
     * change without overwriting what others wrote meanwhile.
     *
     * @return {@code true} when the row was replaced, {@code false} when the item does not exist or
     *   its configuration has changed since it was read
     */
    boolean updateConfigurationIfUnchanged(String id, String expectedConfiguration, String configuration) throws TechnicalException;

    void deleteByIds(List<String> ids) throws TechnicalException;

    void deleteByOrganizationId(String organizationId) throws TechnicalException;

    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
