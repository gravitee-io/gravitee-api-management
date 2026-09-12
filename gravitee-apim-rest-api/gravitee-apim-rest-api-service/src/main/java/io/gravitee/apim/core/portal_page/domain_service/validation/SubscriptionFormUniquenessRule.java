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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.SubscriptionFormAlreadyPublishedException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.RequiredArgsConstructor;

/**
 * For SUBSCRIPTION_FORM area, ensures at most one item is published per environment. There is no
 * stored "default" flag: the environment's default subscription form is derived as "the one
 * published item" (see {@link PortalNavigationItemsQueryService#findPublishedSubscriptionForm}), so
 * this rule is what keeps that derivation unambiguous.
 */
@RequiredArgsConstructor
public class SubscriptionFormUniquenessRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getArea() == PortalArea.SUBSCRIPTION_FORM && Boolean.TRUE.equals(item.getPublished());
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        var alreadyPublished = navigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(environmentId, PortalArea.SUBSCRIPTION_FORM, item.getReference())
            .stream()
            .anyMatch(existing -> Boolean.TRUE.equals(existing.getPublished()));
        if (alreadyPublished) {
            throw new SubscriptionFormAlreadyPublishedException();
        }
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return existingItem.getArea() == PortalArea.SUBSCRIPTION_FORM && Boolean.TRUE.equals(toUpdate.getPublished());
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        var anotherAlreadyPublished = navigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                existingItem.getEnvironmentId(),
                PortalArea.SUBSCRIPTION_FORM,
                existingItem.getReference()
            )
            .stream()
            .anyMatch(existing -> !existing.getId().equals(existingItem.getId()) && Boolean.TRUE.equals(existing.getPublished()));
        if (anotherAlreadyPublished) {
            throw new SubscriptionFormAlreadyPublishedException();
        }
    }
}
