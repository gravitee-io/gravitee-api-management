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
package io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider;

import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyConfigurationCheck;

/**
 * Judges a policy configuration against the schema the policy plugin declares.
 *
 * <p>Its own port rather than a method on {@link PolicyPluginProvider}: that one answers what the
 * plugin registry holds, this one runs a configuration through the platform's JSON schema validation.
 * The product already owns that chain — {@code PolicyValidationDomainService}, which the API flows
 * validate their steps with — and the core cannot call it directly only because the exception it
 * throws, {@code io.gravitee.rest.api.service.exceptions.InvalidDataException}, is outside the
 * packages {@code CoreRulesTest} allows here (AGENTS.md §5). That is the whole reason this port
 * exists; its implementation is a few lines over the shared service.
 *
 * @author GraviteeSource Team
 */
public interface PolicyConfigurationValidator {
    /**
     * Checks a step configuration against the schema the policy declares.
     *
     * <p>The caller is expected to hand a JSON object: the implementation refuses anything else on its
     * own rather than trusting that, because the parser message of a configuration it cannot read
     * quotes the configuration, which may carry a credential. A policy that declares no schema accepts
     * every configuration.
     *
     * <p>A conforming answer carries the configuration <em>as the policy accepts it</em>, which the
     * caller is expected to store — see {@link PolicyConfigurationCheck.Conforming}.
     *
     * <p>Answers rather than throws, for a policy the registry no longer knows as for any other case:
     * a step naming a policy uninstalled since the registry was read is reported as a refused step,
     * not as a 404 out of a validation endpoint.
     */
    PolicyConfigurationCheck check(String policyId, String configuration);
}
