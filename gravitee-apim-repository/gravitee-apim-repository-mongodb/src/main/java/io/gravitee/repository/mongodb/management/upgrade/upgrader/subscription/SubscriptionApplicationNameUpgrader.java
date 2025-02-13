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

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.sharedpolicygroups.SharedPolicyGroupHistoriesPhaseUpgrader.SHARED_POLICY_GROUP_HISTORIES_PHASE_UPGRADER_ORDER;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateManyModel;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionApplicationNameUpgrader extends MongoUpgrader {

    public static final int SUBSCRIPTION_APPLICATION_NAME_UPGRADER_ORDER = SHARED_POLICY_GROUP_HISTORIES_PHASE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        // 1. Find plan IDs with security != 'KEY_LESS'
        // note: security isn't indexed so we need to handle filtering in code to satisfy notablescan rule
        var planDocs = getCollection("plans").find().projection(new Document("_id", 1).append("security", 1));
        List<String> planIds = StreamSupport
            .stream(planDocs.spliterator(), false)
            .filter(doc -> !"KEY_LESS".equals(doc.getString("security")))
            .map(doc -> doc.get("_id").toString())
            .collect(Collectors.toList());

        // 2. Find subscriptions with those plan IDs
        var subscriptionDocs = getCollection("subscriptions")
            .find(Filters.and(Filters.in("plan", planIds), Filters.exists("applicationName", false)))
            .projection(new Document("_id", 1).append("application", 1));
        var bulkActions = StreamSupport
            .stream(subscriptionDocs.spliterator(), false)
            .map(subscriptionDoc -> {
                // 3. Pair with applications
                String applicationId = subscriptionDoc.get("application").toString();
                return new SubscriptionApplicationPair(
                    subscriptionDoc,
                    getCollection("applications").find(Filters.eq("_id", applicationId)).first()
                );
            })
            .filter(pair -> pair.application() != null)
            .map(pair -> {
                String applicationName = pair.application.getString("name");
                // 4. Update subscription with application name
                return new UpdateManyModel<Document>(
                    Filters.eq("_id", pair.subscription.get("_id").toString()),
                    new Document("$set", new Document("applicationName", applicationName))
                );
            })
            .collect(Collectors.toList());

        // Execute bulk actions
        if (!bulkActions.isEmpty()) {
            return getCollection("subscriptions").bulkWrite(bulkActions).wasAcknowledged();
        }
        return true;
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_APPLICATION_NAME_UPGRADER_ORDER;
    }

    record SubscriptionApplicationPair(Document subscription, Document application) {}
}
