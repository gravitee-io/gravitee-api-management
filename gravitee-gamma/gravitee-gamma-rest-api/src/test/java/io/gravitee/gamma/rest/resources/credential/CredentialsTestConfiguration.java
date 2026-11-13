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
package io.gravitee.gamma.rest.resources.credential;

import inmemory.AuditCrudServiceInMemory;
import inmemory.CredentialConsumersInMemory;
import inmemory.CredentialCrudServiceInMemory;
import inmemory.CredentialQueryServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretValueEncryptorInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.credential.domain_service.CredentialAuditDomainService;
import io.gravitee.apim.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.apim.core.credential.domain_service.CredentialSecretsDomainService;
import io.gravitee.apim.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.DeleteCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.GetCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.ListCredentialConsumersUseCase;
import io.gravitee.apim.core.credential.use_case.RotateCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.apim.core.credential.use_case.UpdateCredentialUseCase;
import io.gravitee.apim.core.secret.domain_service.SecretDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The credential use cases, run for real against in-memory stores. */
@Configuration
class CredentialsTestConfiguration {

    @Bean
    CredentialCrudServiceInMemory credentialCrudService() {
        return new CredentialCrudServiceInMemory();
    }

    @Bean
    CredentialConsumersInMemory credentialConsumers() {
        return new CredentialConsumersInMemory();
    }

    @Bean
    SecretCrudServiceInMemory secretCrudService() {
        return new SecretCrudServiceInMemory();
    }

    @Bean
    AuditCrudServiceInMemory auditCrudService() {
        return new AuditCrudServiceInMemory();
    }

    @Bean
    UserCrudServiceInMemory userCrudService() {
        return new UserCrudServiceInMemory();
    }

    @Bean
    CredentialAuditDomainService credentialAuditDomainService(AuditDomainService auditDomainService) {
        return new CredentialAuditDomainService(auditDomainService);
    }

    @Bean
    AuditDomainService auditDomainService(AuditCrudServiceInMemory auditCrudService, UserCrudServiceInMemory userCrudService) {
        return new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
    }

    @Bean
    CredentialSecretsDomainService credentialSecretsDomainService(
        SecretCrudServiceInMemory secretCrudService,
        AuditDomainService auditDomainService
    ) {
        return new CredentialSecretsDomainService(
            new SecretDomainService(secretCrudService, new SecretValueEncryptorInMemory(), auditDomainService),
            secretCrudService
        );
    }

    @Bean
    CredentialOwnerDomainService credentialOwnerDomainService(UserCrudServiceInMemory userCrudService) {
        return new CredentialOwnerDomainService(userCrudService);
    }

    @Bean
    CreateCredentialUseCase createCredentialUseCase(
        CredentialCrudServiceInMemory credentialCrudService,
        CredentialSecretsDomainService credentialSecretsDomainService,
        CredentialOwnerDomainService credentialOwnerDomainService,
        CredentialAuditDomainService credentialAuditDomainService
    ) {
        return new CreateCredentialUseCase(
            credentialCrudService,
            credentialSecretsDomainService,
            credentialOwnerDomainService,
            credentialAuditDomainService
        );
    }

    @Bean
    SearchCredentialsUseCase searchCredentialsUseCase(CredentialCrudServiceInMemory credentialCrudService) {
        return new SearchCredentialsUseCase(new CredentialQueryServiceInMemory(credentialCrudService));
    }

    @Bean
    GetCredentialUseCase getCredentialUseCase(CredentialCrudServiceInMemory credentialCrudService) {
        return new GetCredentialUseCase(credentialCrudService);
    }

    @Bean
    UpdateCredentialUseCase updateCredentialUseCase(
        CredentialCrudServiceInMemory credentialCrudService,
        CredentialOwnerDomainService credentialOwnerDomainService,
        CredentialAuditDomainService credentialAuditDomainService
    ) {
        return new UpdateCredentialUseCase(credentialCrudService, credentialOwnerDomainService, credentialAuditDomainService);
    }

    @Bean
    RotateCredentialUseCase rotateCredentialUseCase(
        CredentialCrudServiceInMemory credentialCrudService,
        CredentialSecretsDomainService credentialSecretsDomainService,
        CredentialAuditDomainService credentialAuditDomainService
    ) {
        return new RotateCredentialUseCase(credentialCrudService, credentialSecretsDomainService, credentialAuditDomainService);
    }

    @Bean
    DeleteCredentialUseCase deleteCredentialUseCase(
        CredentialCrudServiceInMemory credentialCrudService,
        CredentialSecretsDomainService credentialSecretsDomainService,
        CredentialConsumersInMemory credentialConsumers,
        CredentialAuditDomainService credentialAuditDomainService
    ) {
        return new DeleteCredentialUseCase(
            credentialCrudService,
            credentialSecretsDomainService,
            credentialConsumers,
            credentialAuditDomainService
        );
    }

    @Bean
    ListCredentialConsumersUseCase listCredentialConsumersUseCase(
        CredentialCrudServiceInMemory credentialCrudService,
        CredentialConsumersInMemory credentialConsumers
    ) {
        return new ListCredentialConsumersUseCase(credentialCrudService, credentialConsumers);
    }
}
