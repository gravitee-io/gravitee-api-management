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

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

/**
 * Sets {@code environmentId} on the API plans that have none, with the environment of their API.
 *
 * <p>Plans of federated APIs were stored without an environment. Values are read client side and written with plain
 * {@code $set} operators, so the upgrade also runs on Amazon DocumentDB.
 */
@Component
@CustomLog
public class PlanEnvironmentIdUpgrader extends MongoUpgrader {

    public static final int PLAN_ENVIRONMENT_ID_UPGRADER_ORDER = PlanDefinitionVersionUpgrader.PLAN_DEFINITION_VERSION_UPGRADER_ORDER + 1;

    static final int BATCH_SIZE = 1000;

    private static final String ID = "_id";

    private static final String PLANS = "plans";

    private static final String APIS = "apis";

    private static final String API = "api";

    private static final String REFERENCE_TYPE = "referenceType";

    private static final String REFERENCE_ID = "referenceId";

    private static final String ENVIRONMENT_ID = "environmentId";

    private static final Bson NO_ENVIRONMENT = Filters.eq(ENVIRONMENT_ID, null);

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            log.debug("Starting plan environment id upgrader");
            boolean acknowledged = true;
            try (
                MongoCursor<Document> cursor = getCollection(PLANS)
                    .find(NO_ENVIRONMENT)
                    .projection(Projections.include(ID, API, REFERENCE_TYPE, REFERENCE_ID))
                    .batchSize(BATCH_SIZE)
                    .iterator()
            ) {
                var planApiIds = new HashMap<Object, String>();
                while (cursor.hasNext()) {
                    var plan = cursor.next();
                    planApiIds.put(plan.get(ID), apiIdOf(plan));
                    if (planApiIds.size() >= BATCH_SIZE) {
                        acknowledged &= updateBatch(planApiIds);
                        planApiIds.clear();
                    }
                }
                if (!planApiIds.isEmpty()) {
                    acknowledged &= updateBatch(planApiIds);
                }
            }
            return acknowledged;
        });
    }

    private static String apiIdOf(Document plan) {
        var referenceType = plan.getString(REFERENCE_TYPE);
        if (referenceType == null) {
            return plan.getString(API);
        }
        return "API".equals(referenceType) ? plan.getString(REFERENCE_ID) : null;
    }

    private boolean updateBatch(Map<Object, String> planApiIds) {
        var apiEnvironments = new HashMap<String, String>();
        getCollection(APIS)
            .find(Filters.in(ID, planApiIds.values()))
            .projection(Projections.include(ID, ENVIRONMENT_ID))
            .forEach(api -> apiEnvironments.put(api.getString(ID), api.getString(ENVIRONMENT_ID)));

        List<WriteModel<Document>> updates = new ArrayList<>();
        planApiIds.forEach((planId, apiId) -> {
            var environmentId = apiEnvironments.get(apiId);
            if (environmentId != null) {
                // Re-check "no environment yet" on the write: a plan updated by another node in between is not overwritten.
                updates.add(
                    new UpdateOneModel<>(Filters.and(Filters.eq(ID, planId), NO_ENVIRONMENT), Updates.set(ENVIRONMENT_ID, environmentId))
                );
            }
        });
        if (updates.isEmpty()) {
            return true;
        }
        return getCollection(PLANS).bulkWrite(updates, new BulkWriteOptions().ordered(false)).wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return PLAN_ENVIRONMENT_ID_UPGRADER_ORDER;
    }
}
