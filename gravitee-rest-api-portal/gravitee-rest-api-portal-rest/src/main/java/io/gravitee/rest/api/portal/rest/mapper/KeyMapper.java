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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class KeyMapper {

    @Autowired
    PlanService planService;

    protected static final Logger LOGGER = LoggerFactory.getLogger(KeyMapper.class);

    public Key convert(ApiKeyEntity apiKeyEntity) {
        final Key keyItem = new Key();
        final String plan = apiKeyEntity.getPlan();

        try {
            PlanEntity planEntity = planService.findById(plan);
            keyItem.setApi(planEntity.getApi());
        } catch (PlanNotFoundException e) {
            LOGGER.warn("plan does not exist : {}", plan);
        }

        keyItem.setApplication(apiKeyEntity.getApplication());
        keyItem.setCreatedAt(apiKeyEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        keyItem.setId(apiKeyEntity.getKey());
        keyItem.setPaused(apiKeyEntity.isPaused());
        keyItem.setPlan(plan);
        keyItem.setRevoked(apiKeyEntity.isRevoked());
        if (apiKeyEntity.isRevoked()) {
            keyItem.setRevokedAt(apiKeyEntity.getRevokedAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        keyItem.setExpired(apiKeyEntity.isExpired());
        if (apiKeyEntity.getExpireAt() != null) {
            keyItem.setExpireAt(apiKeyEntity.getExpireAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        return keyItem;
    }

}
