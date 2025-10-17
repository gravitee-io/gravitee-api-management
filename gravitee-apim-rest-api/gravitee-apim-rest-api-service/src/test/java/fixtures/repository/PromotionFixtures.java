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
package fixtures.repository;

import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.management.model.PromotionAuthor;
import io.gravitee.repository.management.model.PromotionStatus;

public class PromotionFixtures {

    private PromotionFixtures() {}

    public static Promotion aPromotion() {
        var author = new PromotionAuthor();
        author.setDisplayName("John Smith");
        author.setUserId("user-id");
        author.setSource("source");
        author.setSourceId("source-id");
        author.setEmail("foo@example.com");

        var promotion = new Promotion();
        promotion.setId("promotion-id");
        promotion.setApiDefinition("api-definition");
        promotion.setApiId("api-id");
        promotion.setStatus(PromotionStatus.CREATED);
        promotion.setTargetEnvCockpitId("target-env-cockpit-id");
        promotion.setTargetEnvName("target-env-name");
        promotion.setSourceEnvCockpitId("source-env-cockpit-id");
        promotion.setSourceEnvName("source-env-name");
        promotion.setTargetApiId("target-api-id");
        promotion.setAuthor(author);

        return promotion;
    }
}
