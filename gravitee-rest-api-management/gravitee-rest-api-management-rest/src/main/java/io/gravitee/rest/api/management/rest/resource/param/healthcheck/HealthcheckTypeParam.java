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

import javax.ws.rs.WebApplicationException;

import io.gravitee.rest.api.management.rest.resource.param.AbstractParam;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthcheckTypeParam extends AbstractParam<HealthcheckTypeParam.HealthcheckType> {

    public enum HealthcheckType {
        AVAILABILITY,
        RESPONSE_TIME
    }

    public HealthcheckTypeParam(String param) throws WebApplicationException {
        super(param);
    }

    @Override
    protected HealthcheckType parse(String param) throws Throwable {
        try {
            if (param != null) {
                return HealthcheckType.valueOf(param.toUpperCase());
            }
        } catch (IllegalArgumentException iae) {
        }

        return null;
    }

}
