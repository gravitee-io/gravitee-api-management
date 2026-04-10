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
package io.gravitee.repository.management.api.search;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Criteria to search API Products (e.g. by environment(s), ids, name, version), aligned with ApiCriteria usage.
 * Additional fields are available for future use in search/filter implementations.
 *
 * @author GraviteeSource Team
 */
public class ApiProductCriteria {

    private Collection<String> ids;
    private String name;
    private String version;
    private String environmentId;
    private List<String> environments;
    private Collection<String> apiIds;
    private Collection<String> groups;

    ApiProductCriteria(ApiProductCriteria.Builder builder) {
        this.ids = builder.ids;
        this.name = builder.name;
        this.version = builder.version;
        this.environmentId = builder.environmentId;
        this.environments = builder.environments;
        this.apiIds = builder.apiIds;
        this.groups = builder.groups;
    }

    public Collection<String> getIds() {
        return ids;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public Collection<String> getApiIds() {
        return apiIds;
    }

    public Collection<String> getGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiProductCriteria that = (ApiProductCriteria) o;
        return (
            Objects.equals(ids, that.ids) &&
            Objects.equals(name, that.name) &&
            Objects.equals(version, that.version) &&
            Objects.equals(environmentId, that.environmentId) &&
            Objects.equals(environments, that.environments) &&
            Objects.equals(apiIds, that.apiIds) &&
            Objects.equals(groups, that.groups)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids, name, version, environmentId, environments, apiIds, groups);
    }

    public static class Builder {

        private Collection<String> ids;
        private String name;
        private String version;
        private String environmentId;
        private List<String> environments;
        private Collection<String> apiIds;
        private Collection<String> groups;

        public ApiProductCriteria.Builder ids(String... id) {
            this.ids = id != null && id.length > 0 ? List.of(id) : null;
            return this;
        }

        public ApiProductCriteria.Builder ids(Collection<String> ids) {
            this.ids = ids;
            return this;
        }

        public ApiProductCriteria.Builder name(String name) {
            this.name = name;
            return this;
        }

        public ApiProductCriteria.Builder version(String version) {
            this.version = version;
            return this;
        }

        public ApiProductCriteria.Builder environmentId(String environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public ApiProductCriteria.Builder environments(List<String> environments) {
            this.environments = environments;
            return this;
        }

        public ApiProductCriteria.Builder apiIds(Collection<String> apiIds) {
            this.apiIds = apiIds;
            return this;
        }

        public ApiProductCriteria.Builder groups(Collection<String> groups) {
            this.groups = groups;
            return this;
        }

        public ApiProductCriteria build() {
            return new ApiProductCriteria(this);
        }
    }
}
