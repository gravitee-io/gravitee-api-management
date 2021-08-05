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
package io.gravitee.rest.api.model.key;

import java.util.Collection;
import java.util.Objects;

public class ApiKeyQuery {

    private Collection<String> plans;

    private long from, to;

    private boolean includeRevoked;

    private long expireAfter, expireBefore;

    public Collection<String> getPlans() {
        return plans;
    }

    public void setPlans(Collection<String> plans) {
        this.plans = plans;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public boolean isIncludeRevoked() {
        return includeRevoked;
    }

    public void setIncludeRevoked(boolean includeRevoked) {
        this.includeRevoked = includeRevoked;
    }

    public long getExpireAfter() {
        return expireAfter;
    }

    public void setExpireAfter(long expireAfter) {
        this.expireAfter = expireAfter;
    }

    public long getExpireBefore() {
        return expireBefore;
    }

    public void setExpireBefore(long expireBefore) {
        this.expireBefore = expireBefore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyQuery that = (ApiKeyQuery) o;
        return (
            from == that.from &&
            to == that.to &&
            includeRevoked == that.includeRevoked &&
            expireAfter == that.expireAfter &&
            expireBefore == that.expireBefore &&
            Objects.equals(plans, that.plans)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(plans, from, to, includeRevoked, expireAfter, expireBefore);
    }
}
