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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.slug.model.Slug;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.CustomLog;

/**
 * How the documentation the portal editor stored under API listing rows, in one environment, becomes
 * documentation of the API itself. Built from the items read; {@link #apply} writes the planned values
 * onto those items and hands each changed one to the caller to store.
 * <p>
 * Only listings with no API product above them count: a product's listing shows the product's own
 * documentation, which stays where it is. A top item managed by Portal automation is left in place with
 * everything below it, so automation does not lose it; so is everything under the listing of an API
 * that no longer exists.
 * <p>
 * The plan is a function of the stored state: once applied, a new plan holds nothing, and after an
 * interrupted run it holds what is left. That is why positions are a separate step — once they are
 * stored, the merged order can be read back from the positions alone.
 */
@CustomLog
final class ListingDocumentationMigrationPlan {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SOURCE = "source";

    private final Map<String, PortalNavigationItem> itemsById;
    private final Map<String, List<PortalNavigationItem>> childrenByParentId;
    private final Set<String> existingApiIds;
    private final List<PortalNavigationItem> items;

    private final Map<String, Integer> positions = new LinkedHashMap<>();
    private final List<Relabel> relabels = new ArrayList<>();
    private final List<String> decisions = new ArrayList<>();
    private int apisMigrated;
    private int itemsMoved;
    private int sourcedItemsMoved;
    private int starterPagesMoved;
    private int segmentsRenamed;
    private int apisWithSeveralListings;
    private int itemsLeftUnderApiProducts;
    private int itemsLeftForPortalAutomation;
    private int itemsLeftForMissingApis;

    ListingDocumentationMigrationPlan(List<PortalNavigationItem> environmentItems, Set<String> existingApiIds) {
        this.items = environmentItems;
        this.existingApiIds = existingApiIds;
        this.itemsById = environmentItems
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, item -> item, (first, second) -> first));
        this.childrenByParentId = environmentItems
            .stream()
            .filter(item -> item.getParentId() != null)
            .collect(Collectors.groupingBy(PortalNavigationItem::getParentId));
        planEnvironment();
    }

    /**
     * Sets the planned values on the items this plan was built from and hands each changed item to
     * {@code itemUpdater}: every position first, then each moved subtree from the bottom up, its top item last.
     */
    void apply(Consumer<PortalNavigationItem> itemUpdater) {
        positions.forEach((itemId, order) -> {
            var item = itemsById.get(itemId);
            item.setOrder(order);
            itemUpdater.accept(item);
        });
        relabels.forEach(relabel -> {
            var item = itemsById.get(relabel.itemId());
            item.setParentId(relabel.parentId());
            item.setRootId(relabel.rootId());
            item.setReferenceType(PortalNavigationReferenceType.API);
            item.setReferenceId(relabel.apiId());
            item.setSegment(relabel.segment());
            itemUpdater.accept(item);
        });
    }

    boolean isEmpty() {
        return positions.isEmpty() && relabels.isEmpty();
    }

    List<String> decisions() {
        return decisions;
    }

    Summary summary() {
        return new Summary(
            apisMigrated,
            itemsMoved,
            sourcedItemsMoved,
            starterPagesMoved,
            segmentsRenamed,
            apisWithSeveralListings,
            itemsLeftUnderApiProducts,
            itemsLeftForPortalAutomation,
            itemsLeftForMissingApis
        );
    }

    record Summary(
        int apisMigrated,
        int itemsMoved,
        int sourcedItemsMoved,
        int starterPagesMoved,
        int segmentsRenamed,
        int apisWithSeveralListings,
        int itemsLeftUnderApiProducts,
        int itemsLeftForPortalAutomation,
        int itemsLeftForMissingApis
    ) {}

    /** The target state of one moved item; {@code parentId} is null for a new top item of the API. */
    private record Relabel(String itemId, String parentId, String rootId, String apiId, String segment) {}

    private void planEnvironment() {
        items
            .stream()
            .filter(PortalNavigationItem.IS_API_ITEM_PREDICATE)
            .filter(row -> !skipIfUnderApiProduct(row))
            // Sorted by API id so that two runs over the same data log and write in the same order
            .collect(Collectors.groupingBy(PortalNavigationItem::getApiId, TreeMap::new, Collectors.toList()))
            .forEach(this::planApi);
    }

    private boolean skipIfUnderApiProduct(PortalNavigationItem row) {
        if (!isUnderApiProduct(row)) {
            return false;
        }
        var left = countBelow(row);
        if (left > 0) {
            itemsLeftUnderApiProducts += left;
            decisions.add(
                "Left %d item(s) under listing [%s] of API [%s]: it is inside an API product".formatted(left, row.getId(), row.getApiId())
            );
        }
        return true;
    }

    private void planApi(String apiId, List<PortalNavigationItem> rows) {
        rows.sort(Comparator.comparing(PortalNavigationItem::getId));
        if (!existingApiIds.contains(apiId)) {
            rows.forEach(row -> leaveForMissingApi(apiId, row));
            return;
        }

        var moving = IntStream.range(0, rows.size())
            .boxed()
            .flatMap(rowIndex ->
                childrenByParentId
                    .getOrDefault(rows.get(rowIndex).getId(), List.of())
                    .stream()
                    .map(child -> new Candidate(child, rowIndex, subtree(child)))
            )
            .filter(candidate -> !skipIfManagedByPortalAutomation(apiId, candidate))
            .collect(Collectors.toCollection(ArrayList::new));
        if (moving.isEmpty()) {
            return;
        }

        apisMigrated++;
        var rowsWithMovingItems = moving.stream().map(Candidate::rowIndex).distinct().count();
        if (rowsWithMovingItems > 1) {
            apisWithSeveralListings++;
            decisions.add(
                "Merged the documentation of %d listings of API [%s]: %s".formatted(
                    rows.size(),
                    apiId,
                    rows.stream().map(PortalNavigationItem::getId).toList()
                )
            );
        }

        var area = rows.getFirst().getArea();
        var existingRoots = items
            .stream()
            .filter(item -> item.getParentId() == null)
            .filter(item -> item.getReferenceType() == PortalNavigationReferenceType.API && apiId.equals(item.getReferenceId()))
            .filter(item -> Objects.equals(item.getArea(), area))
            .toList();

        var listOrder = planPositions(moving, existingRoots);
        moving.sort(Comparator.comparingInt(candidate -> listOrder.indexOf(candidate.top())));
        planRelabels(apiId, moving, existingRoots);
    }

    private void leaveForMissingApi(String apiId, PortalNavigationItem row) {
        var left = countBelow(row);
        if (left > 0) {
            itemsLeftForMissingApis += left;
            decisions.add("Left %d item(s) under listing [%s]: API [%s] no longer exists".formatted(left, row.getId(), apiId));
        }
    }

    private boolean skipIfManagedByPortalAutomation(String apiId, Candidate candidate) {
        var subtree = candidate.subtree();
        if (subtree.stream().noneMatch(ListingDocumentationMigrationPlan::isManagedByPortalAutomation)) {
            return false;
        }
        itemsLeftForPortalAutomation += subtree.size();
        decisions.add(
            "Left [%s] and %d item(s) below it under listing [%s] of API [%s]: Portal automation manages it".formatted(
                candidate.top().getId(),
                subtree.size() - 1,
                candidate.top().getParentId(),
                apiId
            )
        );
        return true;
    }

    /**
     * One list, in the order the server shows under the listing today: by position, and on a tie the
     * listing's own child first (rows taken by id), then by id.
     */
    private List<PortalNavigationItem> planPositions(List<Candidate> moving, List<PortalNavigationItem> existingRoots) {
        var merged = new ArrayList<Positioned>();
        moving.forEach(candidate -> merged.add(new Positioned(candidate.top(), 0, candidate.rowIndex())));
        existingRoots.forEach(root -> merged.add(new Positioned(root, 1, 0)));
        merged.sort(
            Comparator.comparing((Positioned positioned) -> positioned.item().getOrder(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Positioned::group)
                .thenComparingInt(Positioned::rowIndex)
                .thenComparing(positioned -> positioned.item().getId())
        );
        IntStream.range(0, merged.size()).forEach(index -> {
            var item = merged.get(index).item();
            if (!Objects.equals(item.getOrder(), index)) {
                positions.put(item.getId(), index);
            }
        });
        return merged.stream().map(Positioned::item).toList();
    }

    /**
     * Below each top item first, the top item last: a run interrupted part-way leaves the top item
     * under the listing, so the next run still finds its whole subtree. Top items go in list order,
     * so the segments a resumed run hands out are the ones an uninterrupted run would have.
     */
    private void planRelabels(String apiId, List<Candidate> moving, List<PortalNavigationItem> existingRoots) {
        var usedSegments = existingRoots
            .stream()
            .map(PortalNavigationItem::getSegment)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
        moving.forEach(candidate -> planRelabel(apiId, candidate, usedSegments));
    }

    private void planRelabel(String apiId, Candidate candidate, Set<String> usedSegments) {
        var top = candidate.top();
        var segment = claimSegment(apiId, top, usedSegments);
        countMoved(candidate);
        candidate
            .subtree()
            .reversed()
            .stream()
            .map(item ->
                new Relabel(
                    item.getId(),
                    item == top ? null : item.getParentId(),
                    top.getId(),
                    apiId,
                    item == top ? segment : item.getSegment()
                )
            )
            .filter(relabel -> !isAligned(itemsById.get(relabel.itemId()), relabel))
            .forEach(relabels::add);
    }

    private String claimSegment(String apiId, PortalNavigationItem top, Set<String> usedSegments) {
        var segment = uniqueSegment(top, usedSegments);
        usedSegments.add(segment);
        if (!Objects.equals(segment, top.getSegment())) {
            segmentsRenamed++;
            decisions.add(
                "Renamed the segment of [%s] from [%s] to [%s]: API [%s] already has a top item named so".formatted(
                    top.getId(),
                    top.getSegment(),
                    segment,
                    apiId
                )
            );
        }
        return segment;
    }

    private void countMoved(Candidate candidate) {
        var subtree = candidate.subtree();
        itemsMoved += subtree.size();
        sourcedItemsMoved += (int) subtree
            .stream()
            .filter(item -> isSourcedOrBelowSourced(item, candidate.top()))
            .count();
        starterPagesMoved += (int) subtree.stream().filter(PortalNavigationItem.IS_OVERVIEW_PAGE_PREDICATE).count();
    }

    private static String uniqueSegment(PortalNavigationItem top, Set<String> used) {
        var base = top.getSegment() == null || top.getSegment().isBlank() ? Slug.from(top.getTitle()).value() : top.getSegment();
        if (!used.contains(base)) {
            return base;
        }
        return IntStream.iterate(2, n -> n + 1)
            .mapToObj(n -> base + "-" + n)
            .filter(candidate -> !used.contains(candidate))
            .findFirst()
            .orElseThrow();
    }

    private static boolean isAligned(PortalNavigationItem item, Relabel relabel) {
        return (
            Objects.equals(item.getParentId(), relabel.parentId()) &&
            Objects.equals(item.getRootId(), relabel.rootId()) &&
            item.getReferenceType() == PortalNavigationReferenceType.API &&
            relabel.apiId().equals(item.getReferenceId()) &&
            Objects.equals(item.getSegment(), relabel.segment())
        );
    }

    /** Pre-order, the given item first; an item reached twice through corrupt parents is taken once. */
    private List<PortalNavigationItem> subtree(PortalNavigationItem top) {
        var result = new ArrayList<PortalNavigationItem>();
        var visited = new HashSet<String>();
        var toVisit = new ArrayList<PortalNavigationItem>();
        toVisit.add(top);
        while (!toVisit.isEmpty()) {
            var item = toVisit.removeLast();
            if (!visited.add(item.getId())) {
                continue;
            }
            result.add(item);
            toVisit.addAll(childrenByParentId.getOrDefault(item.getId(), List.of()).reversed());
        }
        return result;
    }

    private int countBelow(PortalNavigationItem row) {
        return childrenByParentId
            .getOrDefault(row.getId(), List.of())
            .stream()
            .mapToInt(child -> subtree(child).size())
            .sum();
    }

    private boolean isUnderApiProduct(PortalNavigationItem row) {
        var visited = new HashSet<String>();
        var parentId = row.getParentId();
        while (parentId != null && visited.add(parentId)) {
            var parent = itemsById.get(parentId);
            if (parent == null) {
                return false;
            }
            if (parent.getType() == PortalNavigationItem.Type.API_PRODUCT) {
                return true;
            }
            parentId = parent.getParentId();
        }
        return false;
    }

    private boolean isSourcedOrBelowSourced(PortalNavigationItem item, PortalNavigationItem top) {
        var visited = new HashSet<String>();
        var current = item;
        while (current != null && visited.add(current.getId())) {
            if (hasSource(current)) {
                return true;
            }
            if (current == top) {
                return false;
            }
            current = itemsById.get(current.getParentId());
        }
        return false;
    }

    private static boolean hasSource(PortalNavigationItem item) {
        if (item.getConfiguration() == null || item.getConfiguration().isBlank()) {
            return false;
        }
        try {
            JsonNode source = JSON.readTree(item.getConfiguration()).get(SOURCE);
            return source != null && !source.isNull();
        } catch (Exception e) {
            // Only the summary's count of sourced items depends on it; the item is moved either way
            log.warn("Unable to read the configuration of portal navigation item {}; counting it as not sourced", item.getId(), e);
            return false;
        }
    }

    private static boolean isManagedByPortalAutomation(PortalNavigationItem item) {
        var metadata = item.getAutomationMetadata();
        return metadata != null && metadata.getReferenceType() == AutomationTargetReferenceType.PORTAL;
    }

    private record Candidate(PortalNavigationItem top, int rowIndex, List<PortalNavigationItem> subtree) {}

    /** {@code group} 0 for a listing's child, 1 for a top item the API already owns. */
    private record Positioned(PortalNavigationItem item, int group, int rowIndex) {}
}
