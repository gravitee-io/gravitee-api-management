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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.environment;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Initialize `environmentId` attribute on ApiKey, Plan and Subscriptions with the value found in the corresponding API.
 */
@Component
public class MissingEnvironmentUpgrader extends MongoUpgrader {

    private static final int UPGRADER_BATCH_SIZE = 1000;
    public static final int MISSING_ENVIRONMENT_UPGRADER_ORDER = ThemeTypeUpgrader.THEME_TYPE_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v2";
    }

    @Override
    public boolean upgrade() {
        Set<Boolean> upgradeStatus = new HashSet<>();
        updateEnvironmentFromApi(upgradeStatus);
        updateEnvironmentFromSubscription(upgradeStatus);
        return upgradeStatus.stream().allMatch(Boolean.TRUE::equals);
    }

    private void updateEnvironmentFromApi(final Set<Boolean> upgradeStatus) {
        var projection = Projections.fields(Projections.include("_id", "environmentId"));
        var bulkActions = new ArrayList<UpdateManyModel<Document>>();
        this.getCollection("apis")
            .find()
            .projection(projection)
            .forEach(api ->
                bulkActions.add(
                    new UpdateManyModel<>(
                        Filters.and(Filters.eq("api", api.getString("_id")), Filters.exists("environmentId", false)),
                        Updates.set("environmentId", api.getString("environmentId"))
                    )
                )
            );

        if (!bulkActions.isEmpty()) {
            for (int i = 0; i < bulkActions.size(); i += UPGRADER_BATCH_SIZE) {
                // Get the end index for the sublist, ensuring it doesn't go out of bounds
                int end = Math.min(i + UPGRADER_BATCH_SIZE, bulkActions.size());
                var batchActions = bulkActions.subList(i, end);
                // This upgrade is only done on data created before 3.17.0 as ApiKey#api as been deprecated
                upgradeStatus.add(this.getCollection("keys").bulkWrite(batchActions).wasAcknowledged());
                upgradeStatus.add(this.getCollection("plans").bulkWrite(batchActions).wasAcknowledged());
                upgradeStatus.add(this.getCollection("subscriptions").bulkWrite(batchActions).wasAcknowledged());
            }
        }
    }

    private void updateEnvironmentFromSubscription(final Set<Boolean> upgradeStatus) {
        var projection = Projections.fields(Projections.include("_id", "environmentId"));
        var bulkActions = new ArrayList<UpdateManyModel<Document>>();
        this.getCollection("subscriptions")
            .find()
            .projection(projection)
            .forEach(subscription ->
                bulkActions.add(
                    new UpdateManyModel<>(
                        Filters.and(Filters.in("subscriptions", subscription.getString("_id")), Filters.exists("environmentId", false)),
                        Updates.set("environmentId", subscription.getString("environmentId"))
                    )
                )
            );

        if (!bulkActions.isEmpty()) {
            for (int i = 0; i < bulkActions.size(); i += UPGRADER_BATCH_SIZE) {
                // Get the end index for the sublist, ensuring it doesn't go out of bounds
                int end = Math.min(i + UPGRADER_BATCH_SIZE, bulkActions.size());
                var batchActions = bulkActions.subList(i, end);
                upgradeStatus.add(this.getCollection("keys").bulkWrite(batchActions).wasAcknowledged());
            }
        }
    }

    @Override
    public int getOrder() {
        return MISSING_ENVIRONMENT_UPGRADER_ORDER;
    }
}
