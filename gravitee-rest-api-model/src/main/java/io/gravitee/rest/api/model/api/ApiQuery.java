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

import java.util.List;

import io.gravitee.rest.api.model.Visibility;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiQuery {

    private List<String> ids;
    private String view;
    private List<String> groups;
    private String contextPath;
    private String label;
    private String state;
    private Visibility visibility;
    private String version;
    private String name;
    private String tag;
    private List<ApiLifecycleState> lifecycleStates;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
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

    @Override
    public String toString() {
        return "ApiQuery{" +
                "ids=" + ids +
                ", view='" + view + '\'' +
                ", groups=" + groups +
                ", contextPath='" + contextPath + '\'' +
                ", label='" + label + '\'' +
                ", state='" + state + '\'' +
                ", visibility=" + visibility +
                ", version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", lifecycleStates=" + lifecycleStates +
                '}';
    }
}
