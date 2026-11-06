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
package io.gravitee.gamma.module.platform.fixture;

import io.gravitee.gamma.module.platform.core.secret.inmemory.SecretAuditLogInMemory;
import io.gravitee.gamma.module.platform.core.secret.inmemory.SecretRepositoryInMemory;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretValue;
import io.gravitee.gamma.module.platform.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.GetSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.ListSecretsUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.gamma.module.platform.infra.service_provider.secret.AesGcmSecretValueEncryptor;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class SecretFixture {

    public static final String ORGANIZATION_ID = "org-1";
    public static final String ENVIRONMENT_ID = "env-1";
    public static final SecretActor ACTOR = new SecretActor("user-1", "memory", "user-1");

    public final MutableClock clock = new MutableClock(Instant.parse("2026-11-06T10:00:00Z"));
    public final SecretRepositoryInMemory secretRepository = new SecretRepositoryInMemory();
    public final SecretAuditLogInMemory auditLog = new SecretAuditLogInMemory();
    public final AesGcmSecretValueEncryptor encryptor = new AesGcmSecretValueEncryptor(Base64.getEncoder().encodeToString(new byte[32]));

    public final CreateSecretUseCase createSecretUseCase = new CreateSecretUseCase(secretRepository, encryptor, auditLog, clock);
    public final UpdateSecretUseCase updateSecretUseCase = new UpdateSecretUseCase(secretRepository, encryptor, auditLog, clock);
    public final DeleteSecretUseCase deleteSecretUseCase = new DeleteSecretUseCase(secretRepository, auditLog, clock);
    public final GetSecretUseCase getSecretUseCase = new GetSecretUseCase(secretRepository);
    public final ListSecretsUseCase listSecretsUseCase = new ListSecretsUseCase(secretRepository);

    public CreateSecretUseCase.Input.InputBuilder aCreateInput() {
        return CreateSecretUseCase.Input.builder()
            .id(UUID.randomUUID().toString())
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .key("anthropic-key")
            .value(new SecretValue("sk-ant-123"))
            .actor(ACTOR);
    }

    public Secret givenASecret() {
        return givenASecret(input -> input);
    }

    public Secret givenASecret(UnaryOperator<CreateSecretUseCase.Input.InputBuilder> customizer) {
        return createSecretUseCase.execute(customizer.apply(aCreateInput()).build()).secret();
    }
}
