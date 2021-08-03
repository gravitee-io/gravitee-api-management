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
package io.gravitee.rest.api.management.rest.resource.param.healthcheck;

import io.gravitee.rest.api.management.rest.resource.param.AbstractParam;
import javax.ws.rs.WebApplicationException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthcheckFieldParam extends AbstractParam<HealthcheckFieldParam.HealthcheckField> {

    public enum HealthcheckField {
        ENDPOINT,
        GATEWAY,
    }

    public HealthcheckFieldParam(String param) throws WebApplicationException {
        super(param);
    }

    @Override
    protected HealthcheckField parse(String param) throws Throwable {
        try {
            if (param != null) {
                return HealthcheckField.valueOf(param.toUpperCase());
            }
        } catch (IllegalArgumentException iae) {}

        return null;
    }
}
