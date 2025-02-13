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
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionApplicationNameUpgrader extends MongoUpgrader {

    public static final int SUBSCRIPTION_APPLICATION_NAME_UPGRADER_ORDER = SHARED_POLICY_GROUP_HISTORIES_PHASE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        // Fetch all documents from applications
        var applicationDocs = getCollection("applications").find().projection(new Document("_id", 1).append("name", 1));

        var bulkActions = StreamSupport
            .stream(applicationDocs.spliterator(), false)
            .map(applicationDoc -> {
                String applicationId = applicationDoc.get("_id").toString();
                String applicationName = applicationDoc.getString("name");
                // Create UpdateManyModel for each application
                return new UpdateManyModel<Document>(
                    Filters.eq("application", applicationId),
                    Updates.set("applicationName", applicationName)
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
}
