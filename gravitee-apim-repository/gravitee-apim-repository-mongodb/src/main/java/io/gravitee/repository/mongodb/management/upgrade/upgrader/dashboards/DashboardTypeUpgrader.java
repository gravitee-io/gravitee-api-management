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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.dashboards;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.promotions.RemovePromotionAuthorPictureUpgrader.REMOVE_PROMOTION_AUTHOR_PICTURE_UPGRADER_ORDER;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Rename 'referenceType' to 'type' and set 'referenceType' to 'ENVIRONMENT'
 * This allows to distinguish the type of dashboard from the reference it belongs to.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DashboardTypeUpgrader extends MongoUpgrader {

    @Override
    public String version() {
        return "v1";
    }

    public static final int DASHBOARD_TYPE_UPGRADER_ORDER = REMOVE_PROMOTION_AUTHOR_PICTURE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var dashboardsTypeNotExistsQuery = new Document("type", new Document("$exists", false));

        // Because of DocumentDB which does not support latest aggregation APIs of MongoDB, we need to use this kind of update method.
        this.getCollection("dashboards")
            .find(dashboardsTypeNotExistsQuery)
            .forEach(dashboard -> {
                dashboard.append("type", dashboard.getString("referenceType"));
                dashboard.put("referenceType", "ENVIRONMENT");
                this.getCollection("dashboards").replaceOne(Filters.eq("_id", dashboard.getString("_id")), dashboard);
            });
        return true;
    }

    @Override
    public int getOrder() {
        return DASHBOARD_TYPE_UPGRADER_ORDER;
    }
}
