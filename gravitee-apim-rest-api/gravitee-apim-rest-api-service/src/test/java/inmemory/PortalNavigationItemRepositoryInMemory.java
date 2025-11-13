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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PortalNavigationItemRepositoryInMemory implements InMemoryAlternative<PortalNavigationItem>, PortalNavigationItemRepository {

    ArrayList<PortalNavigationItem> storage = new ArrayList<>();

    @Override
    public Optional<PortalNavigationItem> findById(String id) throws TechnicalException {
        return storage
            .stream()
            .filter(item -> id.equals(item.getId()))
            .findFirst();
    }

    @Override
    public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId) {
        return storage
            .stream()
            .filter(
                item ->
                    (organizationId == null || organizationId.equals(item.getOrganizationId())) &&
                    environmentId.equals(item.getEnvironmentId())
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<PortalNavigationItem> findAllByParentIdAndEnvironmentId(String parentId, String environmentId) {
        return storage
            .stream()
            .filter(
                item ->
                    ((parentId == null && item.getParentId() == null) || (parentId != null && parentId.equals(item.getParentId()))) &&
                    environmentId.equals(item.getEnvironmentId())
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentId(PortalNavigationItem.Area area, String environmentId) {
        return storage
            .stream()
            .filter(item -> area.equals(item.getArea()) && environmentId.equals(item.getEnvironmentId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentIdAndParentIdIsNull(PortalNavigationItem.Area area, String environmentId) {
        return storage
            .stream()
            .filter(item -> area.equals(item.getArea()) && environmentId.equals(item.getEnvironmentId()) && item.getParentId() == null)
            .collect(Collectors.toList());
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem item) throws TechnicalException {
        storage.add(item);
        return item;
    }

    @Override
    public PortalNavigationItem update(PortalNavigationItem item) throws TechnicalException {
        var existing = storage
            .stream()
            .filter(i -> item.getId().equals(i.getId()))
            .findFirst();
        if (existing.isPresent()) {
            storage.remove(existing.get());
            storage.add(item);
            return item;
        }
        throw new TechnicalException("Item not found");
    }

    @Override
    public void delete(String id) throws TechnicalException {
        storage.removeIf(item -> id.equals(item.getId()));
    }

    @Override
    public void deleteByOrganizationId(String organizationId) {
        storage.removeIf(item -> organizationId.equals(item.getOrganizationId()));
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) {
        storage.removeIf(item -> environmentId.equals(item.getEnvironmentId()));
    }

    @Override
    public Set<PortalNavigationItem> findAll() throws TechnicalException {
        return Set.copyOf(storage);
    }

    @Override
    public void initWith(List<PortalNavigationItem> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalNavigationItem> storage() {
        return storage;
    }
}
