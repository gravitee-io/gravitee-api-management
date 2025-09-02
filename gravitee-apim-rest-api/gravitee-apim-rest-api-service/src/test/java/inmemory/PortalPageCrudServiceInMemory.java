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
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalPageCrudServiceInMemory implements PortalPageCrudService, InMemoryAlternative<PortalPage> {

    private final List<PortalPage> storage = new ArrayList<>();
    private final Map<PortalViewContext, PortalPage> entrypoints = new HashMap<>();

    @Override
    public void initWith(List<PortalPage> items) {
        storage.clear();
        storage.addAll(items);
        entrypoints.clear();
    }

    @Override
    public void reset() {
        storage.clear();
        entrypoints.clear();
    }

    @Override
    public List<PortalPage> storage() {
        return storage;
    }

    @Override
    public List<PortalPage> byPortalViewContext(String environmentId, PortalViewContext portalViewContext) {
        PortalPage page = entrypoints.get(portalViewContext);
        return page == null ? List.of() : List.of(page);
    }

    @Override
    public boolean portalViewContextExists(String environmentId, PortalViewContext key) {
        return entrypoints.containsKey(key);
    }

    public void initWithContext(PortalViewContext portalViewContext, PortalPage page) {
        entrypoints.put(portalViewContext, page);
    }
}
