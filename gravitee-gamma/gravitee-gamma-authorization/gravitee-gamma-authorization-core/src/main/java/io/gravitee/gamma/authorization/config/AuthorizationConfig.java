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
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.AuthzPolicyAdminApi;
import io.gravitee.gamma.authorization.api.AuthzPolicyRepository;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.audit.ApimAuthzAuditAdapter;
import io.gravitee.gamma.authorization.event.EventRepositoryAuthzEventPublisher;
import io.gravitee.gamma.authorization.infra.repository.MongoAuthzEntityRepository;
import io.gravitee.gamma.authorization.infra.repository.MongoAuthzPolicyRepository;
import io.gravitee.gamma.authorization.listener.ApiEventListener;
import io.gravitee.gamma.authorization.listener.EntityIdExtractor;
import io.gravitee.gamma.authorization.service.AuthzEntityIdValidator;
import io.gravitee.gamma.authorization.service.AuthzEntityServiceImpl;
import io.gravitee.gamma.authorization.service.AuthzPolicyServiceImpl;
import io.gravitee.gamma.authorization.service.AuthzSchemaServiceImpl;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoOperations;

@Configuration
public class AuthorizationConfig {

    @Bean
    @Lazy
    public AuthzEntityRepository authzEntityRepository(@Lazy @Qualifier("managementMongoTemplate") MongoOperations mongoOperations) {
        return new MongoAuthzEntityRepository(mongoOperations);
    }

    @Bean
    @Lazy
    public AuthzPolicyRepository authzPolicyRepository(@Lazy @Qualifier("managementMongoTemplate") MongoOperations mongoOperations) {
        return new MongoAuthzPolicyRepository(mongoOperations);
    }

    @Bean
    public EntityIdExtractor authzEntityIdExtractor() {
        return new EntityIdExtractor();
    }

    @Bean
    public ApiEventListener authzApiEventListener(
        EventManager eventManager,
        AuthzEntityAdminApi entityService,
        EntityIdExtractor entityIdExtractor
    ) {
        return new ApiEventListener(eventManager, entityService, entityIdExtractor);
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
    public AuthzEntityIdValidator authzEntityIdValidator() {
        return new AuthzEntityIdValidator();
    }

    @Bean
    public AuthzAuditPort authzAuditPort(AuditDomainService auditDomainService) {
        return new ApimAuthzAuditAdapter(auditDomainService);
    }

    @Bean
    public AuthzPolicyAdminApi policyService(
        @Lazy AuthzPolicyRepository policyRepository,
        AuthzEntityIdValidator entityIdValidator,
        AuthzSchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        return new AuthzPolicyServiceImpl(policyRepository, entityIdValidator, schemaService, eventPublisher, auditPort);
    }

    @Bean
    public AuthzSchemaAdminApi schemaService(@Lazy AuthzEntityRepository entityRepository, @Lazy AuthzPolicyRepository policyRepository) {
        return new AuthzSchemaServiceImpl(entityRepository, policyRepository);
    }

    @Bean
    public AuthzEntityAdminApi entityService(
        @Lazy AuthzEntityRepository entityRepository,
        @Lazy AuthzPolicyRepository policyRepository,
        AuthzEntityIdValidator entityIdValidator,
        AuthzSchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort,
        @Value("${gravitee.authz.cascade-hard-limit:500}") int cascadeHardLimit
    ) {
        return new AuthzEntityServiceImpl(
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
