/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model.v4;

import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.property.Property;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiBuilder {

    private String apiId;
    private String name = "an-api";
    private String apiVersion = "1.0.0";

    private List<Listener> listeners;
    private List<EndpointGroup> endpointGroups;

    private Map<String, String> properties;
    private Set<String> tags;
    private List<Plan> plans;

    public ApiBuilder() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public ApiBuilder(List<Listener> listeners, List<EndpointGroup> endpointGroups) {
        this.listeners = listeners;
        this.endpointGroups = endpointGroups;
    }

    public static ApiBuilder anApiV4() {
        return aSyncApiV4();
    }

    public static ApiBuilder aSyncApiV4() {
        var httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path()));

        return new ApiBuilder(List.of(httpListener), List.of(EndpointGroup.builder().build()));
    }

    public ApiBuilder id(String id) {
        this.apiId = id;
        return this;
    }

    public ApiBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ApiBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public ApiBuilder properties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public ApiBuilder tags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public ApiBuilder plans(List<Plan> plans) {
        this.plans = plans;
        return this;
    }

    public ApiBuilder listeners(List<Listener> listeners) {
        this.listeners = listeners;
        return this;
    }

    public ApiBuilder endpointGroups(List<EndpointGroup> endpointGroups) {
        this.endpointGroups = endpointGroups;
        return this;
    }

    public Api build() {
        var api = new Api();
        api.setId(apiId);
        api.setName(name);
        api.setApiVersion(apiVersion);
        api.setListeners(listeners);
        api.setEndpointGroups(endpointGroups);
        api.setTags(tags);

        if (plans != null) {
            api.setPlans(plans);
        }

        if (properties != null) {
            api.setProperties(
                properties.entrySet().stream().map(p -> new Property(p.getKey(), p.getValue(), false, false)).collect(Collectors.toList())
            );
        }

        return api;
    }
}
