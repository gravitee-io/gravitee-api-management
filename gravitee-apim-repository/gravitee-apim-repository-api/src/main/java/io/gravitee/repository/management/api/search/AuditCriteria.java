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

import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.EventType;
import java.util.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuditCriteria {

    private Map<Audit.AuditReferenceType, List<String>> references;

    private Map<String, String> properties;

    private List<String> events;

    private long from, to;

    private List<String> environmentIds;

    private String organizationId;

    AuditCriteria(AuditCriteria.Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.references = new HashMap<>(builder.references);
        this.properties = new HashMap<>(builder.properties);
        if (builder.events != null) {
            this.events = builder.events;
        }
        this.environmentIds = builder.environmentIds;
        this.organizationId = builder.organizationId;
    }

    public Map<Audit.AuditReferenceType, List<String>> getReferences() {
        return references;
    }

    public void setReferences(Map<Audit.AuditReferenceType, List<String>> references) {
        this.references = references;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
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

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public List<String> getEnvironmentIds() {
        return environmentIds;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuditCriteria that = (AuditCriteria) o;

        if (from != that.from) return false;
        if (to != that.to) return false;
        if (references != null ? !references.equals(that.references) : that.references != null) return false;
        if (environmentIds != null ? !environmentIds.equals(that.environmentIds) : that.environmentIds != null) return false;
        if (organizationId != that.organizationId) return false;
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        int result = references != null ? references.hashCode() : 0;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (int) (from ^ (from >>> 32));
        result = 31 * result + (int) (to ^ (to >>> 32));
        return result;
    }

    public static class Builder {

        private Map<String, String> properties = new HashMap<>();

        private Map<Audit.AuditReferenceType, List<String>> references = new HashMap<>();

        private long from;

        private long to;

        private List<String> events;

        private List<String> environmentIds;

        private String organizationId;

        public Builder from(long from) {
            this.from = from;
            return this;
        }

        public Builder to(long to) {
            this.to = to;
            return this;
        }

        public Builder references(Audit.AuditReferenceType referenceType, List<String> referenceIds) {
            this.references.put(referenceType, referenceIds);
            return this;
        }

        public Builder property(String key, String value) {
            this.properties.put(key, value);

            return this;
        }

        public Builder events(List<String> events) {
            this.events = events;
            return this;
        }

        public AuditCriteria build() {
            return new AuditCriteria(this);
        }

        public AuditCriteria.Builder environmentIds(final List<String> environmentIds) {
            this.environmentIds = environmentIds;
            return this;
        }

        public AuditCriteria.Builder organizationId(final String organizationId) {
            this.organizationId = organizationId;
            return this;
        }
    }
}
