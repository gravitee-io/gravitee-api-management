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

import io.gravitee.apim.core.portal_menu_link.crud_service.PortalMenuLinkCrudService;
import io.gravitee.apim.core.portal_menu_link.exception.PortalMenuLinkNotFoundException;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class PortalMenuLinkCrudServiceInMemory implements PortalMenuLinkCrudService, InMemoryAlternative<PortalMenuLink> {

    final ArrayList<PortalMenuLink> storage = new ArrayList<>();

    @Override
    public PortalMenuLink getByIdAndEnvironmentId(String portalMenuLinkId, String environmentId) {
        return storage
            .stream()
            .filter(menuLink -> portalMenuLinkId.equals(menuLink.getId()) && environmentId.equals(menuLink.getEnvironmentId()))
            .findFirst()
            .orElseThrow(() -> new PortalMenuLinkNotFoundException(portalMenuLinkId));
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
