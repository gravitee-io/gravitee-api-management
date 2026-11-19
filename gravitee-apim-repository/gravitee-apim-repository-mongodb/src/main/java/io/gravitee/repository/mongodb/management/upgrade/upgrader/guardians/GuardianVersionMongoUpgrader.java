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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.guardians;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormGlobalDefaultMongoUpgrader;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Gives version 1 to the Guardians stored before the draft version existed.
 *
 * <p>A Guardian is created at version 1 and an update has to state the version it was built on. One with no
 * version would carry no {@code ETag}, so its first save could only be an unguarded overwrite: two editors
 * would still undo each other until someone saved it once. The JDBC changelog does the same for its rows.
 *
 * @author GraviteeSource Team
 */
@Component
public class GuardianVersionMongoUpgrader extends MongoUpgrader {

    public static final int GUARDIAN_VERSION_MONGO_UPGRADER_ORDER =
        SubscriptionFormGlobalDefaultMongoUpgrader.SUBSCRIPTION_FORM_GLOBAL_DEFAULT_MONGO_UPGRADER_ORDER + 1;

    static final String VERSION = "version";
    static final int FIRST_VERSION = 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        // The version carries no index, and the databases the tests run against forbid collection scans:
        // walking the _id index visits every Guardian without asking for one.
        var result = this.getCollection("guardians").updateMany(
            Filters.eq(VERSION, null),
            Updates.set(VERSION, FIRST_VERSION),
            new UpdateOptions().hint(new Document("_id", 1))
        );
        return result.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return GUARDIAN_VERSION_MONGO_UPGRADER_ORDER;
    }
}
