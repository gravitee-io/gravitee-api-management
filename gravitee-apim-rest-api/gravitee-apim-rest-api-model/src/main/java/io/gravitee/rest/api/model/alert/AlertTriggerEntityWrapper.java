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

import io.gravitee.alert.api.condition.Condition;
import io.gravitee.alert.api.condition.Filter;
import io.gravitee.alert.api.trigger.Dampening;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.notifier.api.Notification;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertTriggerEntityWrapper extends AlertTriggerEntity {

    private final Trigger trigger;

    public AlertTriggerEntityWrapper(Trigger trigger) {
        super(trigger.getId(), trigger.getName(), trigger.getSource(), trigger.getSeverity(), trigger.isEnabled());
        this.trigger = trigger;
    }

    @Override
    public String getId() {
        return trigger.getId();
    }

    @Override
    public void setId(String id) {
        trigger.setId(id);
    }

    @Override
    public String getName() {
        return trigger.getName();
    }

    @Override
    public void setName(String name) {
        trigger.setName(name);
    }

    @Override
    public void setSource(String source) {
        trigger.setSource(source);
    }

    @Override
    public String getSource() {
        return trigger.getSource();
    }

    @Override
    public List<Condition> getConditions() {
        return trigger.getConditions();
    }

    @Override
    public void setConditions(List<Condition> conditions) {
        trigger.setConditions(conditions);
    }

    @Override
    public Map<String, Map<String, String>> getMetadata() {
        return trigger.getMetadata();
    }

    @Override
    public void setMetadata(Map<String, Map<String, String>> metadata) {
        trigger.setMetadata(metadata);
    }

    @Override
    public List<Notification> getNotifications() {
        return trigger.getNotifications();
    }

    @Override
    public void setNotifications(List<Notification> notifications) {
        trigger.setNotifications(notifications);
    }

    @Override
    public boolean isEnabled() {
        return trigger.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        trigger.setEnabled(enabled);
    }

    @Override
    public Dampening getDampening() {
        return trigger.getDampening();
    }

    @Override
    public void setDampening(Dampening dampening) {
        trigger.setDampening(dampening);
    }

    @Override
    public List<Filter> getFilters() {
        return trigger.getFilters();
    }

    @Override
    public void setFilters(List<Filter> filters) {
        trigger.setFilters(filters);
    }

    @Override
    public Trigger.Severity getSeverity() {
        return trigger.getSeverity();
    }

    @Override
    public void setSeverity(Trigger.Severity severity) {
        trigger.setSeverity(severity);
    }

    @Override
    public String getDescription() {
        return trigger.getDescription();
    }

    @Override
    public void setDescription(String description) {
        trigger.setDescription(description);
    }

    @Override
    public boolean equals(Object o) {
        return trigger.equals(o);
    }

    @Override
    public int hashCode() {
        return trigger.hashCode();
    }

    @Override
    public String toString() {
        return trigger.toString();
    }
}
