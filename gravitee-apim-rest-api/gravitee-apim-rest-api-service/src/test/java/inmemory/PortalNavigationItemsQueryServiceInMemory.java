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
package inmemory;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;

public class PortalNavigationItemsQueryServiceInMemory
    implements InMemoryAlternative<PortalNavigationItem>, PortalNavigationItemsQueryService {

    ArrayList<PortalNavigationItem> storage;

    public PortalNavigationItemsQueryServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    public PortalNavigationItemsQueryServiceInMemory(ArrayList<PortalNavigationItem> storage) {
        this.storage = storage;
    }

    @Override
    public PortalNavigationItem findByIdAndEnvironmentId(String environmentId, PortalNavigationItemId id) {
        return storage
            .stream()
            .filter(item -> environmentId.equals(item.getEnvironmentId()) && id.equals(item.getId()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalNavigationItemId parentId) {
        return storage
            .stream()
            .filter(
                item ->
                    environmentId.equals(item.getEnvironmentId()) &&
                    (parentId == null ? item.getParentId() == null : parentId.equals(item.getParentId()))
            )
            .toList();
    }

    @Override
    public List<PortalNavigationItem> findTopLevelItemsByEnvironmentIdAndPortalArea(String environmentId, PortalArea portalArea) {
        return storage
            .stream()
            .filter(
                item -> environmentId.equals(item.getEnvironmentId()) && portalArea.equals(item.getArea()) && item.getParentId() == null
            )
            .toList();
    }

    @Override
    public void initWith(List<PortalNavigationItem> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalNavigationItem> storage() {
        return storage;
    }
}
