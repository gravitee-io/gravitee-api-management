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
import java.util.Collections;
import java.util.Map;

/**
 * Exception thrown when a client certificate is already used by another active application.
 *
 * @author GraviteeSource Team
 */
public class ClientCertificateAlreadyUsedException extends AbstractManagementException {

    private final String fingerprint;

    public ClientCertificateAlreadyUsedException(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "Client certificate with fingerprint [" + fingerprint + "] is already used by another active application.";
    }

    @Override
    public String getTechnicalCode() {
        return "application.certificate.alreadyUsed";
    }

    @Override
    public Map<String, String> getParameters() {
        return Collections.singletonMap("fingerprint", fingerprint);
    }
}
