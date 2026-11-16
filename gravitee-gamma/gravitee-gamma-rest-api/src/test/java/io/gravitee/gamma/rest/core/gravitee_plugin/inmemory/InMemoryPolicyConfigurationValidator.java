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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyConfigurationCheck;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyConfigurationValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The schema validation as a domain test installs it, honouring the contract of the port rather than
 * stubbing its answers.
 *
 * <p>It is what lets a test tell "the configuration of <em>this</em> step was checked" from "something
 * was checked": the answer depends on the configuration as much as on the policy, {@link #checks()}
 * says what was asked, and a configuration that is no JSON object is refused the way the real
 * validator refuses it — the invariant the port documents, which a stubbed answer would let a caller
 * break unnoticed.
 *
 * @author GraviteeSource Team
 */
public final class InMemoryPolicyConfigurationValidator implements PolicyConfigurationValidator {

    private static final JsonDeserializer JSON = new JacksonJsonDeserializer();

    /** The violation a policy answers for any configuration it is given, by policy id. */
    private final Map<String, String> violations = new HashMap<>();

    /** What a policy accepts in place of a configuration, by submitted configuration. */
    private final Map<String, String> accepted = new HashMap<>();

    private final List<Check> checks = new ArrayList<>();

    /** A configuration the validator was asked about, in the order it was asked. */
    public record Check(String policyId, String configuration) {}

    public InMemoryPolicyConfigurationValidator givenRefuses(String policyId, String message) {
        violations.put(policyId, message);
        return this;
    }

    /**
     * Says that a policy accepts {@code submitted} only once corrected into {@code sanitised} — what
     * the real validator does when it drops a null entry, injects the default of a missing required
     * property or prunes what a closed schema forbids.
     */
    public InMemoryPolicyConfigurationValidator givenAccepts(String submitted, String sanitised) {
        accepted.put(submitted, sanitised);
        return this;
    }

    @Override
    public PolicyConfigurationCheck check(String policyId, String configuration) {
        checks.add(new Check(policyId, configuration));
        if (!isJsonObject(configuration)) {
            return new PolicyConfigurationCheck.Violation("it must be a JSON object");
        }
        var violation = violations.get(policyId);
        return violation != null
            ? new PolicyConfigurationCheck.Violation(violation)
            : new PolicyConfigurationCheck.Conforming(accepted.getOrDefault(configuration, configuration));
    }

    /** What the validator was asked to check, in order. */
    public List<Check> checks() {
        return List.copyOf(checks);
    }

    public void reset() {
        checks.clear();
        violations.clear();
        accepted.clear();
    }

    private static boolean isJsonObject(String configuration) {
        try {
            JsonNode node = JSON.deserialize(configuration, JsonNode.class);
            return node != null && node.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
