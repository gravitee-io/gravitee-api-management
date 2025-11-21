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

import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class PortalNavigationItemsCrudServiceInMemory
    implements InMemoryAlternative<PortalNavigationItem>, PortalNavigationItemCrudService {

    private List<PortalNavigationItem> storage;

    public PortalNavigationItemsCrudServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    public PortalNavigationItemsCrudServiceInMemory(List<PortalNavigationItem> storage) {
        this.storage = storage;
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem portalNavigationItem) {
        storage.add(portalNavigationItem);
        return portalNavigationItem;
    }

    @Override
    public PortalNavigationItem update(PortalNavigationItem portalNavigationItem) {
        OptionalInt index = this.findIndex(storage, item -> item.getId().equals(portalNavigationItem.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), portalNavigationItem);
            return portalNavigationItem;
        }
        throw new IllegalStateException("Item not found");
    }

    @Override
    public void delete(PortalNavigationItemId portalNavigationItemId) {
        storage.removeIf(page -> page.getId().equals(portalNavigationItemId));
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
