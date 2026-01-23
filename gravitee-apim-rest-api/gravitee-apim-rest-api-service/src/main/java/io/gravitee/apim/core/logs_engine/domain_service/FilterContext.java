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
package io.gravitee.apim.core.logs_engine.domain_service;

import io.gravitee.apim.core.logs_engine.model.HttpMethod;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class FilterContext {

    private Set<String> apiIds;
    private Set<String> applicationIds;
    private Set<String> planIds;
    private Set<HttpMethod> methods;
    private Set<Integer> statuses;
    private Set<String> entrypointIds;

    public void limitByApiIds(Set<String> apiIds) {
        if (apiIds == null) {
            return;
        }
        if (this.apiIds == null) {
            this.apiIds = new HashSet<>(apiIds);
        } else {
            this.apiIds.retainAll(apiIds);
        }
    }

    public void limitByApplicationIds(Set<String> applicationIds) {
        if (applicationIds == null) {
            return;
        }
        if (this.applicationIds == null) {
            this.applicationIds = new HashSet<>(applicationIds);
        } else {
            this.applicationIds.retainAll(applicationIds);
        }
    }

    public void limitByPlanIds(Set<String> planIds) {
        if (planIds == null) {
            return;
        }
        if (this.planIds == null) {
            this.planIds = new HashSet<>(planIds);
        } else {
            this.planIds.retainAll(planIds);
        }
    }

    public void limitByHttpMethods(Set<HttpMethod> methods) {
        if (methods == null) {
            return;
        }
        if (this.methods == null) {
            this.methods = new HashSet<>(methods);
        } else {
            this.methods.retainAll(methods);
        }
    }

    public void limitByHttpStatuses(Set<Integer> statuses) {
        if (statuses == null) {
            return;
        }
        if (this.statuses == null) {
            this.statuses = new HashSet<>(statuses);
        } else {
            this.statuses.retainAll(statuses);
        }
    }

    public void limitByEntrypointIds(Set<String> entrypointIds) {
        if (entrypointIds == null) {
            return;
        }
        if (this.entrypointIds == null) {
            this.entrypointIds = new HashSet<>(entrypointIds);
        } else {
            this.entrypointIds.retainAll(entrypointIds);
        }
    }

    public Optional<Set<String>> apiIds() {
        return Optional.ofNullable(apiIds);
    }

    public Optional<Set<String>> applicationIds() {
        return Optional.ofNullable(applicationIds);
    }

    public Optional<Set<String>> planIds() {
        return Optional.ofNullable(planIds);
    }

    public Optional<Set<HttpMethod>> methods() {
        return Optional.ofNullable(methods);
    }

    public Optional<Set<Integer>> statuses() {
        return Optional.ofNullable(statuses);
    }

    public Optional<Set<String>> entrypointIds() {
        return Optional.ofNullable(entrypointIds);
    }
}
