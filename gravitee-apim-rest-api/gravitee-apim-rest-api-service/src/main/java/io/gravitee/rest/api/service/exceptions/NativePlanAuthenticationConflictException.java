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

import static java.util.Collections.singletonMap;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.Plan;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class NativePlanAuthenticationConflictException extends AbstractManagementException {

    private final Plan.PlanSecurityType planToPublishType;

    public NativePlanAuthenticationConflictException(Plan.PlanSecurityType planToPublishType) {
        this.planToPublishType = planToPublishType;
    }

    @Override
    public String getMessage() {
        if (planToPublishType == Plan.PlanSecurityType.KEY_LESS) {
            return "A plan with mTLS or authentication is already published for the Native API. Keyless plans cannot be combined with mTLS or authentication plans.";
        } else if (planToPublishType == Plan.PlanSecurityType.MTLS) {
            return "A Keyless or authentication plan is already published for the Native API. mTLS plans cannot be combined with Keyless or authentication plans.";
        } else {
            return "A Keyless or mTLS plan is already published for the Native API. Authentication plans cannot be combined with Keyless or mTLS plans.";
        }
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "plan.native.authentication.conflict";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("planToPublishType", planToPublishType.name());
    }
}
