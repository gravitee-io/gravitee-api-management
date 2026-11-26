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
import io.gravitee.apim.core.credential.domain_service.CredentialExpressionDomainService;
import io.gravitee.apim.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialAuditEvent;
import io.gravitee.apim.core.credential.model.CredentialConsumerWarning;
import io.gravitee.apim.core.credential.model.UpdateCredentialCommand;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumerPort;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumers;
import io.gravitee.common.utils.TimeProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateCredentialUseCase {

    private final CredentialCrudService credentialCrudService;
    private final CredentialOwnerDomainService credentialOwnerDomainService;
    private final CredentialAuditDomainService credentialAuditDomainService;
    private final CredentialExpressionDomainService credentialExpressionDomainService;
    private final CredentialConsumers credentialConsumers;

    public record Input(CredentialActor actor, String credentialId, UpdateCredentialCommand command) {}

    public record Output(Credential credential, List<CredentialConsumerWarning> warnings) {}

    public Output execute(Input input) {
        var actor = input.actor();
        var existing = credentialCrudService.get(actor.organizationId(), actor.environmentId(), input.credentialId());
        existing.requireManageableBy(actor);

        var command = input.command();
        if (command.ownerId() != null && !command.ownerId().equals(existing.ownerId())) {
            credentialOwnerDomainService.ownerFor(actor, command.ownerId());
        }
        var changed = existing.update(command, TimeProvider.instantNow());
        var reachesConsumers =
            !changed.secretReferences().equals(existing.secretReferences()) || !changed.configuration().equals(existing.configuration());
        // Where the secrets go and who controls that matter as much as the references: a new tokenUrl sends clientSecret elsewhere.
        if (reachesConsumers || !changed.ownerId().equals(existing.ownerId())) {
            credentialExpressionDomainService.requireValidExpressions(changed);
            changed.requireValidStoreReferences(actor);
        }
        var updated = credentialCrudService.update(changed);

        credentialAuditDomainService.record(actor, CredentialAuditEvent.CREDENTIAL_UPDATED, existing, updated);
        var warnings = reachesConsumers
            ? credentialConsumers.changed(
                new CredentialConsumerPort.Change(actor.organizationId(), actor.environmentId(), updated.id(), actor.userId())
            )
            : List.<CredentialConsumerWarning>of();
        return new Output(updated, warnings);
    }
}
