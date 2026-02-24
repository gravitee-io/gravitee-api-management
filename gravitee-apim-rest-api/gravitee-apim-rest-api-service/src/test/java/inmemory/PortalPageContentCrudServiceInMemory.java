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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContent;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.ArrayList;
import java.util.List;

public class PortalPageContentCrudServiceInMemory implements InMemoryAlternative<PortalPageContent<?>>, PortalPageContentCrudService {

    ArrayList<PortalPageContent<?>> storage = new ArrayList<>();

    @Override
    public void initWith(List<PortalPageContent<?>> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalPageContent<?>> storage() {
        return storage;
    }

    @Override
    public PortalPageContent<?> create(PortalPageContent<?> content) {
        storage.add(content);
        return content;
    }

    @Override
    public PortalPageContent<?> createDefault(String organizationId, String environmentId) {
        final var pageContentId = PortalPageContentId.random();
        final var portalPageContent = new GraviteeMarkdownPageContent(
            pageContentId,
            organizationId,
            environmentId,
            new GraviteeMarkdownContent("default page content")
        );
        return this.create(portalPageContent);
    }

    @Override
    public PortalPageContent<?> update(PortalPageContent<?> content) {
        final var existingContent = storage
            .stream()
            .filter(c -> c.getId().id().equals(content.getId().id()))
            .findFirst();
        if (existingContent.isPresent()) {
            storage.remove(existingContent.get());
            storage.add(content);
            return content;
        }
        return null;
    }

    @Override
    public void delete(PortalPageContentId id) {
        storage.removeIf(content -> content.getId().id().equals(id.id()));
    }
}
