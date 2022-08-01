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
package io.gravitee.rest.api.service.v4.exception;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.Maps;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.List;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvalidHostException extends AbstractManagementException {

    private final String host;
    private final List<String> restrictions;

    public InvalidHostException(String host, List<String> restrictions) {
        this.host = host;
        this.restrictions = restrictions;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "virtualHost.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        return Maps.<String, String>builder().put("host", host).put("restrictions", String.join(",", restrictions)).build();
    }

    @Override
    public String getMessage() {
        return "Host [" + host + "] must be a subdomain of " + restrictions;
    }
}
