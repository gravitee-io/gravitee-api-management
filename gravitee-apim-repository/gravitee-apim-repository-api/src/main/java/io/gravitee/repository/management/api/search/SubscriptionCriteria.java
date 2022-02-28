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
package io.gravitee.repository.management.api.search;

import static java.util.stream.Collectors.toList;

import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionCriteria {

    private final Collection<String> apis;

    private final Collection<String> plans;

    private final Collection<String> statuses;

    private final Collection<String> applications;

    private final long from, to;

    private final long endingAtAfter, endingAtBefore;

    private final String clientId;

    private final Collection<String> planSecurityTypes;

    SubscriptionCriteria(SubscriptionCriteria.Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.apis = builder.apis;
        this.plans = builder.plans;
        this.applications = builder.applications;
        this.statuses = builder.status;
        this.clientId = builder.clientId;
        this.endingAtAfter = builder.endingAtAfter;
        this.endingAtBefore = builder.endingAtBefore;
        this.planSecurityTypes = builder.planSecurityTypes;
    }

    public Collection<String> getApis() {
        return apis;
    }

    public Collection<String> getPlans() {
        return plans;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public Collection<String> getStatuses() {
        return statuses;
    }

    public Collection<String> getApplications() {
        return applications;
    }

    public String getClientId() {
        return clientId;
    }

    public long getEndingAtAfter() {
        return endingAtAfter;
    }

    public long getEndingAtBefore() {
        return endingAtBefore;
    }

    public Collection<String> getPlanSecurityTypes() {
        return planSecurityTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionCriteria that = (SubscriptionCriteria) o;

        if (from != that.from) return false;
        if (to != that.to) return false;
        if (apis != null ? !apis.equals(that.apis) : that.apis != null) return false;
        if (plans != null ? !plans.equals(that.plans) : that.plans != null) return false;
        if (statuses != null ? !statuses.equals(that.statuses) : that.statuses != null) return false;
        if (endingAtAfter != that.endingAtAfter) return false;
        if (endingAtBefore != that.endingAtBefore) return false;
        return applications != null ? applications.equals(that.applications) : that.applications == null;
    }

    @Override
    public int hashCode() {
        int result = apis != null ? apis.hashCode() : 0;
        result = 31 * result + (plans != null ? plans.hashCode() : 0);
        result = 31 * result + (statuses != null ? statuses.hashCode() : 0);
        result = 31 * result + (applications != null ? applications.hashCode() : 0);
        result = 31 * result + (int) (from ^ (from >>> 32));
        result = 31 * result + (int) (to ^ (to >>> 32));
        result = 31 * result + (int) (endingAtAfter ^ (endingAtAfter >>> 32));
        result = 31 * result + (int) (endingAtBefore ^ (endingAtBefore >>> 32));
        result = 31 * result + (planSecurityTypes != null ? planSecurityTypes.hashCode() : 0);
        return result;
    }

    public static class Builder {

        private Collection<String> apis;

        private Collection<String> applications;

        private Collection<String> plans;

        private Collection<String> status;

        private String clientId;

        private long from, to;

        private long endingAtAfter, endingAtBefore;

        private Collection<String> planSecurityTypes;

        public SubscriptionCriteria.Builder from(long from) {
            this.from = from;
            return this;
        }

        public SubscriptionCriteria.Builder to(long to) {
            this.to = to;
            return this;
        }

        public SubscriptionCriteria.Builder apis(Collection<String> apis) {
            this.apis = apis;
            return this;
        }

        public SubscriptionCriteria.Builder applications(Collection<String> applications) {
            this.applications = applications;
            return this;
        }

        public SubscriptionCriteria.Builder plans(Collection<String> plans) {
            this.plans = plans;
            return this;
        }

        public SubscriptionCriteria.Builder status(Subscription.Status status) {
            this.status = Collections.singleton(status.name());
            return this;
        }

        public SubscriptionCriteria.Builder statuses(Collection<Subscription.Status> status) {
            this.status = status.stream().map(Enum::name).collect(toList());
            return this;
        }

        public SubscriptionCriteria.Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public SubscriptionCriteria.Builder endingAtAfter(long endingAtAfter) {
            this.endingAtAfter = endingAtAfter;
            return this;
        }

        public SubscriptionCriteria.Builder endingAtBefore(long endingAtBefore) {
            this.endingAtBefore = endingAtBefore;
            return this;
        }

        public SubscriptionCriteria.Builder planSecurityTypes(Collection<Plan.PlanSecurityType> planSecurityType) {
            this.planSecurityTypes = planSecurityType.stream().map(Enum::name).collect(toList());
            return this;
        }

        public SubscriptionCriteria build() {
            return new SubscriptionCriteria(this);
        }
    }
}
