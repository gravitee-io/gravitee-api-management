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
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.query_service.SecretQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchSecretsUseCase {

    private final SecretQueryService secretQueryService;

    public record Input(String organizationId, String environmentId, Pageable pageable) {}

    public record Output(Page<Secret> secrets) {}

    public Output execute(Input input) {
        return new Output(secretQueryService.search(input.organizationId(), input.environmentId(), input.pageable()));
    }
}
