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
package io.gravitee.gamma.rest.core.guardian.model;

import java.time.Instant;

/**
 * A definition of a Guardian once deployed to the gateways, as its deployment history records it.
 *
 * @param id the id of the history entry
 * @param deployerId the id of the user who deployed it; {@code null} when the deployment does not record one
 * @param definition the definition the gateways ran from {@code deployedAt}
 */
public record GuardianRevision(
    String id,
    Instant deployedAt,
    String label,
    String deployerId,
    io.gravitee.definition.model.v4.guardian.Guardian definition
) {}
