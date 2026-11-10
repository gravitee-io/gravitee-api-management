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
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialAuditDomainService;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialSecretsDomainService;
import io.gravitee.gamma.module.platform.core.credential.exception.CredentialInUseException;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialAuditEvent;
import io.gravitee.gamma.module.platform.core.credential.port.repository.CredentialRepository;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialConsumers;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteCredentialUseCase {

    private final CredentialRepository credentialRepository;
    private final CredentialSecretsDomainService credentialSecretsDomainService;
    private final CredentialConsumers credentialConsumers;
    private final CredentialAuditDomainService credentialAuditDomainService;

    public record Input(CredentialActor actor, String credentialId) {}

    public record Output() {}

    public Output execute(Input input) {
        var actor = input.actor();
        var credential = credentialRepository.get(actor.organizationId(), actor.environmentId(), input.credentialId());
        credential.requireManageableBy(actor);
        if (credential.isManaged()) {
            actor.requireSecretAccess(SecretAccess.DELETE);
        }

        var consumers = credentialConsumers.of(actor.environmentId(), credential.id());
        if (!consumers.isEmpty()) {
            throw new CredentialInUseException(credential.id(), consumers);
        }

        // Secrets first: if a delete fails, the credential still lists the secrets left to clean up.
        credentialSecretsDomainService.deleteAll(credential, actor.auditInfo());
        credentialRepository.delete(credential);

        credentialAuditDomainService.record(actor, CredentialAuditEvent.CREDENTIAL_DELETED, credential, null);
        return new Output();
    }
}
