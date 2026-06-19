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

import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class PortalCrudServiceInMemory implements PortalCrudService, InMemoryAlternative<Portal> {

    final ArrayList<Portal> storage = new ArrayList<>();

    @Override
    public Portal create(Portal portal) {
        storage.add(portal);
        return portal;
    }

    @Override
    public Portal update(Portal portal) {
        OptionalInt index = this.findIndex(this.storage, p -> p.getId().equals(portal.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), portal);
            return portal;
        }
        throw new PortalNotFoundException(portal.getId().toString());
    }

    @Override
    public Optional<Portal> findByIdAndEnvironmentId(PortalId portalId, String environmentId) {
        return storage
            .stream()
            .filter(p -> portalId.equals(p.getId()) && environmentId.equals(p.getEnvironmentId()))
            .findFirst();
    }

    @Override
    public List<Portal> findByEnvironmentId(String environmentId) {
        return storage
            .stream()
            .filter(p -> environmentId.equals(p.getEnvironmentId()))
            .toList();
    }

    @Override
    public void delete(PortalId portalId) {
        storage.removeIf(p -> portalId.equals(p.getId()));
    }

    @Override
    public void initWith(List<Portal> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Portal> storage() {
        return Collections.unmodifiableList(storage);
    }
}
