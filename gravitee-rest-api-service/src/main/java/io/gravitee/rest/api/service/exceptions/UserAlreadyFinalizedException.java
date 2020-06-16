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

import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserAlreadyFinalizedException extends AbstractManagementException {

    private final String organization;

    public UserAlreadyFinalizedException(String organization) {
        this.organization = organization;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "User already finalized in organization " + organization + ".";
    }

    @Override
    public String getTechnicalCode() {
        return "user.finalized";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("organization", organization);
    }
}
