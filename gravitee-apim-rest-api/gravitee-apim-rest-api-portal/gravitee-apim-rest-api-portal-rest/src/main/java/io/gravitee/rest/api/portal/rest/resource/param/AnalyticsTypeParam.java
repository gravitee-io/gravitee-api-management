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
package io.gravitee.rest.api.portal.rest.resource.param;

import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class AnalyticsTypeParam extends AbstractParam<AnalyticsTypeParam.AnalyticsType> {

    public enum AnalyticsType {
        GROUP_BY,
        DATE_HISTO,
        COUNT,
        STATS,
    }

    public AnalyticsTypeParam(String param) throws WebApplicationException {
        super(param);
    }

    @Override
    protected AnalyticsType parse(String param) {
        try {
            if (param != null) {
                return AnalyticsType.valueOf(param.toUpperCase());
            }
        } catch (IllegalArgumentException iae) {
            log.debug("IllegalArgumentException ignored in AnalyticsTypeParam: {}", iae.getMessage(), iae);
        }

        return null;
    }
}
