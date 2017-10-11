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

import java.util.Collection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyCriteria {

    private final Collection<String> plans;

    private final long from, to;

    private final boolean includeRevoked;

    ApiKeyCriteria(ApiKeyCriteria.Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.plans = builder.plan;
        this.includeRevoked = builder.includeRevoked;
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

    public boolean isIncludeRevoked() {
        return includeRevoked;
    }

    public static class Builder {
        private Collection<String> plan;

        private long from, to;

        private boolean includeRevoked;

        public ApiKeyCriteria.Builder from(long from) {
            this.from = from;
            return this;
        }

        public ApiKeyCriteria.Builder to(long to) {
            this.to = to;
            return this;
        }

        public ApiKeyCriteria.Builder plans(Collection<String> plans) {
            this.plan = plans;
            return this;
        }

        public ApiKeyCriteria.Builder includeRevoked(boolean include) {
            this.includeRevoked = include;
            return this;
        }

        public ApiKeyCriteria build() {
            return new ApiKeyCriteria(this);
        }
    }
}
