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

        this.createRules = List.of(
            new UniqueItemIdRule(navigationItemsQueryService),
            new HomepageUniquenessRule(navigationItemsQueryService),
            new PageContentExistsRule(pageContentQueryService),
            titleRequiredRule,
            new ApiItemCreateRule(apiProductQueryService),
            new ApiProductItemCreateRule(apiProductQueryService),
            new ApiDocumentationAreaRule(),
            linkUrlRule,
            parentRule,
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
        List<PendingSegmentClaim> pendingSegmentClaims = collectPendingSegmentClaims(creates, updates);

        CreateValidationContext createCtx = new CreateValidationContext(navigationItems, itemsById, pendingItemsById, pendingSegmentClaims);
        UpdateValidationContext updateCtx = new UpdateValidationContext(navigationItems, itemsById, pendingItemsById, pendingSegmentClaims);

        for (BulkCreatePortalNavigationItemValidationRule rule : bulkCreateRules) {
            rule.validate(creates, environmentId, createCtx);
        }
        for (CreatePortalNavigationItem item : creates) {
            applyValidationRules(item, createCtx, environmentId);
        }
        for (PendingUpdate pending : updates) {
            applyValidationRules(pending, updateCtx);
        }
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
        return Stream.concat(fromCreates, fromUpdates).toList();
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
