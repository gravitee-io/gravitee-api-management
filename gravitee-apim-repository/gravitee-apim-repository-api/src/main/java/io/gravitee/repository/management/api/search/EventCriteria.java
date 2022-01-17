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

import io.gravitee.repository.management.model.EventType;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventCriteria {

    private Collection<EventType> types;

    private Map<String, Object> properties;

    private long from, to;

    private String environmentId;

    private boolean strictMode;

    EventCriteria(EventCriteria.Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.types = new HashSet<>(builder.types);
        this.properties = new HashMap<>(builder.properties);
        this.environmentId = builder.environmentId;
        this.strictMode = builder.strictMode;
    }

    public Collection<EventType> getTypes() {
        return types;
    }

    public void setTypes(Collection<EventType> types) {
        this.types = types;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
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

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventCriteria that = (EventCriteria) o;

        if (from != that.from) return false;
        if (to != that.to) return false;
        if (types != null ? !types.equals(that.types) : that.types != null) return false;
        if (environmentId != null ? !environmentId.equals(that.environmentId) : that.environmentId != null) return false;
        if (strictMode != that.strictMode) return false;
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, properties, environmentId, from, to, strictMode);
    }

    public static class Builder {

        private Map<String, Object> properties = new HashMap<>();

        private Collection<EventType> types = new ArrayList<>();

        private long from;

        private long to;

        private String environmentId;

        private boolean strictMode;

        public Builder from(long from) {
            this.from = from;
            return this;
        }

        public Builder to(long to) {
            this.to = to;
            return this;
        }

        public Builder types(EventType... types) {
            for (int i = 0; i < types.length; i++) {
                this.types.add(types[i]);
            }

            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);

            return this;
        }

        public Builder environmentId(String environmentId) {
            this.environmentId = environmentId;

            return this;
        }

        public Builder strictMode(boolean strictMode) {
            this.strictMode = strictMode;

            return this;
        }

        public EventCriteria build() {
            return new EventCriteria(this);
        }
    }
}
