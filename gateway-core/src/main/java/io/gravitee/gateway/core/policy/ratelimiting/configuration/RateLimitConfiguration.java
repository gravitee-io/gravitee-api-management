package io.gravitee.gateway.core.policy.ratelimiting.configuration;

import io.gravitee.gateway.api.PolicyConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RateLimitConfiguration implements PolicyConfiguration {

    private TimeUnit timeUnit;

    private int requests;

    private int time;

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
