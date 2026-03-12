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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.entrypoint.EntrypointInitTargetUpgrader;
import org.springframework.stereotype.Component;

/**
 * Sets a placeholder {@code rootId} on portal navigation items that have a null or missing value,
 * so that Spring Data MongoDB can deserialize them without a NullPointerException.
 * The subsequent {@code PortalNavigationItemRootIdUpgrader} then replaces this placeholder
 * with the correct ancestor id.
 */
@Component
public class PortalNavigationItemDefaultRootIdMongoUpgrader extends MongoUpgrader {

    public static final int PORTAL_NAVIGATION_ITEM_DEFAULT_ROOT_ID_MONGO_UPGRADER_ORDER =
        EntrypointInitTargetUpgrader.ENTRYPOINT_INIT_TARGET_UPGRADER_ORDER + 1;

    public static final String ROOT_ID = "rootId";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var result = this.getCollection("portal_navigation_items").updateMany(
            Filters.or(Filters.eq(ROOT_ID, null), Filters.eq(ROOT_ID, "")),
            Updates.set(ROOT_ID, "00000000-0000-0000-0000-000000000000")
        );
        return result.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_DEFAULT_ROOT_ID_MONGO_UPGRADER_ORDER;
    }
}
