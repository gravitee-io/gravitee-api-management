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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NAVIGATION_ITEM_ROOT_ID_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-time backfill of {@code rootId} on existing portal navigation items so the front end can
 * resolve the root of a navigation hierarchy without traversing parent chains at query time (e.g.
 * redirect from Catalog to a specific API doc page using only the API nav item id). Root-level
 * items get {@code rootId = id}; nested items get the top-level ancestor's id.
 */
@Component
@CustomLog
public class PortalNavigationItemRootIdUpgrader implements Upgrader {

    private static final String ZERO_ROOT_ID = "00000000-0000-0000-0000-000000000000";

    private final EnvironmentRepository environmentRepository;
    private final PortalNavigationItemRepository portalNavigationItemRepository;

    public PortalNavigationItemRootIdUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy PortalNavigationItemRepository portalNavigationItemRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.portalNavigationItemRepository = portalNavigationItemRepository;
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_ROOT_ID_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrateAllEnvironments);
    }

    private boolean migrateAllEnvironments() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            migrateEnvironment(environment.getOrganizationId(), environment.getId());
        }
        return true;
    }

    private void migrateEnvironment(String organizationId, String environmentId) throws TechnicalException {
        List<PortalNavigationItem> items = new ArrayList<>(
            portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(organizationId, environmentId)
        );
        if (items.isEmpty()) {
            return;
        }

        List<PortalNavigationItem> roots = items
            .stream()
            .filter(item -> item.getParentId() == null)
            .toList();
        Deque<PortalNavigationItem> queue = new ArrayDeque<>(roots);
        items.removeAll(roots);

        Map<String, String> resolved = new HashMap<>();
        int updatedCount = 0;

        while (!queue.isEmpty()) {
            PortalNavigationItem item = queue.removeFirst();

            String rootId = item.getParentId() == null ? item.getId() : resolved.get(item.getParentId());
            resolved.put(item.getId(), rootId);

            if (needsRootIdMigration(item)) {
                item.setRootId(rootId);
                portalNavigationItemRepository.update(item);
                updatedCount++;
            }

            List<PortalNavigationItem> children = items
                .stream()
                .filter(child -> item.getId().equals(child.getParentId()))
                .toList();
            queue.addAll(children);
            items.removeAll(children);
        }

        if (!items.isEmpty()) {
            List<String> unresolvedIds = items.stream().map(PortalNavigationItem::getId).toList();
            throw new TechnicalException(
                "Unable to resolve rootId for portal navigation items in environment " +
                    environmentId +
                    ". Unresolved item ids: " +
                    String.join(", ", unresolvedIds)
            );
        }

        log.debug("Updated {} portal navigation items for environment {}", updatedCount, environmentId);
    }

    private static boolean needsRootIdMigration(PortalNavigationItem item) {
        String rootId = item.getRootId();
        return rootId == null || rootId.isBlank() || ZERO_ROOT_ID.equals(rootId);
    }
}
