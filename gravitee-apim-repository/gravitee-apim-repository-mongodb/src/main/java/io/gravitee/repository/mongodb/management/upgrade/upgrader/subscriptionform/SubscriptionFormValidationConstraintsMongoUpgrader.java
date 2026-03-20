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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem.PortalNavigationItemDefaultRootIdMongoUpgrader;
import org.springframework.stereotype.Component;

/**
 * Backfills the {@code validationConstraints} field with an empty JSON object ({@code "{}"}) for
 * subscription form documents that predate the introduction of this field.
 *
 * <p>MongoDB does not apply column defaults, so existing documents simply lack the field (stored as
 * {@code null} / missing). The subsequent {@code SubscriptionFormConstraintsUpgrader} then replaces
 * {@code "{}"} with the actual constraints derived from each form's GMD content.</p>
 */
@Component
public class SubscriptionFormValidationConstraintsMongoUpgrader extends MongoUpgrader {

    public static final int SUBSCRIPTION_FORM_VALIDATION_CONSTRAINTS_MONGO_UPGRADER_ORDER =
        PortalNavigationItemDefaultRootIdMongoUpgrader.PORTAL_NAVIGATION_ITEM_DEFAULT_ROOT_ID_MONGO_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var result = this.getCollection("subscription_forms").updateMany(
            Filters.eq("validationConstraints", null),
            Updates.set("validationConstraints", "{}")
        );
        return result.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_VALIDATION_CONSTRAINTS_MONGO_UPGRADER_ORDER;
    }
}
