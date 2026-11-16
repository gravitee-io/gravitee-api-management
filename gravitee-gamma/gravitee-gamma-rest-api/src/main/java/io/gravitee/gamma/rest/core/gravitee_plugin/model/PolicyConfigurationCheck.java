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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

/**
 * What came of checking a policy configuration against the schema its plugin declares.
 *
 * <p>Answered rather than thrown: a caller checking a sequence of steps reports every refusal at once,
 * and the message belongs to the step rather than to the policy.
 *
 * @author GraviteeSource Team
 */
public sealed interface PolicyConfigurationCheck {
    /**
     * The configuration conforms — but not necessarily the one submitted.
     *
     * @param configuration what the policy accepts, which the validator may have corrected on the way:
     *     {@code null} entries dropped, the default of a missing required property injected, properties
     *     a closed schema forbids pruned. This is the configuration to store: keeping the submitted one
     *     would store a configuration the policy was never validated with, which is how a Guardian the
     *     API accepted could still fail to start on a gateway (AIAM-1750). An API flow stores it the
     *     same way.
     */
    record Conforming(String configuration) implements PolicyConfigurationCheck {}

    /**
     * The configuration does not conform.
     *
     * @param message the schema error to report, naming the paths and keywords the validator reported
     */
    record Violation(String message) implements PolicyConfigurationCheck {}
}
