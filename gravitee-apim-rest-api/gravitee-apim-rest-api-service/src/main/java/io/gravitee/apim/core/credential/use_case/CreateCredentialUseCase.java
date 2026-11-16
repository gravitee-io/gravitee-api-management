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
import io.gravitee.apim.core.credential.domain_service.CredentialAuditDomainService;
import io.gravitee.apim.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.apim.core.credential.domain_service.CredentialSecretsDomainService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.apim.core.credential.model.CredentialAuditEvent;
import io.gravitee.apim.core.credential.model.NewCredentialCommand;
import io.gravitee.common.utils.TimeProvider;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateCredentialUseCase {

    private final CredentialCrudService credentialCrudService;
    private final CredentialSecretsDomainService credentialSecretsDomainService;
    private final CredentialOwnerDomainService credentialOwnerDomainService;
    private final CredentialAuditDomainService credentialAuditDomainService;

    public record Input(String credentialId, CredentialActor actor, NewCredentialCommand command) {}

    public record Output(Credential credential) {}

    public Output execute(Input input) {
        var actor = input.actor();
        var command = input.command();
        var owner = credentialOwnerDomainService.ownerFor(actor, command.ownerId());
        var credential = Credential.create(input.credentialId(), actor, command, owner, TimeProvider.instantNow());
        credential.requireReferencesUsableBy(actor);

        if (command.isManaged()) {
            actor.requireSecretAccess(SecretAccess.CREATE);
            credential = credential.withSecretReferences(
                credentialSecretsDomainService.store(credential, command.values(), actor.auditInfo())
            );
        }

        try {
            credentialCrudService.create(credential);
        } catch (RuntimeException e) {
            if (command.isManaged()) {
                credentialSecretsDomainService.discard(credential.secretReferences().values(), actor.auditInfo(), e);
            }
            throw e;
        }

        credentialAuditDomainService.record(actor, CredentialAuditEvent.CREDENTIAL_CREATED, null, credential);
        return new Output(credential);
    }
}
