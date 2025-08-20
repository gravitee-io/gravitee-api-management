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

import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.Entrypoint;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalPageCrudServiceInMemory implements PortalPageCrudService, InMemoryAlternative<PortalPage> {

    private final List<PortalPage> storage = new ArrayList<>();
    private final Map<PageId, PortalPage> pages = new HashMap<>();
    private final Map<Entrypoint, PortalPage> entrypoints = new HashMap<>();

    @Override
    public void initWith(List<PortalPage> items) {
        storage.clear();
        storage.addAll(items);
        pages.clear();
        entrypoints.clear();
        for (PortalPage page : items) {
            pages.put(page.id(), page);
        }
    }

    @Override
    public void reset() {
        storage.clear();
        pages.clear();
        entrypoints.clear();
    }

    @Override
    public List<PortalPage> storage() {
        return storage;
    }

    @Override
    public PortalPage getHomepage() {
        return entrypoints.get(Entrypoint.HOMEPAGE);
    }

    @Override
    public PortalPage create(PortalPage page) {
        storage.add(page);
        pages.put(page.id(), page);
        return page;
    }

    @Override
    public PortalPage setEntrypoint(Entrypoint entrypoint, PortalPage page) {
        entrypoints.put(entrypoint, page);
        pages.put(page.id(), page);
        if (!storage.contains(page)) {
            storage.add(page);
        }
        return page;
    }

    public PortalPage byEntrypoint(Entrypoint entrypoint) {
        return entrypoints.get(entrypoint);
    }

    public PortalPage getById(PageId id) {
        return pages.get(id);
    }

    @Override
    public boolean entrypointExists(Entrypoint key) {
        return entrypoints.containsKey(key);
    }

    @Override
    public boolean idExists(PageId pageId) {
        return pages.containsKey(pageId);
    }
}
