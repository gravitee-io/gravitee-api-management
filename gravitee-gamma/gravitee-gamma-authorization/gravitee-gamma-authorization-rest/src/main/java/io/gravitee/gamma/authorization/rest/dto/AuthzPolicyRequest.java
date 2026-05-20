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
package io.gravitee.gamma.authorization.rest.dto;

import io.gravitee.gamma.authorization.api.AuthzEntityIdConstants;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthzPolicyRequest(
    @NotBlank String name,
    @NotNull AuthzPolicyKind kind,
    // Mirrors CreateAuthzPolicyCommand.entityId — null is allowed (kind=GLOBAL),
    // but if provided it must match the canonical id grammar so the request
    // surfaces a 400 at the edge instead of poisoning the event log.
    @Size(max = AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH) @Pattern(regexp = AuthzEntityIdConstants.FORMAT_REGEX) String entityId,
    @NotNull String policyText
) {}
