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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.accessPoints;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import java.util.Date;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class AccessPointsStatusAndUpdatedUpgrader extends MongoUpgrader {

    public static final int ACCESSPOINTS_STATUS_AND_UPDATED_UPGRADER_ORDER = ThemeTypeUpgrader.THEME_TYPE_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var accessPointsStatusNullQuery = new Document("status", null);

        template
            .getCollection("access_points")
            .find(accessPointsStatusNullQuery)
            .forEach(accessPoint -> {
                accessPoint.append("status", "CREATED");
                accessPoint.append("updatedAt", new Date());
                template.getCollection("access_points").replaceOne(Filters.eq("_id", accessPoint.getString("_id")), accessPoint);
            });

        return true;
    }

    @Override
    public int getOrder() {
        return ACCESSPOINTS_STATUS_AND_UPDATED_UPGRADER_ORDER;
    }
}
