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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemoveOrphanedSubscriptionsUpgrader extends MongoUpgrader {

    public static final int REMOVE_ORPHANED_SUBSCRIPTIONS_ORDER = ThemeTypeUpgrader.THEME_TYPE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        MongoCollection<Document> applications = getCollection("applications");
        MongoCollection<Document> subscriptions = getCollection("subscriptions");

        // Collect all valid application IDs
        Set<String> validAppIds = applications
            .find()
            .projection(Projections.include("_id"))
            .map(doc -> doc.get("_id").toString())
            .into(new HashSet<>());

        if (validAppIds.isEmpty()) {
            log.warn("No valid applications found! Skipping orphaned subscriptions cleanup.");
            return true;
        }

        long orphanCount = subscriptions.countDocuments(Filters.nin("application", validAppIds));
        if (orphanCount == 0) {
            log.info("No orphaned subscriptions found, nothing to delete.");
            return true;
        }
        DeleteResult deleteResult = subscriptions.deleteMany(Filters.nin("application", validAppIds));
        log.info("Deleted {} orphaned subscriptions.", deleteResult.getDeletedCount());

        return deleteResult.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return REMOVE_ORPHANED_SUBSCRIPTIONS_ORDER;
    }
}
