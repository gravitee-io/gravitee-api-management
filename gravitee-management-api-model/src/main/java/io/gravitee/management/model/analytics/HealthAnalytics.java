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
package io.gravitee.management.model.analytics;

import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HealthAnalytics implements Analytics {

    private long [] timestamps;

    private Map<Boolean, long[]> buckets;

    public Map<Boolean, long[]> getBuckets() {
        return buckets;
    }

    public void setBuckets(Map<Boolean, long[]> buckets) {
        this.buckets = buckets;
    }

    public long[] getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(long[] timestamps) {
        this.timestamps = timestamps;
    }
}
