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
package io.gravitee.gamma.rest.resources.credential.dto;

import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.NewCredentialCommand;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import java.time.Instant;
import java.util.Map;

/**
 * A credential to create, with either {@code references} to secrets that exist or {@code values} for Gravitee to store.
 * The rules live on {@link NewCredentialCommand}, so they hold for every caller.
 */
@RejectUnknownProperties
public record CreateCredentialRequestDto(
    String name,
    CredentialStrategy strategy,
    String serviceId,
    String ownerId,
    Instant expiresAt,
    Map<String, Object> configuration,
    Map<String, String> references,
    Map<String, String> values
) {
    public NewCredentialCommand toCommand() {
        return new NewCredentialCommand(name, strategy, serviceId, ownerId, expiresAt, configuration, references, values);
    }

    @Override
    public String toString() {
        return toCommand().toString();
    }
}
