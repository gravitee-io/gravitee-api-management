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

import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PortalPageContentQueryServiceInMemory implements InMemoryAlternative<PortalPageContent>, PortalPageContentQueryService {

    ArrayList<PortalPageContent> storage = new ArrayList<>();

    public PortalPageContentQueryServiceInMemory() {
        initWith(List.of());
    }

    public PortalPageContentQueryServiceInMemory(List<PortalPageContent> items) {
        initWith(items);
    }

    @Override
    public Optional<PortalPageContent> findById(PortalPageContentId id) {
        return storage
            .stream()
            .filter(content -> content.getId().equals(id))
            .findFirst();
    }

    @Override
    public void initWith(List<PortalPageContent> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalPageContent> storage() {
        return storage;
    }
}
