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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private static final int MAX_PARENT_CHAIN_DEPTH = 50;

    /**
     * Sentinel value for "no root" in the domain; repository may store null/empty or this UUID string.
     */
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
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            migrateEnvironment(environment.getOrganizationId(), environment.getId());
        }
        return true;
    }

    private void migrateEnvironment(String organizationId, String environmentId) throws TechnicalException {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(
            organizationId,
            environmentId
        );

        Map<String, PortalNavigationItem> itemsById = items
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
        Map<String, String> rootIdsByItemId = new HashMap<>();
        int updatedCount = 0;

        for (PortalNavigationItem item : items) {
            if (!needsRootIdMigration(item)) {
                rootIdsByItemId.put(item.getId(), item.getRootId());
                continue;
            }

            item.setRootId(findRootId(item, itemsById, rootIdsByItemId));
            portalNavigationItemRepository.update(item);
            rootIdsByItemId.put(item.getId(), item.getRootId());
            updatedCount++;
        }

        log.debug("Updated {} portal navigation items for environment {}", updatedCount, environmentId);
    }

    private static boolean needsRootIdMigration(PortalNavigationItem item) {
        String rootId = item.getRootId();
        return rootId == null || rootId.isBlank() || ZERO_ROOT_ID.equals(rootId);
    }

    private static void cacheRootId(List<String> visitedItemIds, String rootId, Map<String, String> rootIdsByItemId) {
        for (String itemId : visitedItemIds) {
            rootIdsByItemId.put(itemId, rootId);
        }
    }

    private static String findRootId(
        PortalNavigationItem item,
        Map<String, PortalNavigationItem> itemsById,
        Map<String, String> rootIdsByItemId
    ) {
        String cached = rootIdsByItemId.get(item.getId());
        if (cached != null) {
            return cached;
        }

        List<String> visitedItemIds = new ArrayList<>();
        Set<String> visitedItemIdSet = new HashSet<>();
        PortalNavigationItem current = item;
        int depth = 0;

        while (depth < MAX_PARENT_CHAIN_DEPTH) {
            String currentId = current.getId();

            String cachedRootId = rootIdsByItemId.get(currentId);
            if (cachedRootId != null) {
                cacheRootId(visitedItemIds, cachedRootId, rootIdsByItemId);
                return cachedRootId;
            }

            if (!visitedItemIdSet.add(currentId)) {
                cacheRootId(visitedItemIds, currentId, rootIdsByItemId);
                rootIdsByItemId.put(currentId, currentId);
                return currentId;
            }

            visitedItemIds.add(currentId);

            if (current.getParentId() == null) {
                cacheRootId(visitedItemIds, currentId, rootIdsByItemId);
                return currentId;
            }

            PortalNavigationItem parent = itemsById.get(current.getParentId());
            if (parent == null) {
                cacheRootId(visitedItemIds, currentId, rootIdsByItemId);
                return currentId;
            }

            current = parent;
            depth++;
        }

        String fallbackRootId = visitedItemIds.isEmpty() ? item.getId() : visitedItemIds.get(visitedItemIds.size() - 1);
        cacheRootId(visitedItemIds, fallbackRootId, rootIdsByItemId);
        return fallbackRootId;
    }
}
