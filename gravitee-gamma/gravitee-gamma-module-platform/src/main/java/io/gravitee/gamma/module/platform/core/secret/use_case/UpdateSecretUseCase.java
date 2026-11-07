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
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.model.SecretValue;
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretAuditLog;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretValueEncryptor;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateSecretUseCase {

    private final SecretRepository secretRepository;
    private final SecretValueEncryptor secretValueEncryptor;
    private final SecretAuditLog secretAuditLog;
    private final Clock clock;

    public record Input(String organizationId, String environmentId, String id, SecretValue value, SecretActor actor) {}

    public record Output(Secret secret) {}

    public Output execute(Input input) {
        var current = secretRepository.get(input.organizationId(), input.environmentId(), input.id());
        var rotated = secretRepository.rotate(
            current.rotatedAt(clock.instant()),
            secretValueEncryptor.encrypt(current, input.value().plaintext())
        );
        secretAuditLog.record(SecretAuditEvent.SECRET_UPDATED, current, rotated, input.actor(), rotated.updatedAt());
        return new Output(rotated);
    }
}
