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
package io.gravitee.management.model;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertEventQuery {

    private final long from;
    private final long to;

    private final int pageSize;
    private final int pageNumber;

    private AlertEventQuery(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.pageSize = builder.pageSize;
        this.pageNumber = builder.pageNumber;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public static class Builder {
        private Map<String, Object> properties;
        private Collection<EventType> types;
        private long from, to;
        private int pageSize, pageNumber;

        public AlertEventQuery build() {
            return new AlertEventQuery(this);
        }

        public Builder property(String key, Object value) {
            if (properties == null) {
                properties = new HashMap<>();
            }

            properties.put(key, value);
            return this;
        }

        public Builder type(EventType type) {
            this.types = Collections.singletonList(type);
            return this;
        }

        public Builder type(EventType ... types) {
            this.types = Arrays.asList(types);
            return this;
        }

        public Builder from(long from) {
            this.from = from;
            return this;
        }

        public Builder to(long to) {
            this.to = to;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertEventQuery that = (AlertEventQuery) o;
        return from == that.from &&
                to == that.to &&
                pageSize == that.pageSize &&
                pageNumber == that.pageNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, pageSize, pageNumber);
    }

    @Override
    public String toString() {
        return "AlertEventQuery{" +
                ", from=" + from +
                ", to=" + to +
                ", pageSize=" + pageSize +
                ", pageNumber=" + pageNumber +
                '}';
    }
}
