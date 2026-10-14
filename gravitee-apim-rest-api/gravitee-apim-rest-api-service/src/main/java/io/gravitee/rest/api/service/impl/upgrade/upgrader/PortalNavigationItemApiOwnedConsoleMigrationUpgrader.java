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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NAVIGATION_ITEM_API_OWNED_CONSOLE_MIGRATION_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-time move of the documentation the portal editor stored under API listing rows, before it wrote
 * such items as owned by the API, onto the API itself. {@link PortalNavigationItemApiOwnedRekeyUpgrader}
 * already moved what automation had created there; this moves what is left.
 * <p>
 * Rows are only updated in place — never created, deleted or given another id — and the rules live in
 * {@link ListingDocumentationMigrationPlanner}. Positions are written before any item moves, so a run
 * that stops part-way and starts again reads the same merged order back from them.
 */
@Component
@CustomLog
public class PortalNavigationItemApiOwnedConsoleMigrationUpgrader implements Upgrader {

    private final PortalNavigationItemRepository portalNavigationItemRepository;
    private final ApiRepository apiRepository;

    public PortalNavigationItemApiOwnedConsoleMigrationUpgrader(
        @Lazy PortalNavigationItemRepository portalNavigationItemRepository,
        @Lazy ApiRepository apiRepository
    ) {
        this.portalNavigationItemRepository = portalNavigationItemRepository;
        this.apiRepository = apiRepository;
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_API_OWNED_CONSOLE_MIGRATION_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrate);
    }

    private boolean migrate() throws TechnicalException {
        // One full scan, as PortalNavigationItemApiOwnedRekeyUpgrader#migrate does
        Map<Map.Entry<String, String>, List<PortalNavigationItem>> byOrgAndEnv = portalNavigationItemRepository
            .findAll()
            .stream()
            .collect(Collectors.groupingBy(item -> Map.entry(item.getOrganizationId(), item.getEnvironmentId())));

        for (var entry : byOrgAndEnv.entrySet()) {
            migrateEnvironment(entry.getKey().getValue(), entry.getValue());
        }
        return true;
    }

    private void migrateEnvironment(String environmentId, List<PortalNavigationItem> items) throws TechnicalException {
        var plan = ListingDocumentationMigrationPlanner.plan(items, existingListedApis(environmentId, items));
        var byId = items.stream().collect(Collectors.toMap(PortalNavigationItem::getId, item -> item));

        for (var position : plan.positions().entrySet()) {
            var item = byId.get(position.getKey());
            item.setOrder(position.getValue());
            portalNavigationItemRepository.update(item);
        }
        for (var relabel : plan.relabels()) {
            var item = byId.get(relabel.itemId());
            item.setParentId(relabel.parentId());
            item.setRootId(relabel.rootId());
            item.setReferenceType(PortalNavigationReferenceType.API);
            item.setReferenceId(relabel.apiId());
            item.setSegment(relabel.segment());
            portalNavigationItemRepository.update(item);
        }

        plan.decisions().forEach(decision -> log.info("Environment {}: {}", environmentId, decision));
        if (!plan.isEmpty() || !plan.decisions().isEmpty()) {
            log.info("Environment {}: API documentation moved off listing rows: {}", environmentId, plan.summary());
        }
    }

    /** The listed APIs that still exist in this environment; documentation under any other listing stays where it is. */
    private Set<String> existingListedApis(String environmentId, List<PortalNavigationItem> items) throws TechnicalException {
        var listedApiIds = items
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.API)
            .map(PortalNavigationItem::getApiId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        var existing = new HashSet<String>();
        for (var apiId : listedApiIds) {
            if (
                apiRepository
                    .findById(apiId)
                    .filter(api -> environmentId.equals(api.getEnvironmentId()))
                    .isPresent()
            ) {
                existing.add(apiId);
            }
        }
        return existing;
    }
}
