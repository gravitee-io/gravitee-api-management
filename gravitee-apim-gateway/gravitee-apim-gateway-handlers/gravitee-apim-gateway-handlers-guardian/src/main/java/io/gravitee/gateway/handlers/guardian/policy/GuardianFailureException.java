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
package io.gravitee.gateway.handlers.guardian.policy;

/**
 * Carries the detail of a Guardian failure as the cause of the {@link io.gravitee.gateway.reactive.api.ExecutionFailure}:
 * the detail reaches the diagnostic reported in the metrics, while the client only gets the generic failure message.
 *
 * @author GraviteeSource Team
 */
final class GuardianFailureException extends RuntimeException {

    GuardianFailureException(String message) {
        // Raised on the request path for an expected condition: a stack trace would be costly and say nothing.
        super(message, null, false, false);
    }
}
