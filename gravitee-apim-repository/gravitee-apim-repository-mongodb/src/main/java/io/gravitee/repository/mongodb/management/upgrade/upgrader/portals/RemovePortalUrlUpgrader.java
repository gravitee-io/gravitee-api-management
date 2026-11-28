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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.portals;

import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import org.bson.BsonDocument;
import org.springframework.stereotype.Component;

/**
 * Portals are served at the access point they reference: the URL they stored is not read anymore.
 *
 * @author GraviteeSource Team
 */
@Component("RemovePortalUrlUpgrader")
public class RemovePortalUrlUpgrader extends MongoUpgrader {

    public static final int REMOVE_PORTAL_URL_UPGRADER_ORDER = 0;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var updateResult = this.getCollection("portals").updateMany(new BsonDocument(), Updates.unset("url"));
        return updateResult.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return REMOVE_PORTAL_URL_UPGRADER_ORDER;
    }
}
