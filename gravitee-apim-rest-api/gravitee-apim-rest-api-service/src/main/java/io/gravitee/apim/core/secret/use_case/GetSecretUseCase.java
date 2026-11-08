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
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.model.Secret;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSecretUseCase {

    private final SecretCrudService secretCrudService;

    public record Input(String secretId, String organizationId, String environmentId) {}

    public record Output(Secret secret) {}

    public Output execute(Input input) {
        return new Output(secretCrudService.get(input.organizationId(), input.environmentId(), input.secretId()));
    }
}
