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
package io.gravitee.management.model.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.management.model.AlertEventRuleEntity;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpdateAlertTriggerEntity extends Trigger {

    private String id;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private Severity severity;

    @JsonProperty("reference_type")
    private AlertReferenceType referenceType;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("event_rules")
    private List<AlertEventRuleEntity> eventRules;

    protected UpdateAlertTriggerEntity() {
        super(null, null, Severity.INFO, null, false);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public AlertReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(AlertReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public List<AlertEventRuleEntity> getEventRules() {
        return eventRules;
    }

    public void setEventRules(List<AlertEventRuleEntity> eventRules) {
        this.eventRules = eventRules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateAlertTriggerEntity)) return false;
        UpdateAlertTriggerEntity that = (UpdateAlertTriggerEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UpdateAlertEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", referenceType=" + referenceType +
                ", referenceId='" + referenceId + '\'' +
                '}';
    }
}
