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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListSecretsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final SecretRepository secretRepository;

    /** The first page is page 1. */
    public record Input(String organizationId, String environmentId, int pageNumber, int pageSize) {}

    public record Output(Page<Secret> secrets) {}

    public Output execute(Input input) {
        if (input.pageNumber() < 1 || input.pageSize() < 1 || input.pageSize() > MAX_PAGE_SIZE) {
            throw new ValidationDomainException("The page must be 1 or more and hold 1 to " + MAX_PAGE_SIZE + " secrets.");
        }
        return new Output(secretRepository.search(input.organizationId(), input.environmentId(), input.pageNumber(), input.pageSize()));
    }
}
