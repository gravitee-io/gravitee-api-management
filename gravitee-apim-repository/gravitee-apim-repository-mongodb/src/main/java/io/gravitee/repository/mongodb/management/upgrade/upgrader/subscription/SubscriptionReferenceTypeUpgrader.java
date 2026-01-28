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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class SubscriptionReferenceTypeUpgrader extends MongoUpgrader {

    public static final int SUBSCRIPTION_REFERENCE_TYPE_UPGRADER_ORDER =
        SubscriptionApplicationNameUpgrader.SUBSCRIPTION_APPLICATION_NAME_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        try {
            log.debug("Starting subscription reference type upgrader");

            var query = Filters.and(
                Filters.exists("api", true),
                Filters.ne("api", null),
                Filters.or(Filters.exists("referenceType", false), Filters.eq("referenceType", null))
            );

            var projection = Projections.fields(Projections.include("_id", "api"));

            List<UpdateManyModel<Document>> bulkActions = new ArrayList<>();

            this.getCollection("subscriptions")
                .find(query)
                .projection(projection)
                .forEach(subscription -> {
                    String apiId = subscription.getString("api");
                    if (apiId != null && !apiId.isEmpty()) {
                        bulkActions.add(
                            new UpdateManyModel<>(
                                Filters.eq("_id", subscription.getString("_id")),
                                Updates.combine(Updates.set("referenceType", "API"), Updates.set("referenceId", apiId))
                            )
                        );
                    }
                });

            if (bulkActions.isEmpty()) {
                log.debug("No subscriptions found requiring reference type update");
                return true;
            }

            log.debug("Updating {} subscription(s) with reference type and reference id", bulkActions.size());
            boolean acknowledged = this.getCollection("subscriptions").bulkWrite(bulkActions).wasAcknowledged();
            log.debug("Subscription reference type upgrade completed successfully");
            return acknowledged;
        } catch (Exception ex) {
            log.error("An error occurred while running the subscription reference type upgrader, skipping it", ex);
            return true;
        }
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_REFERENCE_TYPE_UPGRADER_ORDER;
    }
}
