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
import org.springframework.stereotype.Component;

/**
 * Backfills the catalog fields on subscription form documents that predate them: every existing
 * form was the only one of its environment, so it becomes the environment default, named "Default".
 * Mirrors the JDBC changeset {@code 4.13.0_19_add_catalog_columns_to_subscription_forms}.
 */
@Component
public class SubscriptionFormCatalogMongoUpgrader extends MongoUpgrader {

    public static final int SUBSCRIPTION_FORM_CATALOG_MONGO_UPGRADER_ORDER =
        SubscriptionFormValidationConstraintsMongoUpgrader.SUBSCRIPTION_FORM_VALIDATION_CONSTRAINTS_MONGO_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var collection = this.getCollection("subscription_forms");
        var nameResult = collection.updateMany(Filters.exists("name", false), Updates.set("name", "Default"));
        var defaultResult = collection.updateMany(Filters.exists("defaultForm", false), Updates.set("defaultForm", true));
        return nameResult.wasAcknowledged() && defaultResult.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_CATALOG_MONGO_UPGRADER_ORDER;
    }
}
