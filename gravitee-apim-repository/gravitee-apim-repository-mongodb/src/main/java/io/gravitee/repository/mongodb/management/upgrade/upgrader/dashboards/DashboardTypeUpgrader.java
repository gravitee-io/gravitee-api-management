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

import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.List;
import org.bson.BsonDocument;
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
    public boolean upgrade() {
        var updateResult = template
            .getCollection("dashboards")
            .updateMany(new BsonDocument(), List.of(Updates.set("type", "$referenceType"), Updates.set("referenceType", "ENVIRONMENT")));
        return updateResult.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return REMOVE_PROMOTION_AUTHOR_PICTURE_UPGRADER_ORDER + 1;
    }
}
