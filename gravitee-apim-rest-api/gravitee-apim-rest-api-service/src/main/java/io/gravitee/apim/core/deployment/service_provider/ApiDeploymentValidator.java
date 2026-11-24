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
package io.gravitee.apim.core.deployment.service_provider;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;

/**
 * A check an API passes before the gateways are given a new definition to run. Every implementation runs on every
 * deployment and on every start, so a module that owns part of what the gateway loads — the Guardrails of an API, the
 * cluster its endpoints bind to — refuses a definition the gateway could not run.
 *
 * <p>Implementations are collected from the Spring context, including the ones a Gamma module contributes, and must
 * be silent for the APIs they know nothing about.
 *
 * <p>It lives in its own slice rather than with the API domain services because the slices that implement it — the
 * cluster one among them — would otherwise point back at the API slice, which already points at theirs.
 *
 * @author GraviteeSource Team
 */
public interface ApiDeploymentValidator {
    /**
     * @param api the API as it is about to be deployed, never {@code null}, and not always the one the store holds: an
     *     implementation reads {@link Api#getApiDefinitionValue()} and never reads the API back, or it would judge
     *     another definition than the one the gateways are given. It is that <em>definition</em> that is {@code null}
     *     when it could not be parsed, and an implementation lets such an API through — the deployment then fails on
     *     the gateway side, where the definition is.
     * @throws ApiNotDeployableException when the API cannot run, with a message naming what has to be fixed.
     */
    void validateDeployable(Api api);

    /**
     * The same question narrowed to the internal flow of {@code api}: the part of a definition the platform owns and
     * republishes on its own, the rest of the definition staying where it was. A write that hands the gateways nothing
     * but a new internal flow — a rollback restoring the one of a revision — asks this question, so that it refuses
     * over what it is about to change and over nothing else.
     *
     * <p>A no-op by default: an implementation owning nothing of the internal flow takes no part in those writes.
     *
     * @param api the API carrying the internal flow about to be republished, never {@code null}; read from
     *     {@link Api#getApiDefinitionValue()}, under the same rule as {@link #validateDeployable(Api)}.
     * @throws ApiNotDeployableException when the internal flow cannot run, with a message naming what has to be fixed.
     */
    default void validateInternalFlowDeployable(Api api) {}
}
