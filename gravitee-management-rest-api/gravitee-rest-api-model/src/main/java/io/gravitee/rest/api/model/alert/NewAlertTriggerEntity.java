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
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.rest.api.model.AlertEventRuleEntity;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewAlertTriggerEntity extends Trigger {

    @NotNull
    private String name;

    private String description;

    private Severity severity;

    @JsonProperty("reference_type")
    private AlertReferenceType referenceType;

    @JsonProperty("reference_id")
    private String referenceId;

    @NotNull
    private String type;

    private boolean template;

    @JsonProperty("event_rules")
    private List<AlertEventRuleEntity> eventRules;

    protected NewAlertTriggerEntity() {
        super(null, null, Severity.INFO, null, false);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(boolean template) {
        this.template = template;
    }

    public List<AlertEventRuleEntity> getEventRules() {
        return eventRules;
    }

    public void setEventRules(List<AlertEventRuleEntity> eventRules) {
        this.eventRules = eventRules;
    }
}
