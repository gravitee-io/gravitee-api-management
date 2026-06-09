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
package io.gravitee.gamma.module.platform.infra.service_provider;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.am.sdk.management.api.DefaultApi;
import io.gravitee.am.sdk.management.api.DefaultApiImpl;
import io.gravitee.am.sdk.management.api.DomainApi;
import io.gravitee.am.sdk.management.api.DomainApiImpl;
import io.gravitee.am.sdk.management.invoker.ApiClient;
import io.gravitee.am.sdk.management.model.ClientRegistrationSettings;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

// No caching — an updated AM connection saved via the connection resource takes effect on the
// next request without invalidation.
@RequiredArgsConstructor
public class AmSdkClientFactory {

    private final Vertx vertx;
    private final AmConnectionRepository amConnectionRepository;

    public AmApis forOrg(String orgId) {
        return forConnection(amConnectionRepository.requireByOrg(orgId));
    }

    public AmApis forConnection(AmConnection connection) {
        ApiClient apiClient = new ApiClient(vertx, new JsonObject());
        apiClient.setBasePath(connection.baseUrl() + "/management");
        apiClient.setBearerToken(connection.serviceAccountAccessToken());
        // NON_NULL inclusion lets `setX(List.of())` reach AM as `[]` for explicit-clear while
        // omitting untouched fields — the SDK leaves collection fields null until a call site
        // touches them (containerDefaultToNull generator option).
        apiClient.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

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

        // AM serialises these as `isX` but the SDK expects `x`; mixin bridges the gap so the full
        // domain payload (returned by findDomain/listDomains) deserialises cleanly.
        apiClient.getObjectMapper().addMixIn(ClientRegistrationSettings.class, ClientRegistrationSettingsMixin.class);

        return new AmApis(apiClient, new DefaultApiImpl(apiClient), new DomainApiImpl(apiClient));
    }

    @SuppressWarnings("unused")
    private abstract static class ClientRegistrationSettingsMixin {

        @JsonAlias("isDynamicClientRegistrationEnabled")
        abstract void setDynamicClientRegistrationEnabled(Boolean value);

        @JsonAlias("isOpenDynamicClientRegistrationEnabled")
        abstract void setOpenDynamicClientRegistrationEnabled(Boolean value);

        @JsonAlias("isAllowedScopesEnabled")
        abstract void setAllowedScopesEnabled(Boolean value);

        @JsonAlias("isClientTemplateEnabled")
        abstract void setClientTemplateEnabled(Boolean value);
    }

    public record AmApis(ApiClient client, DefaultApi defaults, DomainApi domains) {}
}
