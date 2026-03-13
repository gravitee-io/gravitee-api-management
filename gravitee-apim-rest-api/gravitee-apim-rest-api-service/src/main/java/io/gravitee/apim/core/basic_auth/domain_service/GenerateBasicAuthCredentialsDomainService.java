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
package io.gravitee.apim.core.basic_auth.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.BasicAuthCredentialsAuditEvent;
import io.gravitee.apim.core.basic_auth.crud_service.BasicAuthCredentialsCrudService;
import io.gravitee.apim.core.basic_auth.model.BasicAuthCredentialsEntity;
import io.gravitee.apim.core.basic_auth.model.BasicAuthPlainCredentialsHolder;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DomainService
@CustomLog
@RequiredArgsConstructor
public class GenerateBasicAuthCredentialsDomainService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final BasicAuthCredentialsCrudService basicAuthCredentialsCrudService;
    private final AuditDomainService auditService;

    public BasicAuthCredentialsEntity generate(SubscriptionEntity subscription, AuditInfo auditInfo) {
        log.debug("Generate Basic Auth credentials for subscription {}", subscription.getId());

        BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.generateForSubscription(subscription);
        String plainPassword = credentials.getPassword();

        BasicAuthCredentialsEntity hashedCredentials = credentials.toBuilder().password(PASSWORD_ENCODER.encode(plainPassword)).build();
        basicAuthCredentialsCrudService.create(hashedCredentials);

        createAuditLog(hashedCredentials, subscription, auditInfo, BasicAuthCredentialsAuditEvent.BASIC_AUTH_CREDENTIALS_CREATED);

        BasicAuthPlainCredentialsHolder.set(credentials);

        return credentials;
    }

    public BasicAuthCredentialsEntity renew(SubscriptionEntity subscription, AuditInfo auditInfo) {
        log.debug("Renew Basic Auth credentials for subscription {}", subscription.getId());

        basicAuthCredentialsCrudService
            .findBySubscriptionId(subscription.getId())
            .filter(BasicAuthCredentialsEntity::canBeRevoked)
            .ifPresent(existing -> basicAuthCredentialsCrudService.update(existing.revoke()));

        BasicAuthCredentialsEntity credentials = BasicAuthCredentialsEntity.generateForSubscription(subscription);
        String plainPassword = credentials.getPassword();

        BasicAuthCredentialsEntity hashedCredentials = credentials.toBuilder().password(PASSWORD_ENCODER.encode(plainPassword)).build();
        basicAuthCredentialsCrudService.create(hashedCredentials);

        createAuditLog(hashedCredentials, subscription, auditInfo, BasicAuthCredentialsAuditEvent.BASIC_AUTH_CREDENTIALS_RENEWED);

        return credentials;
    }

    private void createAuditLog(
        BasicAuthCredentialsEntity credentials,
        SubscriptionEntity subscription,
        AuditInfo auditInfo,
        BasicAuthCredentialsAuditEvent event
    ) {
        String apiId = subscription.getApiId();

        Map<AuditProperties, String> properties = new HashMap<>();
        properties.put(AuditProperties.APPLICATION, subscription.getApplicationId());
        if (apiId != null) {
            properties.put(AuditProperties.API, apiId);
        }

        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(apiId)
                .event(event)
                .actor(auditInfo.actor())
                .oldValue(null)
                .newValue(credentials)
                .createdAt(credentials.getCreatedAt())
                .properties(properties)
                .build()
        );
    }
}
