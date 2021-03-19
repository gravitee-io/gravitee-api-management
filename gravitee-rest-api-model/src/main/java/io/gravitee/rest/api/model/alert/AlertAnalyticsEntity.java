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
package io.gravitee.rest.api.model.alert;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AlertAnalyticsEntity {

    private Map<String, Integer> bySeverity;

    private List<AlertAnalyticsEntity.AlertTriggerAnalytics> alerts;

    public AlertAnalyticsEntity(Map<String, Integer> bySeverity, List<AlertAnalyticsEntity.AlertTriggerAnalytics> alerts) {
        this.bySeverity = bySeverity;
        this.alerts = alerts;
    }

    public Map<String, Integer> getBySeverity() {
        return bySeverity;
    }

    public void setBySeverity(Map<String, Integer> bySeverity) {
        this.bySeverity = bySeverity;
    }

    public List<AlertTriggerAnalytics> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertTriggerAnalytics> alerts) {
        this.alerts = alerts;
    }

    public static class AlertTriggerAnalytics {
        private String id;
        private String severity;
        private String name;
        private String description;
        private String type;

        @JsonProperty("events_count")
        private Integer eventsCount;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getEventsCount() {
            return eventsCount;
        }

        public void setEventsCount(Integer eventsCount) {
            this.eventsCount = eventsCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AlertTriggerAnalytics that = (AlertTriggerAnalytics) o;
            return Objects.equals(id, that.id) && Objects.equals(severity, that.severity) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(type, that.type) && Objects.equals(eventsCount, that.eventsCount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, severity, name, description, type, eventsCount);
        }
    }
}
