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
package io.gravitee.gamma.module.platform.core.secret.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.module.platform.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.model.SecretValue;
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretAuditLog;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretValueEncryptor;
import java.time.Clock;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateSecretUseCase {

    private final SecretRepository secretRepository;
    private final SecretValueEncryptor secretValueEncryptor;
    private final SecretAuditLog secretAuditLog;
    private final Clock clock;

    @Builder
    public record Input(String id, String organizationId, String environmentId, String key, SecretValue value, SecretActor actor) {}

    public record Output(Secret secret) {}

    public Output execute(Input input) {
        var secret = Secret.create(input.id(), input.organizationId(), input.environmentId(), input.key(), clock.instant());
        if (secretRepository.findByKey(secret.organizationId(), secret.environmentId(), secret.key()).isPresent()) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        }
        var created = secretRepository.create(secret, secretValueEncryptor.encrypt(secret, input.value().plaintext()));
        secretAuditLog.record(SecretAuditEvent.SECRET_CREATED, null, created, input.actor(), created.createdAt());
        return new Output(created);
    }
}
