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
package io.gravitee.repository.healthcheck.query.responsetime;

import io.gravitee.repository.healthcheck.query.FieldBucket;
import io.gravitee.repository.healthcheck.query.Response;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AverageResponseTimeResponse implements Response {

    private List<FieldBucket<Long>> endpointResponseTimes;

    public List<FieldBucket<Long>> getEndpointResponseTimes() {
        return endpointResponseTimes;
    }

    public void setEndpointResponseTimes(List<FieldBucket<Long>> endpointResponseTimes) {
        this.endpointResponseTimes = endpointResponseTimes;
    }
}
