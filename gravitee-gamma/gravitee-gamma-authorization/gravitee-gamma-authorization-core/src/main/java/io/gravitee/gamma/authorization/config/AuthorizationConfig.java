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
package io.gravitee.gamma.authorization.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.common.event.EventManager;
import io.gravitee.gamma.authorization.api.AuthzAuditPort;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.EntityAdminApi;
import io.gravitee.gamma.authorization.api.EntityRepository;
import io.gravitee.gamma.authorization.api.PolicyAdminApi;
import io.gravitee.gamma.authorization.api.PolicyRepository;
import io.gravitee.gamma.authorization.api.SchemaAdminApi;
import io.gravitee.gamma.authorization.audit.ApimAuthzAuditAdapter;
import io.gravitee.gamma.authorization.event.EventRepositoryAuthzEventPublisher;
import io.gravitee.gamma.authorization.listener.AuthzApiEventListener;
import io.gravitee.gamma.authorization.listener.AuthzEntityIdExtractor;
import io.gravitee.gamma.authorization.service.EntityIdValidator;
import io.gravitee.gamma.authorization.service.EntityServiceImpl;
import io.gravitee.gamma.authorization.service.PolicyServiceImpl;
import io.gravitee.gamma.authorization.service.SchemaServiceImpl;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ComponentScan(
    basePackages = { "io.gravitee.gamma.authorization.infra", "io.gravitee.gamma.repository.mongodb" }
)
@Conditional(GammaEnabledCondition.class)
public class AuthorizationConfig {

    @Bean
    public AuthzEntityIdExtractor authzEntityIdExtractor() {
        return new AuthzEntityIdExtractor();
    }

    @Bean
    public AuthzApiEventListener authzApiEventListener(
        EventManager eventManager,
        EntityAdminApi entityService,
        AuthzEntityIdExtractor authzEntityIdExtractor
    ) {
        return new AuthzApiEventListener(eventManager, entityService, authzEntityIdExtractor);
    }

    @Bean
    public AuthzEventPublisher authzEventPublisher(
        @Lazy EventRepository eventRepository,
        @Lazy EventLatestRepository eventLatestRepository,
        ObjectMapper objectMapper
    ) {
        return new EventRepositoryAuthzEventPublisher(eventRepository, eventLatestRepository, objectMapper);
    }

    @Bean
    public EntityIdValidator entityIdValidator() {
        return new EntityIdValidator();
    }

    @Bean
    public AuthzAuditPort authzAuditPort(AuditDomainService auditDomainService) {
        return new ApimAuthzAuditAdapter(auditDomainService);
    }

    @Bean
    public PolicyAdminApi policyService(
        @Lazy PolicyRepository policyRepository,
        EntityIdValidator entityIdValidator,
        SchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        return new PolicyServiceImpl(policyRepository, entityIdValidator, schemaService, eventPublisher, auditPort);
    }

    @Bean
    public SchemaAdminApi schemaService(@Lazy EntityRepository entityRepository, @Lazy PolicyRepository policyRepository) {
        return new SchemaServiceImpl(entityRepository, policyRepository);
    }

    @Bean
    public EntityAdminApi entityService(
        @Lazy EntityRepository entityRepository,
        @Lazy PolicyRepository policyRepository,
        EntityIdValidator entityIdValidator,
        SchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort,
        @Value("${gravitee.authz.cascade-hard-limit:500}") int cascadeHardLimit
    ) {
        return new EntityServiceImpl(
            entityRepository,
            policyRepository,
            entityIdValidator,
            schemaService,
            eventPublisher,
            auditPort,
            cascadeHardLimit
        );
    }
}
