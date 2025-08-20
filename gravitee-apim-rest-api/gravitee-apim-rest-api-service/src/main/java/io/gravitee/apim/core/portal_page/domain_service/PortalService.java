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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.Entrypoint;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.Portal;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class PortalService {

    private final PortalPageCrudService crudService;

    public PortalService(PortalPageCrudService crudService) {
        this.crudService = crudService;
    }

    public Portal getPortal() {
        Map<Entrypoint, PortalPage> entrypoints = new AbstractMap<>() {
            @Override
            public @NotNull Set<Entry<Entrypoint, PortalPage>> entrySet() {
                return Set.of();
            }

            @Override
            public PortalPage get(Object key) {
                if (!(key instanceof Entrypoint)) {
                    return null;
                }
                return crudService.byEntrypoint((Entrypoint) key);
            }

            @Override
            public PortalPage put(Entrypoint key, PortalPage value) {
                return crudService.setEntrypoint(key, value);
            }

            @Override
            public boolean containsKey(Object key) {
                if (!(key instanceof Entrypoint)) {
                    return false;
                }
                return crudService.entrypointExists((Entrypoint) key);
            }
        };
        Map<PageId, PortalPage> pages = new AbstractMap<>() {
            @Override
            public @NotNull Set<Entry<PageId, PortalPage>> entrySet() {
                return Set.of();
            }

            @Override
            public PortalPage get(Object key) {
                if (!(key instanceof PageId pageId)) {
                    return null;
                }
                return crudService.getById(pageId);
            }

            @Override
            public PortalPage put(PageId key, PortalPage value) {
                return crudService.create(value);
            }

            @Override
            public boolean containsKey(Object key) {
                if (!(key instanceof PageId pageId)) {
                    return false;
                }
                return crudService.idExists(pageId);
            }
        };

        return new Portal(pages, entrypoints);
    }
}
