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
package io.gravitee.apim.infra.domain_service.api_key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import inmemory.AuditCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.domain_service.audit.AuditDomainServiceImpl;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeApiKeyDomainServiceImplTest {

    @Mock
    ApiKeyRepository apiKeyRepository;

    AuditCrudServiceInMemory auditCrudService;
    UserCrudServiceInMemory userCrudService;

    RevokeApiKeyDomainService service;

    @BeforeEach
    void setup() {
        UuidString.overrideGenerator(() -> "audit-id");

        GraviteeContext.setCurrentOrganization("organization-id");
        GraviteeContext.setCurrentEnvironment("environment-id");

        auditCrudService = new AuditCrudServiceInMemory();
        userCrudService = new UserCrudServiceInMemory();
        service =
            new RevokeApiKeyDomainServiceImpl(
                apiKeyRepository,
                new AuditDomainServiceImpl(auditCrudService, userCrudService, GraviteeJacksonMapper.getInstance())
            );
    }

    @SneakyThrows
    @Test
    void should_do_nothing_when_no_api_keys_for_subscription() {
        // Given no API key for subscription
        givenNoApiKeys();

        // When
        service.revokeAllSubscriptionsApiKeys(
            GraviteeContext.getExecutionContext(),
            "api-id",
            "subscription-id",
            AuditActor.builder().userId("user-id").build()
        );

        // Then
        // No modification
        verify(apiKeyRepository, never()).update(any());
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_do_nothing_when_no_active_api_keys_for_subscription() {
        // Given no API key for subscription
        givenApiKeys(
            "subscription-id",
            Set.of(
                ApiKey.builder().subscriptions(List.of("subscription-id")).id("revoked-api-key").revoked(true).build(),
                ApiKey
                    .builder()
                    .subscriptions(List.of("subscription-id"))
                    .id("expired-api-key")
                    .expireAt(new Date(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()))
                    .build()
            )
        );

        // When
        service.revokeAllSubscriptionsApiKeys(
            GraviteeContext.getExecutionContext(),
            "api-id",
            "subscription-id",
            AuditActor.builder().userId("user-id").build()
        );

        // Then
        // No modification
        verify(apiKeyRepository, never()).update(any());
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_revoke_active_api_keys_for_subscription() {
        Date now = new Date();
        // Given no API key for subscription
        givenApiKeys(
            "subscription-id",
            Set.of(
                ApiKey
                    .builder()
                    .subscriptions(List.of("subscription-id"))
                    .id("api-key")
                    .revoked(false)
                    .expireAt(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                    .key("key")
                    .application("application-id")
                    .build()
            )
        );

        // When
        var revokedKeys = service.revokeAllSubscriptionsApiKeys(
            GraviteeContext.getExecutionContext(),
            "api-id",
            "subscription-id",
            AuditActor.builder().userId("user-id").build()
        );

        // Then
        assertThat(revokedKeys.size()).isOne();
        var revokedKey = revokedKeys.stream().findFirst().get();
        assertThat(revokedKey.isRevoked()).isTrue();
        assertThat(revokedKey.getRevokedAt()).isAfterOrEqualTo(now);

        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
            .containsExactly(
                AuditEntity
                    .builder()
                    .id("audit-id")
                    .organizationId("organization-id")
                    .environmentId("environment-id")
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId("api-id")
                    .user("user-id")
                    .properties(Map.of("APPLICATION", "application-id", "API", "api-id", "API_KEY", "key"))
                    .event(ApiKeyAuditEvent.APIKEY_REVOKED.name())
                    .build()
            );
    }

    @SneakyThrows
    private void givenApiKeys(String subscriptionId, Set<ApiKey> keys) {
        lenient().when(apiKeyRepository.findBySubscription(any())).thenReturn(Set.of());
        lenient().when(apiKeyRepository.findBySubscription(subscriptionId)).thenReturn(keys);

        lenient().when(apiKeyRepository.update(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @SneakyThrows
    private void givenNoApiKeys() {
        lenient().when(apiKeyRepository.findBySubscription(any())).thenReturn(Set.of());
    }
}
