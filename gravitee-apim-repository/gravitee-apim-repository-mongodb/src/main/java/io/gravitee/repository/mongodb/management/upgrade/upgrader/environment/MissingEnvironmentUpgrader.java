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

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.dashboards.DashboardTypeUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import java.util.ArrayList;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Initialize `environmentId` attribute on ApiKey, Plan and Subscriptions with the value found in the corresponding API.
 */
@Component
public class MissingEnvironmentUpgrader extends MongoUpgrader {

    public static final int MISSING_ENVIRONMENT_UPGRADER_ORDER = ThemeTypeUpgrader.THEME_TYPE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var projection = Projections.fields(Projections.include("_id", "environmentId"));

        var bulkActions = new ArrayList<UpdateManyModel<Document>>();
        template
            .getCollection("apis")
            .find()
            .projection(projection)
            .forEach(api ->
                bulkActions.add(
                    new UpdateManyModel<>(
                        new Document("api", api.getString("_id")),
                        new Document("$set", new Document("environmentId", api.getString("environmentId")))
                    )
                )
            );

        if (!bulkActions.isEmpty()) {
            return (
                template.getCollection("keys").bulkWrite(bulkActions).wasAcknowledged() &&
                template.getCollection("plans").bulkWrite(bulkActions).wasAcknowledged() &&
                template.getCollection("subscriptions").bulkWrite(bulkActions).wasAcknowledged()
            );
        }
        return true;
    }

    @Override
    public int getOrder() {
        return MISSING_ENVIRONMENT_UPGRADER_ORDER;
    }
}
