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
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretAuditLog;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretValueEncryptor;
import io.gravitee.gamma.module.platform.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.GetSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.ListSecretsUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.gamma.module.platform.infra.repository.secret.ApimSecretRepository;
import io.gravitee.gamma.module.platform.infra.service_provider.secret.AesGcmSecretValueEncryptor;
import io.gravitee.gamma.module.platform.infra.service_provider.secret.ApimSecretAuditLog;
import io.gravitee.gamma.module.platform.infra.service_provider.secret.UnavailableSecretValueEncryptor;
import java.time.Clock;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@CustomLog
@Configuration
public class SecretConfiguration {

    private static final Clock CLOCK = Clock.systemUTC();

    @Bean
    public SecretRepository platformSecretRepository(io.gravitee.repository.management.api.SecretRepository secretRepository) {
        return new ApimSecretRepository(secretRepository);
    }

    // A bad key must not stop the module context: the AM settings live in the same module.
    @Bean
    public SecretValueEncryptor platformSecretValueEncryptor(Environment environment) {
        try {
            return new AesGcmSecretValueEncryptor(environment.getProperty(AesGcmSecretValueEncryptor.KEY_PROPERTY));
        } catch (IllegalArgumentException e) {
            log.warn("The platform module cannot store secret values: {}", e.getMessage());
            return new UnavailableSecretValueEncryptor();
        }
    }

    @Bean
    public SecretAuditLog platformSecretAuditLog(AuditDomainService auditDomainService) {
        return new ApimSecretAuditLog(auditDomainService);
    }

    @Bean
    public CreateSecretUseCase createSecretUseCase(
        SecretRepository secretRepository,
        SecretValueEncryptor secretValueEncryptor,
        SecretAuditLog secretAuditLog
    ) {
        return new CreateSecretUseCase(secretRepository, secretValueEncryptor, secretAuditLog, CLOCK);
    }

    @Bean
    public UpdateSecretUseCase updateSecretUseCase(
        SecretRepository secretRepository,
        SecretValueEncryptor secretValueEncryptor,
        SecretAuditLog secretAuditLog
    ) {
        return new UpdateSecretUseCase(secretRepository, secretValueEncryptor, secretAuditLog, CLOCK);
    }

    @Bean
    public DeleteSecretUseCase deleteSecretUseCase(SecretRepository secretRepository, SecretAuditLog secretAuditLog) {
        return new DeleteSecretUseCase(secretRepository, secretAuditLog, CLOCK);
    }

    @Bean
    public GetSecretUseCase getSecretUseCase(SecretRepository secretRepository) {
        return new GetSecretUseCase(secretRepository);
    }

    @Bean
    public ListSecretsUseCase listSecretsUseCase(SecretRepository secretRepository) {
        return new ListSecretsUseCase(secretRepository);
    }
}
