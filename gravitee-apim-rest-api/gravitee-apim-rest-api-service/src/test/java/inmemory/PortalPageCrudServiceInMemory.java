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
import io.gravitee.apim.core.portal_page.model.PageLocator;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.ArrayList;
import java.util.List;

public class PortalPageCrudServiceInMemory implements PortalPageCrudService, InMemoryAlternative<PortalPage> {

    private final List<PortalPage> storage = new ArrayList<>();

    @Override
    public void initWith(List<PortalPage> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalPage> storage() {
        return storage;
    }

    @Override
    public PortalPage getHomepage() {
        return storage.stream().filter(p -> p.locator().isHomepage()).findFirst().orElse(null);
    }

    @Override
    public PortalPage getByLocator(PageLocator locator) {
        return storage.stream().filter(p -> p.locator().equals(locator)).findFirst().orElse(null);
    }

    @Override
    public boolean locatorExists(PageLocator locator) {
        return storage.stream().anyMatch(p -> p.locator().equals(locator));
    }

    @Override
    public PortalPage create(PortalPage page) {
        storage.add(page);
        return page;
    }

    @Override
    public PortalPage setHomepage(PortalPage page) {
        storage.removeIf(p -> p.locator().isHomepage());
        storage.add(page);
        return page;
    }
}
