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
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretAuditLog;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteSecretUseCase {

    private final SecretRepository secretRepository;
    private final SecretAuditLog secretAuditLog;
    private final Clock clock;

    public record Input(String organizationId, String environmentId, String id, SecretActor actor) {}

    public record Output() {}

    public Output execute(Input input) {
        var secret = secretRepository.get(input.organizationId(), input.environmentId(), input.id());
        secretRepository.delete(secret);
        secretAuditLog.record(SecretAuditEvent.SECRET_DELETED, secret, null, input.actor(), clock.instant());
        return new Output();
    }
}
