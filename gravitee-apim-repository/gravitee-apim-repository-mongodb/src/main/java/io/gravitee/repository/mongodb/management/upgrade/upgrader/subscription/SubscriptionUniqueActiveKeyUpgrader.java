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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.subscription;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.IndexMongoUpgrader;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Backfills the unique active key of the subscriptions written before it was maintained.
 * <p>
 * It uses the client with long timeouts of the index upgraders: the default client gives up after one second. It never
 * reports a failure, which would stop the node, and the key is not enforced yet: the unique index is only created once
 * every node maintains the key, after another repair. Run it again by removing its record from the {@code upgrades}
 * collection, for instance after a rollback to a previous version.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component("SubscriptionsUniqueActiveKeyUpgrader")
public class SubscriptionUniqueActiveKeyUpgrader extends IndexMongoUpgrader {

    /**
     * After {@link SubscriptionReferenceTypeUpgrader}, which fills the reference the key derives from.
     */
    public static final int SUBSCRIPTION_UNIQUE_ACTIVE_KEY_UPGRADER_ORDER =
        SubscriptionReferenceTypeUpgrader.SUBSCRIPTION_REFERENCE_TYPE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        try {
            new SubscriptionUniqueActiveKeyRepair(subscriptionsCollection()).repair();
        } catch (Exception e) {
            log.error(
                "Unexpected error while repairing the unique active key of subscriptions, it will be repaired again before being enforced",
                e
            );
        }
        return true;
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_UNIQUE_ACTIVE_KEY_UPGRADER_ORDER;
    }

    MongoCollection<Document> subscriptionsCollection() {
        return getCollection("subscriptions").block();
    }
}
