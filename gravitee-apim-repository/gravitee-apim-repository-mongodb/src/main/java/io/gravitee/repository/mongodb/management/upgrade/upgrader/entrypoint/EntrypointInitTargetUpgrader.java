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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.entrypoint;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.subscription.SubscriptionApplicationNameUpgrader.SUBSCRIPTION_APPLICATION_NAME_UPGRADER_ORDER;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Adds default target value to existing entrypoint.
 * @author GraviteeSource Team
 */
@Component
public class EntrypointInitTargetUpgrader extends MongoUpgrader {

    public static final int ENTRYPOINT_INIT_TARGET_UPGRADER_ORDER = SUBSCRIPTION_APPLICATION_NAME_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var entrypointTargetNullQuery = new Document("target", null);

        this.getCollection("entrypoints")
            .find(entrypointTargetNullQuery)
            .forEach(entrypoint -> {
                entrypoint.append("target", "HTTP");
                this.getCollection("entrypoints").replaceOne(Filters.eq("_id", entrypoint.getString("_id")), entrypoint);
            });
        return true;
    }

    @Override
    public int getOrder() {
        return ENTRYPOINT_INIT_TARGET_UPGRADER_ORDER;
    }
}
