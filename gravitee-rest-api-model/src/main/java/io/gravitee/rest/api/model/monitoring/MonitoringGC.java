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
package io.gravitee.rest.api.model.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author GraviteeSource Team
 */
public class MonitoringGC {

    @JsonProperty("young_collection_count")
    private int youngCollectionCount;

    @JsonProperty("young_collection_time_in_millis")
    private long youngCollectionTimeInMillis;

    @JsonProperty("old_collection_count")
    private int oldCollectionCount;

    @JsonProperty("old_collection_time_in_millis")
    private long oldCollectionTimeInMillis;

    public int getYoungCollectionCount() {
        return youngCollectionCount;
    }

    public void setYoungCollectionCount(int youngCollectionCount) {
        this.youngCollectionCount = youngCollectionCount;
    }

    public long getYoungCollectionTimeInMillis() {
        return youngCollectionTimeInMillis;
    }

    public void setYoungCollectionTimeInMillis(long youngCollectionTimeInMillis) {
        this.youngCollectionTimeInMillis = youngCollectionTimeInMillis;
    }

    public int getOldCollectionCount() {
        return oldCollectionCount;
    }

    public void setOldCollectionCount(int oldCollectionCount) {
        this.oldCollectionCount = oldCollectionCount;
    }

    public long getOldCollectionTimeInMillis() {
        return oldCollectionTimeInMillis;
    }

    public void setOldCollectionTimeInMillis(long oldCollectionTimeInMillis) {
        this.oldCollectionTimeInMillis = oldCollectionTimeInMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringGC)) return false;
        MonitoringGC that = (MonitoringGC) o;
        return (
            youngCollectionCount == that.youngCollectionCount &&
            youngCollectionTimeInMillis == that.youngCollectionTimeInMillis &&
            oldCollectionCount == that.oldCollectionCount &&
            oldCollectionTimeInMillis == that.oldCollectionTimeInMillis
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(youngCollectionCount, youngCollectionTimeInMillis, oldCollectionCount, oldCollectionTimeInMillis);
    }

    @Override
    public String toString() {
        return (
            "MonitoringGC{" +
            "youngCollectionCount=" +
            youngCollectionCount +
            ", youngCollectionTimeInMillis=" +
            youngCollectionTimeInMillis +
            ", oldCollectionCount=" +
            oldCollectionCount +
            ", oldCollectionTimeInMillis=" +
            oldCollectionTimeInMillis +
            '}'
        );
    }
}
