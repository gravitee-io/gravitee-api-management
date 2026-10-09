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
package io.gravitee.apim.core.portal_page.crud_service;

import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import java.util.List;
import java.util.Optional;

public interface PortalNavigationItemCrudService {
    PortalNavigationItem create(PortalNavigationItem portalNavigationItem);

    PortalNavigationItem update(PortalNavigationItem portalNavigationItem);

    /**
     * Persists the fetch state of the item's source and nothing else: every other attribute stays as
     * currently stored, so a concurrent change to the item is never overwritten. The state is dropped
     * when the item no longer exists, or when its stored source no longer has the origin of
     * {@code fetchedSource}: a stamp belongs to the source it was fetched from.
     *
     * @return the item as stored afterwards, empty when it no longer exists
     */
    Optional<PortalNavigationItem> updateSourceFetchState(
        PortalNavigationItemId id,
        PortalNavigationItemSource fetchedSource,
        PortalNavigationItemSource.FetchState fetchState
    );

    void delete(PortalNavigationItemId portalNavigationItemId);

    void deleteByIds(List<PortalNavigationItemId> ids);
}
