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

import static java.util.Arrays.asList;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode
public class ApiCriteria {

    private final Collection<String> ids;
    private final Collection<String> groups;
    private final String category;
    private final String label;
    private final LifecycleState state;
    private final Visibility visibility;
    private final String version;
    private final String name;
    private final List<ApiLifecycleState> lifecycleStates;
    private final String environmentId;
    private final List<String> environments;
    private String crossId;
    private List<DefinitionVersion> definitionVersion;
    private String integrationId;
    private String filterName;

    ApiCriteria(ApiCriteria.Builder builder) {
        this.ids = builder.ids;
        this.groups = builder.groups;
        this.category = builder.category;
        this.label = builder.label;
        this.state = builder.state;
        this.visibility = builder.visibility;
        this.version = builder.version;
        this.name = builder.name;
        this.lifecycleStates = builder.lifecycleStates;
        this.environmentId = builder.environmentId;
        this.environments = builder.environments;
        this.crossId = builder.crossId;
        this.definitionVersion = builder.definitionVersion;
        this.integrationId = builder.integrationId;
        this.filterName = builder.filterName;
    }

    public static class Builder {

        private Collection<String> ids;
        private Collection<String> groups;
        private String category;
        private String label;
        private LifecycleState state;
        private Visibility visibility;
        private String version;
        private String name;
        private List<ApiLifecycleState> lifecycleStates;
        private String environmentId;
        private List<String> environments;
        private String crossId;
        private List<DefinitionVersion> definitionVersion;
        private String integrationId;
        private String filterName;

        public ApiCriteria.Builder ids(final String... id) {
            this.ids = Set.of(id);
            return this;
        }

        public ApiCriteria.Builder ids(final Collection<String> ids) {
            this.ids = Set.copyOf(ids);
            return this;
        }

        public ApiCriteria.Builder groups(final String... group) {
            this.groups = asList(group);
            return this;
        }

        public ApiCriteria.Builder groups(final Collection<String> groups) {
            this.groups = groups;
            return this;
        }

        public ApiCriteria.Builder category(final String category) {
            this.category = category;
            return this;
        }

        public ApiCriteria.Builder label(final String label) {
            this.label = label;
            return this;
        }

        public ApiCriteria.Builder state(final LifecycleState state) {
            this.state = state;
            return this;
        }

        public ApiCriteria.Builder visibility(final Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public ApiCriteria.Builder version(final String version) {
            this.version = version;
            return this;
        }

        public ApiCriteria.Builder name(final String name) {
            this.name = name;
            return this;
        }

        public ApiCriteria.Builder lifecycleStates(final List<ApiLifecycleState> lifecycleStates) {
            this.lifecycleStates = lifecycleStates;
            return this;
        }

        public ApiCriteria.Builder environmentId(final String environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public ApiCriteria.Builder environments(final List<String> environments) {
            this.environments = environments;
            return this;
        }

        public ApiCriteria.Builder crossId(final String crossId) {
            this.crossId = crossId;
            return this;
        }

        public ApiCriteria.Builder definitionVersion(final List<DefinitionVersion> definitionVersion) {
            this.definitionVersion = definitionVersion;
            return this;
        }

        public ApiCriteria.Builder integrationId(final String integrationId) {
            this.integrationId = integrationId;
            return this;
        }

        public ApiCriteria.Builder filterName(final String filterName) {
            this.filterName = filterName;
            return this;
        }

        public ApiCriteria build() {
            return new ApiCriteria(this);
        }
    }
}
