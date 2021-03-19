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
package io.gravitee.rest.api.model;

import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertAnalyticsQuery {

    private final long from;
    private final long to;

    private AlertAnalyticsQuery(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public static class Builder {
        private long from, to;

        public AlertAnalyticsQuery build() {
            return new AlertAnalyticsQuery(this);
        }

        public Builder from(long from) {
            this.from = from;
            return this;
        }

        public Builder to(long to) {
            this.to = to;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertAnalyticsQuery that = (AlertAnalyticsQuery) o;
        return from == that.from &&
                to == that.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return "AlertEventQuery{" +
                ", from=" + from +
                ", to=" + to +
                '}';
    }
}
