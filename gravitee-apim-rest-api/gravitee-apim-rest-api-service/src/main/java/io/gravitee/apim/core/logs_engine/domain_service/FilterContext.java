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
    private Set<String> mcpMethods;
    private Set<String> transactionIds;
    private Set<String> requestIds;
    private String uri;
    private Long responseTimeFrom;
    private Long responseTimeTo;
    private Set<String> errorKeys;

    private <T> Set<T> limitBy(Set<T> current, Set<T> incoming) {
        if (incoming == null) {
            return current;
        }
        if (current == null) {
            return new HashSet<>(incoming);
        }
        current.retainAll(incoming);
        return current;
    }

    public void limitByApiIds(Set<String> apiIds) {
        this.apiIds = limitBy(this.apiIds, apiIds);
    }

    public void limitByApplicationIds(Set<String> applicationIds) {
        this.applicationIds = limitBy(this.applicationIds, applicationIds);
    }

    public void limitByPlanIds(Set<String> planIds) {
        this.planIds = limitBy(this.planIds, planIds);
    }

    public void limitByHttpMethods(Set<HttpMethod> methods) {
        this.methods = limitBy(this.methods, methods);
    }

    public void limitByHttpStatuses(Set<Integer> statuses) {
        this.statuses = limitBy(this.statuses, statuses);
    }

    public void limitByEntrypointIds(Set<String> entrypointIds) {
        this.entrypointIds = limitBy(this.entrypointIds, entrypointIds);
    }

    public void limitByMcpMethods(Set<String> mcpMethods) {
        this.mcpMethods = limitBy(this.mcpMethods, mcpMethods);
    }

    public void limitByTransactionIds(Set<String> transactionIds) {
        this.transactionIds = limitBy(this.transactionIds, transactionIds);
    }

    public void limitByRequestIds(Set<String> requestIds) {
        this.requestIds = limitBy(this.requestIds, requestIds);
    }

    public void limitByErrorKeys(Set<String> errorKeys) {
        this.errorKeys = limitBy(this.errorKeys, errorKeys);
    }

    public void limitByUri(String uri) {
        if (uri == null) {
            return;
        }
        this.uri = uri;
    }

    public void limitByResponseTimeFrom(Long responseTimeFrom) {
        if (responseTimeFrom == null) {
            return;
        }
        this.responseTimeFrom = responseTimeFrom;
    }

    public void limitByResponseTimeTo(Long responseTimeTo) {
        if (responseTimeTo == null) {
            return;
        }
        this.responseTimeTo = responseTimeTo;
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

    public Optional<Set<String>> mcpMethods() {
        return Optional.ofNullable(mcpMethods);
    }

    public Optional<Set<String>> transactionIds() {
        return Optional.ofNullable(transactionIds);
    }

    public Optional<Set<String>> requestIds() {
        return Optional.ofNullable(requestIds);
    }

    public Optional<String> uri() {
        return Optional.ofNullable(uri);
    }

    public Optional<Long> responseTimeFrom() {
        return Optional.ofNullable(responseTimeFrom);
    }

    public Optional<Long> responseTimeTo() {
        return Optional.ofNullable(responseTimeTo);
    }

    public Optional<Set<String>> errorKeys() {
        return Optional.ofNullable(errorKeys);
    }
}
