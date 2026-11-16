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
package io.gravitee.gamma.rest.core.gravitee_plugin.inmemory;

import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The schema validation of the platform as a domain test installs it, in place of the service the
 * Guardian domain validates its step configurations with.
 *
 * <p>Answering on the configuration as much as on the policy is what lets a test tell "the
 * configuration of <em>this</em> step was checked" from "something was checked", and {@link #checks()}
 * says what was asked, in order. The real service corrects a document before accepting it, which
 * {@link #givenAccepts(String, String)} reproduces — the behaviour the whole fix turns on — and
 * answers its two refusals as exceptions, which is what the domain has to map.
 *
 * @author GraviteeSource Team
 */
public final class InMemoryPolicyValidation implements PolicyValidationDomainService {

    /** The schema error a policy answers for any configuration it is given, by policy id. */
    private final Map<String, String> violations = new HashMap<>();

    /** What a policy accepts in place of a configuration, by the configuration it was given. */
    private final Map<String, String> accepted = new HashMap<>();

    /** The policies this service no longer resolves, whatever the registry snapshot held. */
    private final Set<String> uninstalled = new HashSet<>();

    private final List<Check> checks = new ArrayList<>();

    /** A configuration the service was asked about, in the order it was asked. */
    public record Check(String policyId, String configuration) {}

    public InMemoryPolicyValidation givenRefuses(String policyId, String message) {
        violations.put(policyId, message);
        return this;
    }

    /**
     * Says that a policy accepts {@code submitted} only once corrected into {@code sanitised} — what
     * the real service does when it drops a null entry, injects the default of a missing required
     * property or prunes what a closed schema forbids.
     */
    public InMemoryPolicyValidation givenAccepts(String submitted, String sanitised) {
        accepted.put(submitted, sanitised);
        return this;
    }

    /** A policy uninstalled between the registry read and the check, which answers a 404 of its own. */
    public InMemoryPolicyValidation givenNotInstalled(String policyId) {
        uninstalled.add(policyId);
        return this;
    }

    @Override
    public String validateAndSanitizeConfiguration(String policyName, String configuration) {
        checks.add(new Check(policyName, configuration));
        if (uninstalled.contains(policyName)) {
            throw new PluginNotFoundException(policyName);
        }
        var violation = violations.get(policyName);
        if (violation != null) {
            throw new InvalidDataException(violation);
        }
        return accepted.getOrDefault(configuration, configuration);
    }

    @Override
    public void validatePoliciesFlowPhase(List<String> policyIds, ApiType apiType, FlowPhase phase) {
        throw new UnsupportedOperationException("The Guardian domain reads the declared flows of a plugin itself.");
    }

    /** What the service was asked to validate, in order. */
    public List<Check> checks() {
        return List.copyOf(checks);
    }

    public void reset() {
        checks.clear();
        violations.clear();
        accepted.clear();
        uninstalled.clear();
    }
}
