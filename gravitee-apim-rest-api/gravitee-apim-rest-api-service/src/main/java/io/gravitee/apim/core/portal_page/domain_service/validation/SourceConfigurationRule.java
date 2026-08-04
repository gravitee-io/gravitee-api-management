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

import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

/**
 * A provided source must reference a known fetcher plugin, carry a configuration that plugin
 * accepts, and a valid cron expression when auto-fetch is enabled. The validation itself is
 * delegated to the source domain service, passed as a consumer to keep this package free of
 * dependencies on its parent.
 */
@RequiredArgsConstructor
public class SourceConfigurationRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    private final Consumer<PortalNavigationItemSource> sourceConfigurationValidator;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getSource() != null;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        sourceConfigurationValidator.accept(item.getSource());
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return toUpdate.getSource() != null;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        sourceConfigurationValidator.accept(toUpdate.getSource());
    }
}
