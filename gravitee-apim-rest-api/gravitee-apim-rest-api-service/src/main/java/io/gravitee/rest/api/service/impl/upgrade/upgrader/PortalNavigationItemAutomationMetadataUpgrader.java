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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NAVIGATION_ITEM_AUTOMATION_METADATA_UPGRADER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalPageContent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-time backfill of {@code automationMetadata} onto the {@link PortalNavigationItem} rows that
 * correspond to already-materialized, automation-managed Documentation pages — copied from the
 * (now deprecated) {@link PortalPageContent#getAutomationMetadata()}. New writes populate the nav
 * item directly; this only backfills history predating that change.
 *
 * <p>Pre-existing automation-managed Links have no equivalent historical source to backfill from
 * (they never had a {@link PortalPageContent} to store metadata on) and are therefore fixed forward
 * only, i.e. on their next automation apply.
 */
@Component
@CustomLog
public class PortalNavigationItemAutomationMetadataUpgrader implements Upgrader {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PORTAL_PAGE_CONTENT_ID = "portalPageContentId";

    private final PortalPageContentRepository portalPageContentRepository;
    private final PortalNavigationItemRepository portalNavigationItemRepository;

    public PortalNavigationItemAutomationMetadataUpgrader(
        @Lazy PortalPageContentRepository portalPageContentRepository,
        @Lazy PortalNavigationItemRepository portalNavigationItemRepository
    ) {
        this.portalPageContentRepository = portalPageContentRepository;
        this.portalNavigationItemRepository = portalNavigationItemRepository;
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_AUTOMATION_METADATA_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrate);
    }

    private boolean migrate() throws TechnicalException {
        // FindAllRepository#findAll() returns the whole table with no filter — content rows are few
        // enough (one per automation-managed Documentation page, across all environments) that a
        // single full scan, grouped by (organizationId, environmentId) below, is simpler and cheaper
        // than looping environments and re-querying content per iteration.
        Map<Map.Entry<String, String>, List<PortalPageContent>> managedContentsByOrgAndEnv = portalPageContentRepository
            .findAll()
            .stream()
            .filter(pc -> pc.getAutomationMetadata() != null)
            .collect(Collectors.groupingBy(pc -> Map.entry(pc.getOrganizationId(), pc.getEnvironmentId())));

        int updatedCount = 0;
        for (var entry : managedContentsByOrgAndEnv.entrySet()) {
            updatedCount += migrateEnvironment(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue());
        }
        log.debug("Backfilled automationMetadata on {} portal navigation items", updatedCount);
        return true;
    }

    private int migrateEnvironment(String organizationId, String environmentId, List<PortalPageContent> managedContents)
        throws TechnicalException {
        Map<String, List<PortalNavigationItem>> pagesByContentId = pageItemsWithContentIdAttached(organizationId, environmentId);

        // Paired with its source content rather than migrated inline: repository writes are kept out
        // of the stream since PortalNavigationItemRepository#update throws a checked TechnicalException.
        List<Map.Entry<PortalNavigationItem, PortalPageContent>> pagesToMigrate = managedContents
            .stream()
            .flatMap(content ->
                pagesByContentId
                    .getOrDefault(content.getId(), List.of())
                    .stream()
                    .map(page -> Map.entry(page, content))
            )
            .filter(entry -> entry.getKey().getAutomationMetadata() == null)
            .toList();

        for (var entry : pagesToMigrate) {
            PortalNavigationItem page = entry.getKey();
            page.setAutomationMetadata(entry.getValue().getAutomationMetadata().trimmedForNavItem());
            portalNavigationItemRepository.update(page);
        }
        return pagesToMigrate.size();
    }

    // Items whose configuration doesn't carry a parsable portalPageContentId are excluded up front
    // (rather than relying on the groupingBy to reject them) since a null key would throw instead of
    // being skipped. A single portalPageContentId can legitimately be shared by several nav items
    // (e.g. one API-doc page materialized into multiple nav-api rows), so every match must be updated,
    // not just one.
    private Map<String, List<PortalNavigationItem>> pageItemsWithContentIdAttached(String organizationId, String environmentId)
        throws TechnicalException {
        return portalNavigationItemRepository
            .findAllByOrganizationIdAndEnvironmentId(organizationId, environmentId)
            .stream()
            .filter(item -> item.getType() == PortalNavigationItem.Type.PAGE && extractContentId(item) != null)
            .collect(Collectors.groupingBy(this::extractContentId));
    }

    private String extractContentId(PortalNavigationItem item) {
        try {
            JsonNode node = JSON.readTree(item.getConfiguration()).get(PORTAL_PAGE_CONTENT_ID);
            return node == null ? null : node.asText();
        } catch (Exception e) {
            log.warn(
                "Unable to parse configuration for portal navigation item {}; skipping automationMetadata backfill for it",
                item.getId(),
                e
            );
            return null;
        }
    }
}
