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
package io.gravitee.apim.core.credential.model;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;

/**
 * The new details of a credential. The strategy cannot change, and the values are changed by a rotation.
 *
 * @param ownerId the new owner, or {@code null} to keep the owner
 * @param references new references, or {@code null} to keep them; only for a credential that points at the user's own secrets
 */
@Builder
public record UpdateCredentialCommand(
    String name,
    String serviceId,
    String ownerId,
    Instant expiresAt,
    Map<String, Object> configuration,
    Map<String, String> references
) {}
