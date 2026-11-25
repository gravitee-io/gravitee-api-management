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
package io.gravitee.gamma.rest.core.guardian.port.service_provider;

import java.util.Optional;

/**
 * Reads the condition of a flow step the way the gateway does before evaluating it.
 *
 * <p>The gateway answers every call of the API with an error when a step condition does not parse, and skips the step
 * on every call when the condition is text rather than an expression — a Guardian silently running on no call. Both
 * are known before the condition is written.
 *
 * @author GraviteeSource Team
 */
public interface ConditionSyntaxPort {
    /**
     * @param condition a non-null step condition.
     * @return why the gateway cannot evaluate it as one expression, empty when it can. Says nothing of what it
     *     evaluates to on a call.
     */
    Optional<String> errorIn(String condition);
}
