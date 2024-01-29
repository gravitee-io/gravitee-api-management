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
package io.gravitee.definition.model.federation;

import java.util.Set;

public class FederatedApiBuilder {

    private String apiId;
    private String name = "an-api";
    private String apiVersion = "1.0.0";

    private String accessPoint;

    private Set<String> tags;

    public static FederatedApiBuilder aFederatedApi() {
        return new FederatedApiBuilder();
    }

    public FederatedApiBuilder id(String id) {
        this.apiId = id;
        return this;
    }

    public FederatedApiBuilder name(String name) {
        this.name = name;
        return this;
    }

    public FederatedApiBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public FederatedApiBuilder accessPoint(String accessPoint) {
        this.accessPoint = accessPoint;
        return this;
    }

    public FederatedApiBuilder tags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public FederatedApi build() {
        var api = new FederatedApi();
        api.setId(apiId);
        api.setName(name);
        api.setApiVersion(apiVersion);
        api.setTags(tags);
        api.setAccessPoint(accessPoint);

        return api;
    }
}
