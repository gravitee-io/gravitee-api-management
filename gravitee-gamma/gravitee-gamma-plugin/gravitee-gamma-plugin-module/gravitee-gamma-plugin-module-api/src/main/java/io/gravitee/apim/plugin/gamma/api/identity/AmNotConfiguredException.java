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
package io.gravitee.apim.plugin.gamma.api.identity;

import io.gravitee.apim.core.exception.AbstractDomainException;

// APIM's plugin SPI does not allow registering a plugin-local JAX-RS exception mapper, so each
// module's REST boundary translates this explicitly to 503 {"code": "am_not_configured", ...}.
public class AmNotConfiguredException extends AbstractDomainException {

    public static final String CODE = "am_not_configured";

    public AmNotConfiguredException() {
        super("Access Management is not configured for this module.");
    }

    public AmNotConfiguredException(String message) {
        super(message);
    }

    public String code() {
        return CODE;
    }
}
