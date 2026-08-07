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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.portalnavigationindex;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Compound index on portal_navigation_items to support {@code findByAutomationReference} queries.
 * Mirrors the JDBC {@code idx_portal_navigation_items_automation_reference} index.
 */
@Component
public class PortalNavigationItemAutomationReferenceIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("portal_navigation_items")
            .name("e1art1ari1")
            .key("environmentId", ascending())
            .key("automationMetadata.referenceType", ascending())
            .key("automationMetadata.referenceId", ascending())
            .build();
    }
}
