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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.plan;

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.dashboards.DashboardTypeUpgrader;
import java.util.ArrayList;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Initialize `definitionVersion` attribute with the value found in the corresponding API.
 */
@Component
public class PlanDefinitionVersionUpgrader extends MongoUpgrader {

    public static final int PLAN_DEFINITION_VERSION_UPGRADER_ORDER = DashboardTypeUpgrader.DASHBOARD_TYPE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var query = new Document("definitionVersion", DefinitionVersion.V4.name());
        var projection = Projections.fields(Projections.include("_id", "definitionVersion"));

        var bulkActions = new ArrayList<UpdateManyModel<Document>>();
        template
            .getCollection("apis")
            .find(query)
            .projection(projection)
            .forEach(v4Api -> {
                bulkActions.add(
                    new UpdateManyModel<>(
                        new Document("api", v4Api.getString("_id")),
                        new Document("$set", new Document("definitionVersion", DefinitionVersion.V4.name()))
                    )
                );
            });

        if (!bulkActions.isEmpty()) {
            return template.getCollection("plans").bulkWrite(bulkActions).wasAcknowledged();
        }
        return true;
    }

    @Override
    public int getOrder() {
        return PLAN_DEFINITION_VERSION_UPGRADER_ORDER;
    }
}
