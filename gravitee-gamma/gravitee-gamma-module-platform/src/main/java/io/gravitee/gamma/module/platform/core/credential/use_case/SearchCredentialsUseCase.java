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
package io.gravitee.gamma.module.platform.core.credential.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import io.gravitee.gamma.module.platform.core.credential.port.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchCredentialsUseCase {

    private final CredentialRepository credentialRepository;

    /** @param pageNumber the page, from 0 */
    public record Input(CredentialActor actor, String query, int pageNumber, int pageSize) {}

    public record Output(Page<Credential> page) {}

    public Output execute(Input input) {
        var actor = input.actor();
        return new Output(
            credentialRepository.search(actor.organizationId(), actor.environmentId(), input.query(), input.pageNumber(), input.pageSize())
        );
    }
}
