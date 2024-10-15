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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class Api extends AbstractApi {

    @JsonProperty(required = true)
    @NotEmpty
    private List<@NotNull Listener> listeners;

    @JsonProperty(required = true)
    @NotEmpty
    private List<EndpointGroup> endpointGroups;

    private Analytics analytics;

    private Failover failover;

    @JsonProperty(required = true)
    @NotNull
    private Map<@NotEmpty String, @NotNull Plan> plans;

    @Builder.Default
    private FlowExecution flowExecution = new FlowExecution();

    private List<Flow> flows;

    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    private ApiServices services;

    public Plan getPlan(final String plan) {
        return plans.get(plan);
    }

    @Nullable
    public List<Plan> getPlans() {
        if (plans != null) {
            return new ArrayList<>(this.plans.values());
        }
        return null;
    }

    public void setPlans(List<Plan> plans) {
        if (plans != null) {
            this.plans = plans.stream().collect(Collectors.toMap(Plan::getId, Function.identity()));
        } else {
            this.plans = new HashMap<>();
        }
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream
            .of(
                Optional
                    .ofNullable(this.getResources())
                    .map(r -> r.stream().filter(Resource::isEnabled).map(Resource::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getFlows())
                    .map(f -> f.stream().filter(Flow::isEnabled).map(Flow::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getPlans())
                    .map(p -> p.stream().map(Plan::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getListeners())
                    .map(l -> l.stream().map(Listener::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getEndpointGroups())
                    .map(r -> r.stream().map(EndpointGroup::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional.ofNullable(this.getServices()).map(ApiServices::getPlugins).orElse(List.of())
            )
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public boolean failoverEnabled() {
        return failover != null && failover.isEnabled();
    }

    @JsonIgnore
    public boolean isTcpProxy() {
        return ApiType.PROXY.equals(getType()) && listeners.stream().anyMatch(TcpListener.class::isInstance);
    }
}
