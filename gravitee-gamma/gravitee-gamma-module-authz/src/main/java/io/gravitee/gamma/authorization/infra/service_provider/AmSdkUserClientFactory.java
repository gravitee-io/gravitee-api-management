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
package io.gravitee.gamma.authorization.infra.service_provider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.am.sdk.management.api.UserApi;
import io.gravitee.am.sdk.management.api.UserApiImpl;
import io.gravitee.am.sdk.management.invoker.ApiClient;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Builds an AM management {@link UserApi} from a stored {@link AmConnection}. No caching — a sync
 * opens one client per run (see {@code AmSdkUserClient.openSession}) and closes it when done, so an
 * updated connection takes effect on the next sync. Mirrors the AIM module's {@code AmSdkClientFactory}.
 */
public class AmSdkUserClientFactory {

    private final Vertx vertx;

    public AmSdkUserClientFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public UserApi userApi(AmConnection connection) {
        ApiClient apiClient = new ApiClient(vertx, new JsonObject());
        apiClient.setBasePath(connection.baseUrl() + "/management");
        apiClient.setBearerToken(connection.serviceAccountAccessToken());

        // AM serialises timestamps as epoch *milliseconds*, but the SDK's JavaTimeModule parses
        // numeric input as epoch *seconds* — pushing every parsed timestamp into year ~58312.
        SimpleModule millisTimestamps = new SimpleModule();
        millisTimestamps.addDeserializer(
            OffsetDateTime.class,
            new JsonDeserializer<>() {
                @Override
                public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(p.getLongValue()), ZoneOffset.UTC);
                    }
                    String text = p.getValueAsString();
                    return text == null || text.isEmpty() ? null : OffsetDateTime.parse(text);
                }
            }
        );
        apiClient.getObjectMapper().registerModule(millisTimestamps);

        return new UserApiImpl(apiClient);
    }
}
