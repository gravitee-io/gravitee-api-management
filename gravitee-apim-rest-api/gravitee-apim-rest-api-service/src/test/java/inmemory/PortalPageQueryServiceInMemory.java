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

import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.query_service.PortalPageQueryService;
import java.util.ArrayList;
import java.util.List;

public class PortalPageQueryServiceInMemory implements PortalPageQueryService, InMemoryAlternative<PortalPageWithViewDetails> {

    private List<PortalPageWithViewDetails> storage = new ArrayList<>();

    @Override
    public void initWith(List<PortalPageWithViewDetails> items) {
        this.storage = items;
    }

    @Override
    public void reset() {
        this.storage = List.of();
    }

    @Override
    public List<PortalPageWithViewDetails> storage() {
        return List.copyOf(storage);
    }

    @Override
    public List<PortalPageWithViewDetails> findByEnvironmentIdAndContext(String environmentId, PortalViewContext context) {
        return storage
            .stream()
            .filter(pageWithViewDetails -> pageWithViewDetails.viewDetails().context().equals(context))
            .toList();
    }

    @Override
    public PortalPageWithViewDetails loadContentFor(PageId pageId, PortalPageView details) {
        return storage
            .stream()
            .filter(p -> p.page().getId().equals(pageId))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<PortalPageWithViewDetails> findByEnvironmentIdAndContext(
        String environmentId,
        PortalViewContext context,
        List<ExpandsViewContext> expand
    ) {
        return findByEnvironmentIdAndContext(environmentId, context);
    }
}
