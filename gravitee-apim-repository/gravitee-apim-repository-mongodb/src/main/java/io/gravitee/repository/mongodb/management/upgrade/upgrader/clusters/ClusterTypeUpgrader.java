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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.clusters;

import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.sharedpolicygroups.SharedPolicyGroupHistoriesPhaseUpgrader;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class ClusterTypeUpgrader extends MongoUpgrader {

    public static final int CLUSTER_TYPE_UPGRADER_ORDER =
        SharedPolicyGroupHistoriesPhaseUpgrader.SHARED_POLICY_GROUP_HISTORIES_PHASE_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        this.getCollection("clusters").updateMany(
            new Document("type", new Document("$exists", false)),
            Updates.set("type", "KAFKA_CLUSTER_CONNECTION")
        );
        return true;
    }

    @Override
    public int getOrder() {
        return CLUSTER_TYPE_UPGRADER_ORDER;
    }
}
