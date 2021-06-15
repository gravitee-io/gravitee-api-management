/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.promotion;

import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionRequestEntity;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PromotionService {
    /**
     * List all available target environments of an organization, to request a promotion. As the source environment of the promotion could
     * not be a target, it will not be returned as an available environment.
     * @param organizationId the ID of the organization the targets must belong to
     * @param environmentId the ID of the current environment which will be the source of the promotion
     * @return the list of available target environments
     */
    List<PromotionTargetEntity> listPromotionTargets(String organizationId, String environmentId);

    PromotionEntity promote(String api, PromotionRequestEntity promotionRequest);
}
