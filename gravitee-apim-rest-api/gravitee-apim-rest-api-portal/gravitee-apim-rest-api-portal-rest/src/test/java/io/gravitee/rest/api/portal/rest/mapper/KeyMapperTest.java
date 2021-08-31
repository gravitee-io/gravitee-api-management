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

import static org.junit.Assert.*;

import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.portal.rest.model.Key;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyMapperTest {

    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";
    private static final String SUBSCRIPTION = "my-subscription";
    private static final String KEY = "my-key";
    private static final String ID = "my-ID";

    private final Instant now = Instant.now();
    private final Date nowDate = Date.from(now);

    private KeyMapper keyMapper = new KeyMapper();

    @Test
    public void should_convert() {
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setApplication(APPLICATION);
        apiKeyEntity.setCreatedAt(nowDate);
        apiKeyEntity.setExpireAt(nowDate);
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setPaused(false);
        apiKeyEntity.setPlan(PLAN);
        apiKeyEntity.setRevoked(false);
        apiKeyEntity.setRevokedAt(nowDate);
        apiKeyEntity.setSubscription(SUBSCRIPTION);
        apiKeyEntity.setUpdatedAt(nowDate);
        apiKeyEntity.setApi(API);
        apiKeyEntity.setId(ID);

        Key key = keyMapper.convert(apiKeyEntity);

        assertNotNull(key);
        assertEquals(API, key.getApi());
        assertEquals(APPLICATION, key.getApplication());
        assertEquals(now.toEpochMilli(), key.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(KEY, key.getKey());
        assertEquals(ID, key.getId());
        assertEquals(Boolean.FALSE, key.getPaused());
        assertEquals(PLAN, key.getPlan());
        assertEquals(Boolean.FALSE, key.getRevoked());
        assertNull(key.getRevokedAt());
    }
}
