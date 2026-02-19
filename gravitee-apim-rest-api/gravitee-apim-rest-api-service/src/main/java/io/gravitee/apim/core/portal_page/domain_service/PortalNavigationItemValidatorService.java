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
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiItemCreateRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ApiItemUpdateRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.BulkCreatePortalNavigationItemValidationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.CreatePortalNavigationItemValidationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.CreateValidationContext;
import io.gravitee.apim.core.portal_page.domain_service.validation.DuplicateApiIdsInPayloadRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.HomepageUniquenessRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.LinkUrlRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.PageContentExistsRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.ParentRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.TitleRequiredRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.TypeConsistencyRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.UniqueItemIdRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.UpdatePortalNavigationItemValidationRule;
import io.gravitee.apim.core.portal_page.domain_service.validation.UpdateValidationContext;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@DomainService
public class PortalNavigationItemValidatorService {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final List<BulkCreatePortalNavigationItemValidationRule> bulkCreateRules;
    private final List<CreatePortalNavigationItemValidationRule> createRules;
    private final List<UpdatePortalNavigationItemValidationRule> updateRules;

    public PortalNavigationItemValidatorService(
        PortalNavigationItemsQueryService navigationItemsQueryService,
        PortalPageContentQueryService pageContentQueryService
    ) {
        this.navigationItemsQueryService = navigationItemsQueryService;
        this.bulkCreateRules = List.of(new DuplicateApiIdsInPayloadRule());

        // Rules applied on both update and create
        var titleRequiredRule = new TitleRequiredRule();
        var parentRule = new ParentRule(navigationItemsQueryService);
        var linkUrlRule = new LinkUrlRule();

        this.createRules = List.of(
            new UniqueItemIdRule(navigationItemsQueryService),
            new HomepageUniquenessRule(navigationItemsQueryService),
            new PageContentExistsRule(pageContentQueryService),
            titleRequiredRule,
            new ApiItemCreateRule(),
            linkUrlRule,
            parentRule
        );
        this.updateRules = List.of(new TypeConsistencyRule(), titleRequiredRule, new ApiItemUpdateRule(), parentRule, linkUrlRule);
    }

    public void validateAll(List<CreatePortalNavigationItem> items, String environmentId) {
        Set<String> seenApiIdsInPayload = new HashSet<>();
        CreateValidationContext ctx = new CreateValidationContext(List.of(), Map.of(), seenApiIdsInPayload);
        for (BulkCreatePortalNavigationItemValidationRule rule : bulkCreateRules) {
            rule.validate(items, environmentId, ctx);
        }

        List<PortalNavigationItem> navigationItems = hasApiItems(items) ? fetchAllNavigationItems(environmentId) : List.of();
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById = navigationItems
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
        ctx = new CreateValidationContext(navigationItems, itemsById, seenApiIdsInPayload);

        for (CreatePortalNavigationItem item : items) {
            for (CreatePortalNavigationItemValidationRule rule : createRules) {
                if (rule.appliesTo(item)) {
                    rule.validate(item, environmentId, ctx);
                }
            }
        }
    }

    public void validateOne(CreatePortalNavigationItem item, String environmentId) {
        Set<String> seenApiIdsInPayload = new HashSet<>();
        List<PortalNavigationItem> navigationItems = item.getType() == PortalNavigationItemType.API
            ? fetchAllNavigationItems(environmentId)
            : List.of();
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById = navigationItems
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
        CreateValidationContext ctx = new CreateValidationContext(navigationItems, itemsById, seenApiIdsInPayload);

        for (CreatePortalNavigationItemValidationRule rule : createRules) {
            if (rule.appliesTo(item)) {
                rule.validate(item, environmentId, ctx);
            }
        }
    }

    public void validateToUpdate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        List<PortalNavigationItem> navigationItems;
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById;
        if (existingItem instanceof PortalNavigationApi) {
            navigationItems = fetchAllNavigationItems(existingItem.getEnvironmentId());
            itemsById = navigationItems.stream().collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
        } else {
            navigationItems = List.of();
            itemsById = Map.of();
        }
        UpdateValidationContext ctx = new UpdateValidationContext(navigationItems, itemsById);

        for (UpdatePortalNavigationItemValidationRule rule : updateRules) {
            if (rule.appliesTo(toUpdate, existingItem)) {
                rule.validate(toUpdate, existingItem, ctx);
            }
        }
    }

    private List<PortalNavigationItem> fetchAllNavigationItems(String environmentId) {
        var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).root(false).build();
        return navigationItemsQueryService.search(criteria);
    }

    private boolean hasApiItems(List<CreatePortalNavigationItem> items) {
        return items.stream().anyMatch(item -> item.getType() == PortalNavigationItemType.API);
    }
}
