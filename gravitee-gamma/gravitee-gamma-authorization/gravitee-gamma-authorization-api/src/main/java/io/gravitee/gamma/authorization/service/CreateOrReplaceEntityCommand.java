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
package io.gravitee.gamma.authorization.service;

import io.gravitee.gamma.authorization.api.Validators;
import io.gravitee.gamma.authorization.domain.EntityKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record CreateOrReplaceEntityCommand(
    @NotBlank String environmentId,
    @NotBlank String entityId,
    @NotNull EntityKind kind,
    Map<String, Object> attributes,
    List<String> parents,
    @NotBlank String source
) {
    public CreateOrReplaceEntityCommand {
        Validators.validateCtor(CreateOrReplaceEntityCommand.class, environmentId, entityId, kind, attributes, parents, source);
        attributes = Map.copyOf(attributes);
        parents = List.copyOf(parents);
    }
}
