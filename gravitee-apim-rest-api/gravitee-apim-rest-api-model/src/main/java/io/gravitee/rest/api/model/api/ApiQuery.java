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
package io.gravitee.rest.api.model.api;

import io.gravitee.rest.api.model.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiQuery {

    private Collection<String> ids;
    private String category;
    private List<String> groups;
    private String contextPath;
    private String label;
    private String state;
    private Visibility visibility;
    private String version;
    private String name;
    private String tag;
    private String crossId;
    private List<ApiLifecycleState> lifecycleStates;

    public Collection<String> getIds() {
        return ids;
    }

    public void setIds(Collection<String> ids) {
        this.ids = ids;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<ApiLifecycleState> getLifecycleStates() {
        return lifecycleStates;
    }

    public void setLifecycleStates(List<ApiLifecycleState> lifecycleStates) {
        this.lifecycleStates = lifecycleStates;
    }

    public String getCrossId() {
        return crossId;
    }

    public void setCrossId(String crossId) {
        this.crossId = crossId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiQuery apiQuery = (ApiQuery) o;
        return (
            Objects.equals(ids, apiQuery.ids) &&
            Objects.equals(category, apiQuery.category) &&
            Objects.equals(groups, apiQuery.groups) &&
            Objects.equals(contextPath, apiQuery.contextPath) &&
            Objects.equals(label, apiQuery.label) &&
            Objects.equals(state, apiQuery.state) &&
            visibility == apiQuery.visibility &&
            Objects.equals(version, apiQuery.version) &&
            Objects.equals(name, apiQuery.name) &&
            Objects.equals(tag, apiQuery.tag) &&
            Objects.equals(lifecycleStates, apiQuery.lifecycleStates) &&
            Objects.equals(crossId, apiQuery.crossId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, category, groups, contextPath, label, state, visibility, version, name, tag, lifecycleStates, crossId);
    }

    @Override
    public String toString() {
        return (
            "ApiQuery{" +
            "ids=" +
            ids +
            ", category='" +
            category +
            '\'' +
            ", groups=" +
            groups +
            ", contextPath='" +
            contextPath +
            '\'' +
            ", label='" +
            label +
            '\'' +
            ", state='" +
            state +
            '\'' +
            ", visibility=" +
            visibility +
            ", version='" +
            version +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", tag='" +
            tag +
            '\'' +
            ", lifecycleStates=" +
            lifecycleStates +
            +'\'' +
            ", crossId=" +
            crossId +
            '}'
        );
    }
}
