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
package io.gravitee.apim.core.secret.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.secret.domain_service.SecretDomainService;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretValue;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateSecretUseCase {

    private final SecretDomainService secretDomainService;

    public record Input(String key, SecretValue value, AuditInfo auditInfo) {}

    public record Output(Secret secret) {}

    public Output execute(Input input) {
        Secret.requireUserKey(input.key());
        return new Output(secretDomainService.create(input.key(), input.value(), input.auditInfo()));
    }
}
