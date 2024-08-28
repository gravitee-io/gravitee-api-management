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

import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import io.gravitee.apim.core.portal_menu_link.query_service.PortalMenuLinkQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PortalMenuLinkQueryServiceInMemory implements PortalMenuLinkQueryService, InMemoryAlternative<PortalMenuLink> {

    ArrayList<PortalMenuLink> storage;

    public PortalMenuLinkQueryServiceInMemory(PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudServiceInMemory) {
        storage = portalMenuLinkCrudServiceInMemory.storage;
    }

    @Override
    public List<PortalMenuLink> findByEnvironmentIdSortByOrder(String environmentId) {
        return storage
            .stream()
            .filter(menuLink -> environmentId.equals(menuLink.getEnvironmentId()))
            .sorted(Comparator.comparingInt(PortalMenuLink::getOrder))
            .toList();
    }

    @Override
    public List<PortalMenuLink> findByEnvironmentIdAndVisibilitySortByOrder(String environmentId, PortalMenuLinkVisibility visibility) {
        return storage
            .stream()
            .filter(menuLink -> environmentId.equals(menuLink.getEnvironmentId()) && visibility.equals(menuLink.getVisibility()))
            .sorted(Comparator.comparingInt(PortalMenuLink::getOrder))
            .toList();
    }

    @Override
    public void initWith(List<PortalMenuLink> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalMenuLink> storage() {
        return Collections.unmodifiableList(storage);
    }
}
