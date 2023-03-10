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
package io.gravitee.gateway.services.sync.process.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiKeyMapperTest {

    private ApiKeyMapper cut;
    private ApiKey apiKey;
    private Subscription subscription;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiKeyMapper();

        apiKey = new ApiKey();
        apiKey.setId("id");
        apiKey.setKey("key");
        apiKey.setApplication("application");
        apiKey.setCreatedAt(new Date());
        apiKey.setExpireAt(new Date());
        apiKey.setUpdatedAt(new Date());
        apiKey.setRevokedAt(new Date());
        apiKey.setRevoked(false);
        apiKey.setPaused(false);
        apiKey.setDaysToExpirationOnLastNotification(123);

        subscription = new Subscription();
        subscription.setId("sub");
        subscription.setApi("api");
        subscription.setPlan("plan");
        subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.ACCEPTED.name());
    }

    @Test
    void should_map_apikey() {
        io.gravitee.gateway.api.service.ApiKey gatewayApiKey = cut.to(apiKey, subscription);

        assertThat(gatewayApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(gatewayApiKey.getKey()).isEqualTo(apiKey.getKey());
        assertThat(gatewayApiKey.getExpireAt()).isEqualTo(apiKey.getExpireAt());
        assertThat(gatewayApiKey.isActive()).isTrue();
        assertThat(gatewayApiKey.getApi()).isEqualTo(subscription.getApi());
        assertThat(gatewayApiKey.getPlan()).isEqualTo(subscription.getPlan());
        assertThat(gatewayApiKey.getSubscription()).isEqualTo(subscription.getId());
    }

    @Test
    void should_map_inactive_apikey_when_api_key_is_paused() {
        apiKey.setPaused(true);
        io.gravitee.gateway.api.service.ApiKey gatewayApiKey = cut.to(apiKey, subscription);

        assertThat(gatewayApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(gatewayApiKey.getKey()).isEqualTo(apiKey.getKey());
        assertThat(gatewayApiKey.getExpireAt()).isEqualTo(apiKey.getExpireAt());
        assertThat(gatewayApiKey.isActive()).isFalse();
        assertThat(gatewayApiKey.getApi()).isEqualTo(subscription.getApi());
        assertThat(gatewayApiKey.getPlan()).isEqualTo(subscription.getPlan());
        assertThat(gatewayApiKey.getSubscription()).isEqualTo(subscription.getId());
    }

    @Test
    void should_map_inactive_apikey_when_api_key_is_revoked() {
        apiKey.setRevoked(true);
        io.gravitee.gateway.api.service.ApiKey gatewayApiKey = cut.to(apiKey, subscription);

        assertThat(gatewayApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(gatewayApiKey.getKey()).isEqualTo(apiKey.getKey());
        assertThat(gatewayApiKey.getExpireAt()).isEqualTo(apiKey.getExpireAt());
        assertThat(gatewayApiKey.isActive()).isFalse();
        assertThat(gatewayApiKey.getApi()).isEqualTo(subscription.getApi());
        assertThat(gatewayApiKey.getPlan()).isEqualTo(subscription.getPlan());
        assertThat(gatewayApiKey.getSubscription()).isEqualTo(subscription.getId());
    }

    @Test
    void should_map_inactive_apikey_when_subscription_is_not_accepted() {
        subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.CLOSED.name());
        io.gravitee.gateway.api.service.ApiKey gatewayApiKey = cut.to(apiKey, subscription);

        assertThat(gatewayApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(gatewayApiKey.getKey()).isEqualTo(apiKey.getKey());
        assertThat(gatewayApiKey.getExpireAt()).isEqualTo(apiKey.getExpireAt());
        assertThat(gatewayApiKey.isActive()).isFalse();
        assertThat(gatewayApiKey.getApi()).isEqualTo(subscription.getApi());
        assertThat(gatewayApiKey.getPlan()).isEqualTo(subscription.getPlan());
        assertThat(gatewayApiKey.getSubscription()).isEqualTo(subscription.getId());
    }

    @Test
    void should_map_inactive_apikey_without_subscription() {
        io.gravitee.gateway.api.service.ApiKey gatewayApiKey = cut.to(apiKey, (Subscription) null);

        assertThat(gatewayApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(gatewayApiKey.getKey()).isEqualTo(apiKey.getKey());
        assertThat(gatewayApiKey.getExpireAt()).isEqualTo(apiKey.getExpireAt());
        assertThat(gatewayApiKey.isActive()).isFalse();
    }

    @Test
    void should_map_inactive_apikey_with_empty_subscription() {
        io.gravitee.gateway.api.service.ApiKey gatewayApiKey = cut.to(apiKey, Optional.empty());

        assertThat(gatewayApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(gatewayApiKey.getKey()).isEqualTo(apiKey.getKey());
        assertThat(gatewayApiKey.getExpireAt()).isEqualTo(apiKey.getExpireAt());
        assertThat(gatewayApiKey.isActive()).isFalse();
    }
}
