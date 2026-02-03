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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.ArrayList;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class PlanReferenceTypeUpgrader extends MongoUpgrader {

    public static final int PLAN_REFERENCE_TYPE_UPGRADER_ORDER = PlanDefinitionVersionUpgrader.PLAN_DEFINITION_VERSION_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        try {
            log.debug("Starting plan reference type upgrader");
            var query = Filters.and(
                Filters.exists("api", true),
                Filters.ne("api", null),
                Filters.or(Filters.exists("referenceType", false), Filters.eq("referenceType", null))
            );

            var projection = Projections.fields(Projections.include("_id", "api"));

            var bulkActions = new ArrayList<UpdateManyModel<Document>>();

            this.getCollection("plans")
                .find(query)
                .projection(projection)
                .forEach(plan -> {
                    String apiId = plan.getString("api");
                    if (apiId != null && !apiId.isEmpty()) {
                        bulkActions.add(
                            new UpdateManyModel<>(
                                Filters.eq("_id", plan.getString("_id")),
                                Updates.combine(Updates.set("referenceType", "API"), Updates.set("referenceId", apiId))
                            )
                        );
                    }
                });

            if (bulkActions.isEmpty()) {
                log.debug("No plans found requiring reference type update");
                return true;
            }

            log.debug("Updating {} plan(s) with reference type and reference id", bulkActions.size());
            boolean acknowledged = this.getCollection("plans").bulkWrite(bulkActions).wasAcknowledged();
            log.debug("Plan reference type upgrade completed successfully");
            return acknowledged;
        } catch (Exception ex) {
            log.error("An error occurred while running the plan reference type upgrader, skipping it", ex);
            return true;
        }
    }

    @Override
    public int getOrder() {
        return PLAN_REFERENCE_TYPE_UPGRADER_ORDER;
    }
}
