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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialAuditDomainService;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialAuditEvent;
import io.gravitee.gamma.module.platform.core.credential.model.UpdateCredentialCommand;
import io.gravitee.gamma.module.platform.core.credential.port.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateCredentialUseCase {

    private final CredentialRepository credentialRepository;
    private final CredentialOwnerDomainService credentialOwnerDomainService;
    private final CredentialAuditDomainService credentialAuditDomainService;

    public record Input(CredentialActor actor, String credentialId, UpdateCredentialCommand command) {}

    public record Output(Credential credential) {}

    public Output execute(Input input) {
        var actor = input.actor();
        var existing = credentialRepository.get(actor.organizationId(), actor.environmentId(), input.credentialId());
        existing.requireManageableBy(actor);

        var command = input.command();
        if (command.ownerId() != null && !command.ownerId().equals(existing.ownerId())) {
            credentialOwnerDomainService.ownerFor(actor, command.ownerId());
        }
        var changed = existing.update(command, TimeProvider.instantNow());
        if (!changed.secretReferences().equals(existing.secretReferences())) {
            changed.requireReferencesUsableBy(actor);
        }
        var updated = credentialRepository.update(changed);

        credentialAuditDomainService.record(actor, CredentialAuditEvent.CREDENTIAL_UPDATED, existing, updated);
        return new Output(updated);
    }
}
