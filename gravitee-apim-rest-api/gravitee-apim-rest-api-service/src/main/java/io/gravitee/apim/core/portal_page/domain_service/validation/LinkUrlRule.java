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

import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * For LINK type, ensures the URL is valid (create and update).
 */
public class LinkUrlRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getType() == PortalNavigationItemType.LINK;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        validateUrl(item.getUrl());
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return existingItem instanceof PortalNavigationLink;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        validateUrl(toUpdate.getUrl());
    }

    private static void validateUrl(String url) {
        if (!isWellFormedAbsoluteUrl(url)) {
            throw new InvalidUrlFormatException();
        }
    }

    /** Uses {@link URI}-based parsing rather than the {@code new URL(String)} constructor directly, since the latter is more
     * lenient/inconsistent about what it accepts. */
    private static boolean isWellFormedAbsoluteUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            new URI(url).toURL();
            return true;
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }
}
