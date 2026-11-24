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

import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The schema validation of the resource plugins as a domain test installs it, the counterpart of
 * {@link InMemoryPolicyValidation}: it answers by type, corrects a document the way the real service
 * does, refuses with the exception the domain has to map, and says what it was asked.
 *
 * @author GraviteeSource Team
 */
public final class InMemoryResourceValidation implements ValidateResourceDomainService {

    /** The schema error a type answers for any configuration it is given, by type. */
    private final Map<String, String> violations = new HashMap<>();

    /** What a type accepts in place of the configuration it was given, by type. */
    private final Map<String, String> accepted = new HashMap<>();

    private final List<Check> checks = new ArrayList<>();

    /** A configuration the service was asked about, in the order it was asked. */
    public record Check(String type, String configuration) {}

    public InMemoryResourceValidation givenRefuses(String type, String message) {
        violations.put(type, message);
        return this;
    }

    /** Says that a type only accepts its configuration corrected, as when a null entry is dropped or a default injected. */
    public InMemoryResourceValidation givenRewrites(String type, String acceptedConfiguration) {
        accepted.put(type, acceptedConfiguration);
        return this;
    }

    @Override
    public String validateAndSanitizeConfiguration(String resourceType, String configuration) {
        checks.add(new Check(resourceType, configuration));
        var violation = violations.get(resourceType);
        if (violation != null) {
            throw new InvalidDataException(violation);
        }
        return accepted.getOrDefault(resourceType, configuration);
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        throw new UnsupportedOperationException("The Guardrail domain judges its resources one at a time.");
    }

    /** What the service was asked to validate, in order. */
    public List<Check> checks() {
        return List.copyOf(checks);
    }

    /** A bean of a test context is shared: what one test installed must not reach the next. */
    public void reset() {
        violations.clear();
        accepted.clear();
        checks.clear();
    }
}
