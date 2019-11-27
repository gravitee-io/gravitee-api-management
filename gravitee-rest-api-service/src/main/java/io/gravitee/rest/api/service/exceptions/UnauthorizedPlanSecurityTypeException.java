/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.PlanSecurityType;

import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UnauthorizedPlanSecurityTypeException extends AbstractManagementException {

    private final String security;

    public UnauthorizedPlanSecurityTypeException(PlanSecurityType security) {
        this.security = security.name();
    }

    @Override
    public String getMessage() {
        return "The security type " + security + " is not allowed.";
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "plan.security.typeNotAllowed";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("type", security);
    }
}
