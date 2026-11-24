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
package io.gravitee.gamma.rest.resources.guardrail.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailRevision;
import java.time.Instant;

/**
 * A revision of a Guardrail in its deployment history. {@code deployedAt} is ISO 8601 in UTC, for the same reason
 * as in {@link GuardrailDto}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuardrailDeploymentDto(
    String id,
    String label,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant deployedAt,
    Deployer deployedBy
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Deployer(String id, String displayName) {
        static Deployer from(BaseUserEntity user) {
            return user == null ? null : new Deployer(user.getId(), user.displayName());
        }
    }

    public static GuardrailDeploymentDto from(GuardrailRevision revision) {
        return new GuardrailDeploymentDto(revision.id(), revision.label(), revision.deployedAt(), Deployer.from(revision.deployer()));
    }
}
