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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NAVIGATION_ITEM_DEFAULT_SEGMENT_UPGRADER;

import io.gravitee.apim.core.portal_page.model.Slug;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Backfills {@code segment} from {@code title} on portal navigation items that pre-date the segment field,
 * applying the same slugification as the application layer so all rows have a consistent, non-null segment.
 * Sibling collisions are resolved by appending {@code -2}, {@code -3}, … in sibling-order.
 */
@Component
@CustomLog
public class PortalNavigationItemDefaultSegmentUpgrader implements Upgrader {

    private static final String UNSET_SEGMENT = "__CHANGE_ME__";

    private final EnvironmentRepository environmentRepository;
    private final PortalNavigationItemRepository portalNavigationItemRepository;

    public PortalNavigationItemDefaultSegmentUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy PortalNavigationItemRepository portalNavigationItemRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.portalNavigationItemRepository = portalNavigationItemRepository;
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
        var items = portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(organizationId, environmentId);

        // Group by (parentId, area) — mirrors the domain service's sibling query
        var bySiblingGroup = items
            .stream()
            .collect(Collectors.groupingBy(item -> Objects.toString(item.getParentId(), "") + "|" + item.getArea()));

        int updatedCount = 0;
        for (var group : bySiblingGroup.values()) {
            var used = group
                .stream()
                .filter(i -> !needsSegmentBackfill(i))
                .map(PortalNavigationItem::getSegment)
                .collect(Collectors.toCollection(HashSet::new));

            var nullSegmentItems = group
                .stream()
                .filter(PortalNavigationItemDefaultSegmentUpgrader::needsSegmentBackfill)
                .sorted(Comparator.comparingInt(i -> Objects.requireNonNullElse(i.getOrder(), 0)))
                .toList();

            for (var item : nullSegmentItems) {
                String s = uniqueSlug(item.getTitle(), used);
                used.add(s);
                item.setSegment(s);
                portalNavigationItemRepository.update(item);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.debug("Backfilled segment for {} portal navigation items in environment {}", updatedCount, environmentId);
        }
    }

    private static boolean needsSegmentBackfill(PortalNavigationItem item) {
        String segment = item.getSegment();
        return segment == null || segment.isBlank() || UNSET_SEGMENT.equals(segment);
    }

    private static String uniqueSlug(String title, Set<String> used) {
        String base = slug(title);
        if (!used.contains(base)) return base;
        return IntStream.iterate(2, n -> n + 1)
            .mapToObj(n -> base + "-" + n)
            .filter(c -> !used.contains(c))
            .findFirst()
            .orElseThrow();
    }

    private static String slug(String value) {
        return Slug.from(value).value();
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_DEFAULT_SEGMENT_UPGRADER;
    }
}
