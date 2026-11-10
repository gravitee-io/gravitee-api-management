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
import io.gravitee.apim.core.credential.domain_service.CredentialSecretsDomainService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.apim.core.credential.model.CredentialAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Writes new values into the secrets of a credential Gravitee stores; the id and references stay the same. */
@UseCase
@RequiredArgsConstructor
public class RotateCredentialUseCase {

    private final CredentialCrudService credentialCrudService;
    private final CredentialSecretsDomainService credentialSecretsDomainService;
    private final CredentialAuditDomainService credentialAuditDomainService;

    /** @param values the new value of each secret field to rotate */
    public record Input(CredentialActor actor, String credentialId, Map<String, String> values) {
        @Override
        public String toString() {
            return "Input[actor=" + actor + ", credentialId=" + credentialId + ", fields=" + values.keySet() + "]";
        }
    }

    public record Output(Credential credential) {}

    public Output execute(Input input) {
        var actor = input.actor();
        var existing = credentialCrudService.get(actor.organizationId(), actor.environmentId(), input.credentialId());
        existing.requireManageableBy(actor);
        existing.requireRotatableWith(input.values());
        actor.requireSecretAccess(SecretAccess.UPDATE);

        credentialSecretsDomainService.rotate(existing, input.values(), actor.auditInfo());
        var rotated = credentialCrudService.update(existing.rotated(TimeProvider.instantNow()));

        credentialAuditDomainService.record(actor, CredentialAuditEvent.CREDENTIAL_ROTATED, existing, rotated);
        return new Output(rotated);
    }
}
