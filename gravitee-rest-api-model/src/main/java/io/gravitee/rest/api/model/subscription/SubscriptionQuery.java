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
package io.gravitee.rest.api.model.subscription;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import io.gravitee.rest.api.model.SubscriptionStatus;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionQuery {

    private Collection<String> apis;

    private Collection<String> plans;

    private Collection<SubscriptionStatus> statuses;

    private Collection<String> applications;

    private long from, to;

    public Collection<String> getApis() {
        return apis;
    }

    public void setApis(Collection<String> apis) {
        this.apis = apis;
    }

    public void setApi(String api) {
        this.apis = Collections.singleton(api);
    }

    public Collection<String> getPlans() {
        return plans;
    }

    public void setPlans(Collection<String> plans) {
        this.plans = plans;
    }

    public void setPlan(String plan) {
        this.plans = Collections.singleton(plan);
    }

    public Collection<SubscriptionStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(Collection<SubscriptionStatus> statuses) {
        this.statuses = statuses;
    }

    public Collection<String> getApplications() {
        return applications;
    }

    public void setApplications(Collection<String> applications) {
        this.applications = applications;
    }

    public void setApplication(String application) {
        this.applications = Collections.singleton(application);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionQuery)) return false;
        SubscriptionQuery that = (SubscriptionQuery) o;
        return from == that.from &&
                to == that.to &&
                Objects.equals(apis, that.apis) &&
                Objects.equals(plans, that.plans) &&
                Objects.equals(statuses, that.statuses) &&
                Objects.equals(applications, that.applications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apis, plans, statuses, applications, from, to);
    }
}
