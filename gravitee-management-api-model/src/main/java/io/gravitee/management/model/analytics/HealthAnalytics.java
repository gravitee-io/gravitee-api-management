package io.gravitee.management.model.analytics;

import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HealthAnalytics {

    private long [] timestamps;

    private Map<Integer, long[]> buckets;

    public Map<Integer, long[]> getBuckets() {
        return buckets;
    }

    public void setBuckets(Map<Integer, long[]> buckets) {
        this.buckets = buckets;
    }

    public long[] getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(long[] timestamps) {
        this.timestamps = timestamps;
    }
}
