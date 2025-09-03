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

import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;

public class PortalPageContextCrudServiceInMemory implements PortalPageContextCrudService, InMemoryAlternative<PortalPageContext> {

    private List<PortalPageContext> storage;

    @Override
    public List<PageId> findAllByContextTypeAndEnvironmentId(PortalViewContext contextType, String environmentId) {
        var repoCtx = PortalPageContextType.valueOf(contextType.name());
        return storage
            .stream()
            .filter(ppc -> ppc.getEnvironmentId().equals(environmentId) && ppc.getContextType() == repoCtx)
            .map(PortalPageContext::getPageId)
            .map(PageId::of)
            .toList();
    }

    @Override
    public void initWith(List<PortalPageContext> items) {
        this.storage = items;
    }

    @Override
    public void reset() {
        this.storage = List.of();
    }

    @Override
    public List<PortalPageContext> storage() {
        return storage;
    }
}
