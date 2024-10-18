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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.sharedpolicygroups;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.keys.ApiKeyFederatedUpgrader;
import java.util.ArrayList;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Update the phase of shared policy groups from MESSAGE_REQUEST to PUBLISH and MESSAGE_RESPONSE to SUBSCRIBE
 */
@Component
public class SharedPolicyGroupPhaseUpgrader extends MongoUpgrader {

    public static final int SHARED_POLICY_GROUP_PHASE_UPGRADER_ORDER = ApiKeyFederatedUpgrader.API_KEY_FEDERATED_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var projection = Projections.fields(Projections.include("_id", "phase"));
        var bulkActions = new ArrayList<UpdateManyModel<Document>>();
        this.getCollection("sharedpolicygroups")
            .find()
            .projection(projection)
            .forEach(spg -> {
                if (spg.getString("phase").equals("MESSAGE_REQUEST")) {
                    bulkActions.add(new UpdateManyModel<>(Filters.eq("_id", spg.getString("_id")), Updates.set("phase", "PUBLISH")));
                }
                if (spg.getString("phase").equals("MESSAGE_RESPONSE")) {
                    bulkActions.add(new UpdateManyModel<>(Filters.eq("_id", spg.getString("_id")), Updates.set("phase", "SUBSCRIBE")));
                }
            });
        if (!bulkActions.isEmpty()) {
            this.getCollection("sharedpolicygroups").bulkWrite(bulkActions).wasAcknowledged();
        }

        return true;
    }

    @Override
    public int getOrder() {
        return SHARED_POLICY_GROUP_PHASE_UPGRADER_ORDER;
    }
}
