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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import org.springframework.stereotype.Component;

/**
 * Backfills the {@code visibility} on portal navigation items that were persisted before this
 * field was mandatory, so that Spring Data MongoDB can deserialize them into the now-{@code
 * @Nonnull} core domain model, and so that DB-filtered queries (which exclude null-visibility
 * documents) and Java-side viewer filtering (which defaults them to PUBLIC) no longer disagree
 * about whether such an item is visible to anonymous portal viewers.
 *
 * Must run before {@code EnvironmentsDefaultPortalNavigationItemsUpgrader} (order 712), whose guard
 * query fully deserializes existing items into the {@code @Nonnull}-annotated core domain model.
 */
@Component
public class PortalNavigationItemDefaultVisibilityMongoUpgrader extends MongoUpgrader {

    public static final int PORTAL_NAVIGATION_ITEM_DEFAULT_VISIBILITY_MONGO_UPGRADER_ORDER =
        PortalNavigationItemDefaultReferenceMongoUpgrader.PORTAL_NAVIGATION_ITEM_DEFAULT_REFERENCE_MONGO_UPGRADER_ORDER + 1;

    public static final String VISIBILITY = "visibility";
    public static final String DEFAULT_VISIBILITY = "PUBLIC";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var result = this.getCollection("portal_navigation_items").updateMany(
            Filters.eq(VISIBILITY, null),
            Updates.set(VISIBILITY, DEFAULT_VISIBILITY)
        );
        return result.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_DEFAULT_VISIBILITY_MONGO_UPGRADER_ORDER;
    }
}
