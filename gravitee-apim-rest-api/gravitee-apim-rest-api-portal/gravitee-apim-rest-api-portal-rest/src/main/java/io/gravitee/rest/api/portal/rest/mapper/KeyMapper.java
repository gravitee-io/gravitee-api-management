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
import io.gravitee.rest.api.portal.rest.model.Key;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class KeyMapper {

    public Key convert(ApiKeyEntity apiKeyEntity) {
        final Key keyItem = new Key();
        keyItem.setId(apiKeyEntity.getId());
        keyItem.setApi(apiKeyEntity.getApi());
        keyItem.setApplication(apiKeyEntity.getApplication());
        keyItem.setCreatedAt(apiKeyEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        keyItem.setKey(apiKeyEntity.getKey());
        keyItem.setPaused(apiKeyEntity.isPaused());
        keyItem.setPlan(apiKeyEntity.getPlan());
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
