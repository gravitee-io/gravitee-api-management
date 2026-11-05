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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What updating a Guardian takes: the whole definition, plus the description.
 *
 * <p>A {@code PUT} replaces rather than patches, so both travel in full — what this command leaves
 * out is left out of the stored Guardian too. Only the identity, which comes from the path, and the
 * deployment stamp, which only a deployment writes, are kept back by {@link Guardian#update}.
 *
 * <p>The definition carries {@link NotNull} but deliberately <strong>not</strong> {@code @Valid}.
 * Not because validating a definition is unsafe — it is not — but because the definition declares
 * {@code @NotBlank} on its {@code id} and {@code environmentId}, and this body carries neither: both
 * come from the path, and {@link Guardian#update} fills them in once the stored Guardian has been
 * read. Cascading would refuse every legitimate update for a reason that has nothing to do with what
 * the caller sent. {@code GuardianDefinitionValidationTest} pins it.
 *
 * <p>The definition's own constraints are not skipped, only deferred: once {@link Guardian#update}
 * has merged the identity in, {@code ValidateGuardianDomainService.validateDefinition} runs them on
 * the result.
 *
 * <p>Creation does not have the problem: {@link CreateGuardian} carries the definition's attributes
 * flattened, with its own constraints.
 *
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UpdateGuardianCommand {

    @NotNull
    private io.gravitee.definition.model.v4.guardian.Guardian definition;

    @Size(max = CreateGuardian.DESCRIPTION_MAX_LENGTH)
    private String description;
}
