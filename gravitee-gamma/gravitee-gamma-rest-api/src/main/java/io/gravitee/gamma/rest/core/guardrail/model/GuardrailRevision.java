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
package io.gravitee.gamma.rest.core.guardrail.model;

import io.gravitee.apim.core.user.model.BaseUserEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A deployment of a Guardrail to the gateways, as its deployment history records it.
 *
 * @param id the id of the history entry
 * @param deployer the user who deployed it, known by id only when the platform no longer knows them; {@code null}
 *                 when the deployment does not record one
 */
public record GuardrailRevision(String id, Instant deployedAt, String label, BaseUserEntity deployer) {
    // the precision of GuardrailDeployment, so a revision dates the same as the Guardrail it deployed
    public GuardrailRevision {
        deployedAt = deployedAt.truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * A revision with the definition the gateways ran from its {@code deployedAt}.
     */
    public record Deployed(GuardrailRevision revision, io.gravitee.definition.model.v4.guardrail.Guardrail definition) {}
}
