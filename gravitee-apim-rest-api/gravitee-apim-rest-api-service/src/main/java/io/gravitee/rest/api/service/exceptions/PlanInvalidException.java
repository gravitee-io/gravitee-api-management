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

import static java.util.Optional.ofNullable;

import io.gravitee.common.http.HttpStatusCode;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 * @deprecated use {@link io.gravitee.apim.core.plan.exception.PlanInvalidException} instead
 */
@Deprecated
public class PlanInvalidException extends AbstractManagementException {

    private final String plan;
    private final String reason;

    public PlanInvalidException(String plan, String reason) {
        this.plan = plan;
        this.reason = reason;
    }

    public PlanInvalidException(String reason) {
        this(null, reason);
    }

    @Override
    public String getMessage() {
        return plan != null ? "Plan " + plan + " invalid:" + reason : reason;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "plan.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("plan", ofNullable(plan).orElse(""), "reason", reason);
    }
}
