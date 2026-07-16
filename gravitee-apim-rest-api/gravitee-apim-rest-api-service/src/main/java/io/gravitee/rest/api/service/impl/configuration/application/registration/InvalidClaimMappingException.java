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
package io.gravitee.rest.api.service.impl.configuration.application.registration;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Map;

/**
 * Raised when a client registration provider is configured with an invalid claim mapping (blank DCR field path or a
 * path targeting a security-relevant field).
 *
 * @author GraviteeSource Team
 */
public class InvalidClaimMappingException extends AbstractManagementException {

    private final String detail;

    public InvalidClaimMappingException(String detail) {
        this.detail = detail;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "Invalid claim mapping: " + detail;
    }

    @Override
    public String getTechnicalCode() {
        return "clientRegistrationProvider.claimMappings.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }
}
