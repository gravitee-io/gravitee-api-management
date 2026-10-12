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
package io.gravitee.gamma.rest.core.observability.dashboard.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * Raised when a dashboard, or a request to save one, breaks a validation rule; answered with
 * {@code 400}. A technical code is set where a client has to tell the rule apart.
 *
 * @author GraviteeSource Team
 */
public class InvalidDashboardException extends ValidationDomainException {

    public InvalidDashboardException(String message) {
        super(message);
    }

    public InvalidDashboardException(String message, String technicalCode) {
        super(message, technicalCode);
    }
}
