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
package io.gravitee.apim.core.credential.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.credential.crud_service.CredentialCrudService;
import io.gravitee.apim.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCredentialUseCase {

    private final CredentialCrudService credentialCrudService;
    private final CredentialOwnerDomainService credentialOwnerDomainService;

    public record Input(CredentialActor actor, String credentialId) {}

    /** @param ownerName null when the owner is no longer a user */
    public record Output(Credential credential, String ownerName) {}

    public Output execute(Input input) {
        var actor = input.actor();
        var credential = credentialCrudService.get(actor.organizationId(), actor.environmentId(), input.credentialId());
        var ownerNames = credentialOwnerDomainService.namesOf(actor, List.of(credential.ownerId()));
        return new Output(credential, ownerNames.get(credential.ownerId()));
    }
}
