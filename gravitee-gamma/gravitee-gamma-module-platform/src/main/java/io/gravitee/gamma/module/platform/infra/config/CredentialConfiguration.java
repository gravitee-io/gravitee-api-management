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
package io.gravitee.gamma.module.platform.infra.config;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.apim.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.apim.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.plugin.gamma.api.credential.CredentialConsumerPort;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialAuditDomainService;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialSecretsDomainService;
import io.gravitee.gamma.module.platform.core.credential.port.repository.CredentialRepository;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialConsumers;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialSecretStore;
import io.gravitee.gamma.module.platform.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.DeleteCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.GetCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.ListCredentialConsumersUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.RotateCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.UpdateCredentialUseCase;
import io.gravitee.gamma.module.platform.infra.repository.credential.ApimCredentialRepository;
import io.gravitee.gamma.module.platform.infra.service_provider.ApimCredentialSecretStore;
import io.gravitee.gamma.module.platform.infra.service_provider.ModuleCredentialConsumers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Every bean is named platformCredential*: the module's beans are copied into the host context, where a plain name
// such as credentialRepository would clash with the APIM repository bean.
@Configuration
public class CredentialConfiguration {

    @Bean
    public CredentialRepository platformCredentialRepository(io.gravitee.repository.management.api.CredentialRepository repository) {
        return new ApimCredentialRepository(repository);
    }

    @Bean
    public CredentialSecretStore platformCredentialSecretStore(
        CreateSecretUseCase createSecretUseCase,
        UpdateSecretUseCase updateSecretUseCase,
        DeleteSecretUseCase deleteSecretUseCase
    ) {
        return new ApimCredentialSecretStore(createSecretUseCase, updateSecretUseCase, deleteSecretUseCase);
    }

    @Bean
    public CredentialConsumers platformCredentialConsumers(ObjectProvider<CredentialConsumerPort> ports) {
        return new ModuleCredentialConsumers(ports);
    }

    @Bean
    public CredentialSecretsDomainService platformCredentialSecretsDomainService(CredentialSecretStore platformCredentialSecretStore) {
        return new CredentialSecretsDomainService(platformCredentialSecretStore);
    }

    @Bean
    public CredentialAuditDomainService platformCredentialAuditDomainService(AuditDomainService auditDomainService) {
        return new CredentialAuditDomainService(auditDomainService);
    }

    @Bean
    public CredentialOwnerDomainService platformCredentialOwnerDomainService(UserCrudService userCrudService) {
        return new CredentialOwnerDomainService(userCrudService);
    }

    @Bean
    public CreateCredentialUseCase platformCreateCredentialUseCase(
        CredentialRepository platformCredentialRepository,
        CredentialSecretsDomainService platformCredentialSecretsDomainService,
        CredentialOwnerDomainService platformCredentialOwnerDomainService,
        CredentialAuditDomainService platformCredentialAuditDomainService
    ) {
        return new CreateCredentialUseCase(
            platformCredentialRepository,
            platformCredentialSecretsDomainService,
            platformCredentialOwnerDomainService,
            platformCredentialAuditDomainService
        );
    }

    @Bean
    public GetCredentialUseCase platformGetCredentialUseCase(CredentialRepository platformCredentialRepository) {
        return new GetCredentialUseCase(platformCredentialRepository);
    }

    @Bean
    public SearchCredentialsUseCase platformSearchCredentialsUseCase(CredentialRepository platformCredentialRepository) {
        return new SearchCredentialsUseCase(platformCredentialRepository);
    }

    @Bean
    public UpdateCredentialUseCase platformUpdateCredentialUseCase(
        CredentialRepository platformCredentialRepository,
        CredentialOwnerDomainService platformCredentialOwnerDomainService,
        CredentialAuditDomainService platformCredentialAuditDomainService
    ) {
        return new UpdateCredentialUseCase(
            platformCredentialRepository,
            platformCredentialOwnerDomainService,
            platformCredentialAuditDomainService
        );
    }

    @Bean
    public RotateCredentialUseCase platformRotateCredentialUseCase(
        CredentialRepository platformCredentialRepository,
        CredentialSecretsDomainService platformCredentialSecretsDomainService,
        CredentialAuditDomainService platformCredentialAuditDomainService
    ) {
        return new RotateCredentialUseCase(
            platformCredentialRepository,
            platformCredentialSecretsDomainService,
            platformCredentialAuditDomainService
        );
    }

    @Bean
    public DeleteCredentialUseCase platformDeleteCredentialUseCase(
        CredentialRepository platformCredentialRepository,
        CredentialSecretsDomainService platformCredentialSecretsDomainService,
        CredentialConsumers platformCredentialConsumers,
        CredentialAuditDomainService platformCredentialAuditDomainService
    ) {
        return new DeleteCredentialUseCase(
            platformCredentialRepository,
            platformCredentialSecretsDomainService,
            platformCredentialConsumers,
            platformCredentialAuditDomainService
        );
    }

    @Bean
    public ListCredentialConsumersUseCase platformListCredentialConsumersUseCase(
        CredentialRepository platformCredentialRepository,
        CredentialConsumers platformCredentialConsumers
    ) {
        return new ListCredentialConsumersUseCase(platformCredentialRepository, platformCredentialConsumers);
    }
}
