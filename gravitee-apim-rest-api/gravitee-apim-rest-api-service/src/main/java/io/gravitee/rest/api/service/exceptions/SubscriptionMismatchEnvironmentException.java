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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionMismatchEnvironmentException extends AbstractManagementException {

    private final String applicationId;
    private final String planId;

    public SubscriptionMismatchEnvironmentException(final String applicationId, final String planId) {
        this.applicationId = applicationId;
        this.planId = planId;
    }

    @Override
    public String getMessage() {
        return ("Application [" + applicationId + "] and plan [" + planId + "] doesn't belong to the same environment");
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "subscription.environment.mismatch";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("applicationId", applicationId, "planId", planId);
    }
}
