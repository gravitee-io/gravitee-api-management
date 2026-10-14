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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

class FakePortalNavigationItemRepository implements PortalNavigationItemRepository {

    private final List<PortalNavigationItem> items = new ArrayList<>();
    private int updateCount;
    private int failingUpdate;

    void initWith(List<PortalNavigationItem> initial) {
        items.clear();
        initial.forEach(item -> items.add(copy(item)));
        updateCount = 0;
        failingUpdate = 0;
    }

    /** Makes the n-th update from now on throw, as a database failing in the middle of a run would. */
    void failOnUpdate(int n) {
        failingUpdate = updateCount + n;
    }

    List<PortalNavigationItem> storage() {
        return items;
    }

    int updateCount() {
        return updateCount;
    }

    PortalNavigationItem get(String id) {
        return findById(id).orElseThrow(() -> new AssertionError("No item at id " + id));
    }

    @Override
    public Set<PortalNavigationItem> findAll() {
        return Set.copyOf(items.stream().map(FakePortalNavigationItemRepository::copy).toList());
    }

    @Override
    public Optional<PortalNavigationItem> findById(String id) {
        return items
            .stream()
            .filter(item -> item.getId().equals(id))
            .findFirst()
            .map(FakePortalNavigationItemRepository::copy);
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem item) {
        items.add(copy(item));
        return item;
    }

    @Override
    public PortalNavigationItem update(PortalNavigationItem item) throws TechnicalException {
        updateCount++;
        if (updateCount == failingUpdate) {
            throw new TechnicalException("Simulated failure on update " + updateCount);
        }
        items.removeIf(existing -> existing.getId().equals(item.getId()));
        items.add(copy(item));
        return item;
    }

    @Override
    public void delete(String id) {
        items.removeIf(item -> item.getId().equals(id));
    }

    @Override
    public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> findByAutomationReference(
        String environmentId,
        AutomationTargetReferenceType referenceType,
        String referenceId
    ) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> findAllByParentIdAndEnvironmentId(String parentId, String environmentId) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentIdAndParentIdIsNull(PortalNavigationItem.Area area, String environmentId) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> findAllTopLevelByAreaAndEnvironmentAndReference(
        PortalNavigationItem.Area area,
        String environmentId,
        PortalNavigationReferenceType referenceType,
        String referenceId
    ) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentId(PortalNavigationItem.Area area, String environmentId) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> searchByCriteria(PortalNavigationItemCriteria criteria) {
        return List.of();
    }

    @Override
    public List<PortalNavigationItem> findAllByRootId(String rootId, String environmentId) {
        return List.of();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        items.removeIf(item -> ids.contains(item.getId()));
    }

    @Override
    public void deleteByOrganizationId(String organizationId) {
        items.removeIf(item -> item.getOrganizationId().equals(organizationId));
    }

    @Override
    public boolean updateConfigurationIfUnchanged(String id, String expectedConfiguration, String configuration) {
        return items
            .stream()
            .filter(item -> item.getId().equals(id) && Objects.equals(item.getConfiguration(), expectedConfiguration))
            .findFirst()
            .map(item -> {
                item.setConfiguration(configuration);
                return true;
            })
            .orElse(false);
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) {
        items.removeIf(item -> item.getEnvironmentId().equals(environmentId));
    }

    /** Stored rows are copies, so a change made to a row read from here only counts once it is updated. */
    private static PortalNavigationItem copy(PortalNavigationItem item) {
        return PortalNavigationItem.builder()
            .id(item.getId())
            .organizationId(item.getOrganizationId())
            .environmentId(item.getEnvironmentId())
            .referenceType(item.getReferenceType())
            .referenceId(item.getReferenceId())
            .title(item.getTitle())
            .segment(item.getSegment())
            .type(item.getType())
            .area(item.getArea())
            .parentId(item.getParentId())
            .rootId(item.getRootId())
            .order(item.getOrder())
            .configuration(item.getConfiguration())
            .published(item.isPublished())
            .visibility(item.getVisibility())
            .apiId(item.getApiId())
            .apiProductId(item.getApiProductId())
            .useAutoFetch(item.isUseAutoFetch())
            .categoryIds(item.getCategoryIds())
            .automationMetadata(item.getAutomationMetadata())
            .build();
    }
}
