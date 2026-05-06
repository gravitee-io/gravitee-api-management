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
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.List;
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
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            log.debug("Starting plan reference type upgrader");

            var filter = Filters.and(
                Filters.exists("api", true),
                Filters.ne("api", null),
                Filters.ne("api", ""),
                Filters.or(Filters.exists("referenceType", false), Filters.eq("referenceType", null))
            );

            var pipeline = List.of(new Document("$set", new Document("referenceType", "API").append("referenceId", "$api")));

            var result = this.getCollection("plans").updateMany(filter, pipeline);
            log.debug("Upgraded {} plan(s) with reference type and reference id", result.getModifiedCount());
            return result.wasAcknowledged();
        });
    }

    @Override
    public int getOrder() {
        return PLAN_REFERENCE_TYPE_UPGRADER_ORDER;
    }
}
