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
package fixtures.core.model;

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;

public class ApiKeyFixtures {

    private ApiKeyFixtures() {}

    private static final Supplier<ApiKeyEntity.ApiKeyEntityBuilder> BASE = () ->
        ApiKeyEntity.builder()
            .id("api-key-id")
            .key("c080f684-2c35-40a1-903c-627c219e0567")
            .applicationId("application-id")
            .subscriptions(List.of("subscription-id"))
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .expireAt(Instant.parse("2051-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .revoked(false)
            .paused(false)
            .daysToExpirationOnLastNotification(310);

    public static ApiKeyEntity anApiKey() {
        return BASE.get().build();
    }
}
