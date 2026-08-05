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
import org.springframework.stereotype.Component;

/**
 * Backfills the {@code referenceType} / {@code referenceId} pair on portal navigation items that
 * were persisted before this schema existed, so that Spring Data MongoDB can deserialize them
 * into the now-{@code @Nonnull} fields.
 *
 * Follow-up (multi-portal): items carrying the UNATTACHED sentinel are meant to be reassigned to
 * the first real Portal created in the environment.
 */
@Component
public class PortalNavigationItemDefaultReferenceMongoUpgrader extends MongoUpgrader {

    public static final int PORTAL_NAVIGATION_ITEM_DEFAULT_REFERENCE_MONGO_UPGRADER_ORDER =
        PortalNavigationItemDefaultRootIdMongoUpgrader.PORTAL_NAVIGATION_ITEM_DEFAULT_ROOT_ID_MONGO_UPGRADER_ORDER + 1;

    public static final String REFERENCE_TYPE = "referenceType";
    public static final String REFERENCE_ID = "referenceId";
    public static final String UNATTACHED_REFERENCE_TYPE = "PORTAL";
    public static final String UNATTACHED_REFERENCE_ID = "00000000-0000-0000-0000-000000000000";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var collection = this.getCollection("portal_navigation_items");
        var typeResult = collection.updateMany(
            Filters.or(Filters.exists(REFERENCE_TYPE, false), Filters.eq(REFERENCE_TYPE, null)),
            Updates.set(REFERENCE_TYPE, UNATTACHED_REFERENCE_TYPE)
        );
        var idResult = collection.updateMany(
            Filters.or(Filters.exists(REFERENCE_ID, false), Filters.eq(REFERENCE_ID, null), Filters.eq(REFERENCE_ID, "")),
            Updates.set(REFERENCE_ID, UNATTACHED_REFERENCE_ID)
        );
        return typeResult.wasAcknowledged() && idResult.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_ITEM_DEFAULT_REFERENCE_MONGO_UPGRADER_ORDER;
    }
}
