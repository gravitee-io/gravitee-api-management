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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator;
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiDocumentationAreaRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiItemCreateRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiItemUpdateRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiProductItemCreateRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiProductItemUpdateRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.BulkCreatePortalNavigationItemValidationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.CreatePortalNavigationItemValidationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.CreateValidationContext;
import io.gravitee.apim.core.portal_page.domain_service.validation.DuplicateApiIdsInPayloadRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.DuplicateApiProductIdsInPayloadRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ExternalSourceItemTypeRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.FileListingSourceOnFolderRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.HomepageUniquenessRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.LinkUrlRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.PageContentExistsRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ParentRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.PendingSegmentClaim;
import io.gravitee.apim.core.portal_page.domain_service.validation.SegmentConflictRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.SourceAutomationExclusivityRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.SourceConfigurationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.SourcedAncestorFinder;
import io.gravitee.apim.core.portal_page.domain_service.validation.SourcedItemReadOnlyRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.SubscriptionFormContentTypeRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.SubscriptionFormNoParentRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.SubscriptionFormUniquenessRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.TitleRequiredRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.TypeConsistencyRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.UniqueItemIdRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.UpdatePortalNavigationItemValidationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.UpdateValidationContext;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DomainService
public class PortalNavigationItemValidatorService implements PortalNavigationValidator {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final List<BulkCreatePortalNavigationItemValidationRule> bulkCreateRules;
    private final List<CreatePortalNavigationItemValidationRule> createRules;
    private final List<UpdatePortalNavigationItemValidationRule> updateRules;

    public PortalNavigationItemValidatorService(
        PortalNavigationItemsQueryService navigationItemsQueryService,
        PortalPageContentQueryService pageContentQueryService,
        ApiProductQueryService apiProductQueryService,
        PortalNavigationItemSourceDomainService portalNavigationItemSourceDomainService
    ) {
        this.navigationItemsQueryService = navigationItemsQueryService;
        this.bulkCreateRules = List.of(new DuplicateApiIdsInPayloadRule(), new DuplicateApiProductIdsInPayloadRule());

        // Rules applied on both update and create
        var titleRequiredRule = new TitleRequiredRule();
        var parentRule = new ParentRule(navigationItemsQueryService);
        var segmentConflictRule = new SegmentConflictRule(navigationItemsQueryService);
        var linkUrlRule = new LinkUrlRule();
        var externalSourceItemTypeRule = new ExternalSourceItemTypeRule();
        var sourceConfigurationRule = new SourceConfigurationRule(portalNavigationItemSourceDomainService::validateSourceConfiguration);
        var sourceAutomationExclusivityRule = new SourceAutomationExclusivityRule(navigationItemsQueryService, pageContentQueryService);
        var sourcedItemReadOnlyRule = new SourcedItemReadOnlyRule(new SourcedAncestorFinder(navigationItemsQueryService));
        var fileListingSourceOnFolderRule = new FileListingSourceOnFolderRule(portalNavigationItemSourceDomainService::supportsFileListing);
        var subscriptionFormNoParentRule = new SubscriptionFormNoParentRule();
        var subscriptionFormUniquenessRule = new SubscriptionFormUniquenessRule(navigationItemsQueryService);

        this.createRules = List.of(
            new UniqueItemIdRule(navigationItemsQueryService),
            new HomepageUniquenessRule(navigationItemsQueryService),
            subscriptionFormUniquenessRule,
            new PageContentExistsRule(pageContentQueryService),
            new SubscriptionFormContentTypeRule(pageContentQueryService),
            titleRequiredRule,
            new ApiItemCreateRule(apiProductQueryService),
            new ApiProductItemCreateRule(apiProductQueryService),
            new ApiDocumentationAreaRule(),
            linkUrlRule,
            parentRule,
            subscriptionFormNoParentRule,
            segmentConflictRule,
            externalSourceItemTypeRule,
            sourceConfigurationRule,
            sourceAutomationExclusivityRule,
            sourcedItemReadOnlyRule
        );
        this.updateRules = List.of(
            new TypeConsistencyRule(),
            titleRequiredRule,
            new ApiItemUpdateRule(apiProductQueryService),
            new ApiProductItemUpdateRule(),
            parentRule,
            subscriptionFormNoParentRule,
            subscriptionFormUniquenessRule,
            segmentConflictRule,
            linkUrlRule,
            externalSourceItemTypeRule,
            sourceConfigurationRule,
            sourceAutomationExclusivityRule,
            sourcedItemReadOnlyRule,
            fileListingSourceOnFolderRule
        );
    }

    @Override
    public void validate(List<CreatePortalNavigationItem> creates, List<PendingUpdate> updates, String environmentId) {
        List<PortalNavigationItem> navigationItems = shouldFetch(creates, updates) ? fetchAllNavigationItems(environmentId) : List.of();
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById = navigationItems
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
        Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById = creates
            .stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(CreatePortalNavigationItem::getId, Function.identity(), (first, ignored) -> first));

        List<PendingUpdate> augmentedUpdates = withDescendantsOfPendingContainers(creates, updates, environmentId);
        List<PendingSegmentClaim> pendingSegmentClaims = collectPendingSegmentClaims(creates, augmentedUpdates);
        Map<PortalNavigationItemId, PendingUpdate> pendingUpdatesByExistingId = updates
            .stream()
            .filter(u -> u.existing() != null && u.existing().getId() != null)
            .collect(Collectors.toMap(u -> u.existing().getId(), Function.identity(), (first, ignored) -> first));

        CreateValidationContext createCtx = new CreateValidationContext(
            navigationItems,
            itemsById,
            pendingItemsById,
            pendingUpdatesByExistingId,
            pendingSegmentClaims
        );
        UpdateValidationContext updateCtx = new UpdateValidationContext(
            navigationItems,
            itemsById,
            pendingItemsById,
            pendingUpdatesByExistingId,
            pendingSegmentClaims
        );

        for (BulkCreatePortalNavigationItemValidationRule rule : bulkCreateRules) {
            rule.validate(creates, environmentId, createCtx);
        }
        for (CreatePortalNavigationItem item : creates) {
            applyValidationRules(item, createCtx, environmentId);
        }
        for (PendingUpdate pending : augmentedUpdates) {
            applyValidationRules(pending, updateCtx);
        }
    }

    // Include pre-existing descendants of pending container creates and of pending container updates that change visibility.
    private List<PendingUpdate> withDescendantsOfPendingContainers(
        List<CreatePortalNavigationItem> creates,
        List<PendingUpdate> updates,
        String environmentId
    ) {
        var descendantsOfCreates = creates
            .stream()
            .filter(PortalNavigationItemValidatorService::isPendingContainer)
            .flatMap(container -> descendantsOf(container.getId(), environmentId).stream());
        var descendantsOfUpdates = updates
            .stream()
            .filter(PortalNavigationItemValidatorService::isContainerVisibilityChange)
            .flatMap(update -> descendantsOf(update.existing().getId(), environmentId).stream());
        var descendantUpdates = Stream.concat(descendantsOfCreates, descendantsOfUpdates).map(
            PortalNavigationItemValidatorService::asUnchangedUpdate
        );
        return Stream.concat(updates.stream(), descendantUpdates).toList();
    }

    private static boolean isPendingContainer(CreatePortalNavigationItem item) {
        return item.getId() != null && item.getType().isContainer();
    }

    private static boolean isContainerVisibilityChange(PendingUpdate update) {
        var existing = update.existing();
        var toUpdate = update.toUpdate();
        return (
            existing != null &&
            existing.getId() != null &&
            existing.getType().isContainer() &&
            toUpdate.getVisibility() != null &&
            toUpdate.getVisibility() != existing.getVisibility()
        );
    }

    private List<PortalNavigationItem> descendantsOf(PortalNavigationItemId containerId, String environmentId) {
        return navigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, containerId);
    }

    // Wraps a persisted descendant as a no-op update so the update rules re-run against the pending parent.
    private static PendingUpdate asUnchangedUpdate(PortalNavigationItem descendant) {
        return new PendingUpdate(unchangedUpdateFor(descendant), descendant);
    }

    private static UpdatePortalNavigationItem unchangedUpdateFor(PortalNavigationItem item) {
        return UpdatePortalNavigationItem.builder()
            .type(item.getType())
            .title(item.getTitle())
            .segment(item.getSegment())
            .order(item.getOrder())
            .parentId(item.getParentId())
            .visibility(item.getVisibility())
            .published(item.getPublished())
            .source(item.getSource())
            .build();
    }

    @Override
    public void validateAll(List<CreatePortalNavigationItem> items, String environmentId) {
        validate(items, List.of(), environmentId);
    }

    @Override
    public void validateOne(CreatePortalNavigationItem item, String environmentId) {
        validate(List.of(item), List.of(), environmentId);
    }

    @Override
    public void validateToUpdate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        validate(List.of(), List.of(new PendingUpdate(toUpdate, existingItem)), existingItem.getEnvironmentId());
    }

    private static List<PendingSegmentClaim> collectPendingSegmentClaims(
        List<CreatePortalNavigationItem> creates,
        List<PendingUpdate> updates
    ) {
        var fromCreates = creates
            .stream()
            .filter(c -> c.getId() != null)
            .map(PendingSegmentClaim::forCreate);
        var fromUpdates = updates.stream().map(u -> PendingSegmentClaim.forUpdate(u.existing(), u.toUpdate()));
        // Record the old slot when an update relocates or renames, so a same-batch sibling can safely take it over.
        var released = updates
            .stream()
            .filter(PortalNavigationItemValidatorService::vacatesSlot)
            .map(u -> PendingSegmentClaim.forRelease(u.existing()));
        return Stream.concat(Stream.concat(fromCreates, fromUpdates), released).toList();
    }

    private static boolean vacatesSlot(PendingUpdate update) {
        var existing = update.existing();
        var toUpdate = update.toUpdate();
        return (
            !Objects.equals(existing.getParentId(), toUpdate.getParentId()) || !Objects.equals(existing.getSegment(), toUpdate.getSegment())
        );
    }

    private static boolean shouldFetch(List<CreatePortalNavigationItem> creates, List<PendingUpdate> updates) {
        return (hasApiOrApiProductItems(creates) || updates.stream().anyMatch(u -> u.existing() instanceof PortalNavigationItemContainer));
    }

    private List<PortalNavigationItem> fetchAllNavigationItems(String environmentId) {
        var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).root(false).build();
        return navigationItemsQueryService.search(criteria);
    }

    private static boolean hasApiOrApiProductItems(List<CreatePortalNavigationItem> items) {
        return items
            .stream()
            .anyMatch(item -> item.getType() == PortalNavigationItemType.API || item.getType() == PortalNavigationItemType.API_PRODUCT);
    }

    private void applyValidationRules(PendingUpdate pending, UpdateValidationContext updateCtx) {
        for (UpdatePortalNavigationItemValidationRule rule : updateRules) {
            if (rule.appliesTo(pending.toUpdate(), pending.existing())) {
                rule.validate(pending.toUpdate(), pending.existing(), updateCtx);
            }
        }
    }

    private void applyValidationRules(CreatePortalNavigationItem item, CreateValidationContext createCtx, String environmentId) {
        for (CreatePortalNavigationItemValidationRule rule : createRules) {
            if (rule.appliesTo(item)) {
                rule.validate(item, environmentId, createCtx);
            }
        }
    }
}
