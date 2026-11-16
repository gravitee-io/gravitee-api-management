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

import io.gravitee.apim.core.credential.model.UpdateCredentialCommand;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import java.time.Instant;
import java.util.Map;

/**
 * The new details of a credential. Leave {@code ownerId} out to keep the owner, and {@code references} out, or send
 * back the ones read, to keep them.
 */
@RejectUnknownProperties
public record UpdateCredentialRequestDto(
    String name,
    String serviceId,
    String ownerId,
    Instant expiresAt,
    Map<String, Object> configuration,
    Map<String, String> references
) {
    public UpdateCredentialCommand toCommand() {
        return new UpdateCredentialCommand(name, serviceId, ownerId, expiresAt, configuration, references);
    }
}
