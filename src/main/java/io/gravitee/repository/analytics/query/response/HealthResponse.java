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
package io.gravitee.repository.analytics.query.response;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthResponse implements Response {

    private long [] timestamps;

    private Map<Boolean, long[]> buckets;

    public long [] timestamps() {
        return timestamps;
    }

    public void timestamps(long[] timestamps) {
        this.timestamps = timestamps;
    }

    public Map<Boolean, long[]> buckets() {
        return buckets;
    }

    public void buckets(Map<Boolean, long[]> buckets) {
        this.buckets = buckets;
    }
}
