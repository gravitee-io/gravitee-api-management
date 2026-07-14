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

import io.gravitee.apim.core.portal_category.crud_service.PortalCategoryCrudService;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PortalCategoryCrudServiceInMemory implements PortalCategoryCrudService, InMemoryAlternative<PortalCategory> {

    final ArrayList<PortalCategory> storage = new ArrayList<>();

    @Override
    public PortalCategory create(PortalCategory portalCategory) {
        storage.add(portalCategory);
        return portalCategory;
    }

    @Override
    public PortalCategory update(PortalCategory portalCategory) {
        var index = findIndex(storage, pc -> pc.getId().equals(portalCategory.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), portalCategory);
            return portalCategory;
        }
        throw new IllegalStateException("Portal category not found");
    }

    @Override
    public void delete(String id) {
        storage.removeIf(pc -> id.equals(pc.getId()));
    }

    @Override
    public Optional<PortalCategory> get(String id) {
        return storage
            .stream()
            .filter(pc -> id.equals(pc.getId()))
            .findFirst();
    }

    @Override
    public void initWith(List<PortalCategory> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalCategory> storage() {
        return Collections.unmodifiableList(storage);
    }
}
