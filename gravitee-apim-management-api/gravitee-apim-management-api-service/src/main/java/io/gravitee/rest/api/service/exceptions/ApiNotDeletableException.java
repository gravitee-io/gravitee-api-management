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

import static java.util.Collections.singletonMap;

import io.gravitee.common.http.HttpStatusCode;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiNotDeletableException extends AbstractManagementException {

    private final String plans;

    public ApiNotDeletableException(Set<String> plans) {
        this.plans = String.join(", ", plans);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "Plan(s) [" + plans + "] must be closed before being able to delete the API !";
    }

    @Override
    public String getTechnicalCode() {
        return "api.notDeletable";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("plans", plans);
    }
}
