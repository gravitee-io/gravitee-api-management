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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.keys;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.environment.MissingEnvironmentUpgrader;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Initialize `environmentId` attribute on ApiKey, Plan and Subscriptions with the value found in the corresponding API.
 */
@Component
public class ApiKeyFederatedUpgrader extends MongoUpgrader {

    public static final int API_KEY_FEDERATED_UPGRADER_ORDER = MissingEnvironmentUpgrader.MISSING_ENVIRONMENT_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var unsetFederatedApiKeyQuery = new Document("federated", new Document("$exists", false));
        var updateOperation = new Document("$set", new Document("federated", false));

        return this.getCollection("keys").updateMany(unsetFederatedApiKeyQuery, updateOperation).wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return API_KEY_FEDERATED_UPGRADER_ORDER;
    }
}
