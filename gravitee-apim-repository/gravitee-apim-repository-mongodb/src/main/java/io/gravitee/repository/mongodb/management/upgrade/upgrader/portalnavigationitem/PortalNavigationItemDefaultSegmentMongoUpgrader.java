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
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormValidationConstraintsMongoUpgrader;
import java.util.List;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Backfills {@code segment} from {@code title} on portal navigation items that pre-date the
 * segment field. After this runs every row has a non-null segment, so domain readers can call
 * {@code getSegment()} directly without a fallback path.
 */
@Component
public class PortalNavigationItemDefaultSegmentMongoUpgrader extends MongoUpgrader {

    public static final int PORTAL_NAVIGATION_ITEM_DEFAULT_SEGMENT_MONGO_UPGRADER_ORDER =
        SubscriptionFormValidationConstraintsMongoUpgrader.SUBSCRIPTION_FORM_VALIDATION_CONSTRAINTS_MONGO_UPGRADER_ORDER + 1;

    public static final String SEGMENT = "segment";
    public static final String TITLE = "title";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        final List<Document> pipeline = List.of(new Document("$set", new Document(SEGMENT, "$" + TITLE)));
        var result = this.getCollection("portal_navigation_items").updateMany(Filters.eq(SEGMENT, null), pipeline);
        return result.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_DEFAULT_SEGMENT_MONGO_UPGRADER_ORDER;
    }
}
