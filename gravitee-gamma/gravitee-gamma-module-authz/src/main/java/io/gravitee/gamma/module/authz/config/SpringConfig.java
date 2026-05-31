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
package io.gravitee.gamma.module.authz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.apim.plugin.gamma.api.identity.ApimAmConnectionRepository;
import io.gravitee.gamma.authorization.api.AuthzAuditPort;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.AuthzPolicyAdminApi;
import io.gravitee.gamma.authorization.api.AuthzPolicyRepository;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.audit.ApimAuthzAuditAdapter;
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserClient;
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserSyncRunner;
import io.gravitee.gamma.authorization.core.am.use_case.GetAmUserSyncStatusUseCase;
import io.gravitee.gamma.authorization.core.am.use_case.StartAmUserSyncUseCase;
import io.gravitee.gamma.authorization.core.am.use_case.SyncAmUsersUseCase;
import io.gravitee.gamma.authorization.event.EventRepositoryAuthzEventPublisher;
import io.gravitee.gamma.authorization.infra.repository.MongoAuthzEntityRepository;
import io.gravitee.gamma.authorization.infra.repository.MongoAuthzPolicyRepository;
import io.gravitee.gamma.authorization.infra.service_provider.AmSdkUserClient;
import io.gravitee.gamma.authorization.infra.service_provider.AmSdkUserClientFactory;
import io.gravitee.gamma.authorization.infra.service_provider.AmUserSyncRunnerImpl;
import io.gravitee.gamma.authorization.service.AuthzEntityIdValidator;
import io.gravitee.gamma.authorization.service.AuthzEntityServiceImpl;
import io.gravitee.gamma.authorization.service.AuthzPolicyServiceImpl;
import io.gravitee.gamma.authorization.service.AuthzSchemaServiceImpl;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.rest.api.service.AmConnectionService;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * Spring configuration for the authz Gamma plugin.
 *
 * <p>This class lives inside the plugin jar (not in gamma-authorization-core) so that
 * the JVM resolves @Bean method parameter types — most notably {@code MongoOperations}
 * from spring-data-mongodb, which is on the apim-mongo plugin's classpath — through
 * <em>this</em> class's defining ClassLoader (the plugin's URLClassLoader). That
 * URLClassLoader's parent chain sees apim-mongo's classes, whereas gamma-authorization-core's
 * defining ClassLoader (graviteeClassLoader / lib/) does not.
 *
 * <p>Mirrors the AIM module's CatalogConfiguration pattern.
 */
@Configuration
public class SpringConfig {

    @Bean
    public AuthzEntityRepository authzEntityRepository(@Qualifier("managementMongoTemplate") MongoOperations mongoOperations) {
        return new MongoAuthzEntityRepository(mongoOperations);
    }

    @Bean
    public AuthzPolicyRepository authzPolicyRepository(@Qualifier("managementMongoTemplate") MongoOperations mongoOperations) {
        return new MongoAuthzPolicyRepository(mongoOperations);
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

    // @Qualifier is required on @Lazy params for repositories/services because the plugin
    // handler propagates plugin-ctx beans into the parent rest-api ctx with an "authz." prefix
    // (see GammaModulePluginHandler.initPluginSpringContext). At @Lazy proxy resolution time
    // the plugin ctx sees both its own bean and the propagated parent copy — autowire-by-type
    // would then fail with NoUniqueBeanDefinitionException. Pinning by name picks the local one.

    @Bean
    public AuthzPolicyAdminApi policyService(
        @Lazy @Qualifier("authzPolicyRepository") AuthzPolicyRepository policyRepository,
        AuthzEntityIdValidator entityIdValidator,
        AuthzSchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        return new AuthzPolicyServiceImpl(policyRepository, entityIdValidator, schemaService, eventPublisher, auditPort);
    }

    @Bean
    public AuthzSchemaAdminApi schemaService(
        @Lazy @Qualifier("authzEntityRepository") AuthzEntityRepository entityRepository,
        @Lazy @Qualifier("authzPolicyRepository") AuthzPolicyRepository policyRepository
    ) {
        return new AuthzSchemaServiceImpl(entityRepository, policyRepository);
    }

    @Bean
    public AuthzEntityAdminApi entityService(
        @Lazy @Qualifier("authzEntityRepository") AuthzEntityRepository entityRepository,
        @Lazy @Qualifier("authzPolicyRepository") AuthzPolicyRepository policyRepository,
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

    @Bean
    public AmConnectionRepository amConnectionRepository(@Lazy AmConnectionService amConnectionService) {
        return new ApimAmConnectionRepository(amConnectionService);
    }

    @Bean
    public AmSdkUserClientFactory amSdkUserClientFactory(Vertx vertx) {
        return new AmSdkUserClientFactory(vertx);
    }

    @Bean
    public AmUserClient amUserClient(AmSdkUserClientFactory amSdkUserClientFactory) {
        return new AmSdkUserClient(amSdkUserClientFactory);
    }

    @Bean
    public SyncAmUsersUseCase syncAmUsersUseCase(
        AmUserClient amUserClient,
        @Lazy @Qualifier("entityService") AuthzEntityAdminApi entityService
    ) {
        return new SyncAmUsersUseCase(amUserClient, entityService);
    }

    @Bean
    public AmUserSyncRunner amUserSyncRunner(SyncAmUsersUseCase syncAmUsersUseCase, AsyncJobCrudService asyncJobCrudService) {
        return new AmUserSyncRunnerImpl(syncAmUsersUseCase, asyncJobCrudService);
    }

    @Bean
    public StartAmUserSyncUseCase startAmUserSyncUseCase(
        AsyncJobQueryService asyncJobQueryService,
        AsyncJobCrudService asyncJobCrudService,
        @Lazy @Qualifier("amConnectionRepository") AmConnectionRepository amConnectionRepository,
        AmUserClient amUserClient,
        AmUserSyncRunner amUserSyncRunner
    ) {
        return new StartAmUserSyncUseCase(asyncJobQueryService, asyncJobCrudService, amConnectionRepository, amUserClient, amUserSyncRunner);
    }

    @Bean
    public GetAmUserSyncStatusUseCase getAmUserSyncStatusUseCase(AsyncJobQueryService asyncJobQueryService) {
        return new GetAmUserSyncStatusUseCase(asyncJobQueryService);
    }
}
