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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.promotions;

import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import org.bson.BsonDocument;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("RemovePromotionAuthorPictureUpgrader")
public class RemovePromotionAuthorPictureUpgrader extends MongoUpgrader {

    @Override
    public boolean upgrade() {
        var updateResult = template.getCollection("promotions").updateMany(new BsonDocument(), Updates.unset("author.picture"));
        return updateResult.wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
