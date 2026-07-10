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
package io.gravitee.definition.model.v4.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.agent.definition.AgentChannel;
import io.gravitee.definition.model.v4.agent.workflow.Workflow;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.resource.Resource;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * A first-class agent definition — a sibling of {@code Api}/{@code NativeApi} under {@link AbstractApi}, selected
 * by {@code type: agent}. Unlike the proxy {@code Api}, it models no endpointGroups/flows: an agent is
 * <b>exposure</b> ({@code listeners} — HTTP entrypoints) plus {@code plans}, a {@code composable} flag (whether it can
 * be referenced as a workflow sub-agent), a {@code kind} ({@code standalone}|{@code workflow}) and the matching body:
 * <ul>
 *   <li>{@code standalone} → {@link StandaloneAgentDefinition} {@code standalone} (a single task agent);</li>
 *   <li>{@code workflow} → {@link Workflow} {@code workflow} (an orchestration whose agent leaves are
 *       <b>references</b> by id to independently-deployed agents — never embedded).</li>
 * </ul>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class AgentApi extends AbstractApi {

    @Serial
    private static final long serialVersionUID = -505816775318303114L;

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    private ApiType type = ApiType.AGENT;

    /** How the agent is exposed — HTTP listeners carrying agent entrypoints. */
    private List<@NotNull Listener> listeners;

    /** {@code standalone} (a single agent) | {@code workflow} (an orchestration over referenced agents). */
    @JsonProperty(required = true)
    @NotNull
    private String kind;

    /** The single-agent body — present when {@code kind=standalone}. */
    private StandaloneAgentDefinition standalone;

    /** The orchestration root (a control) — present when {@code kind=workflow}. Type-locked to {@link Workflow}. */
    private Workflow workflow;

    /** Exposure plans (e.g. a keyless plan) — reuses the Gravitee {@link Plan}. */
    private List<Plan> plans;

    /**
     * When {@code true}, the agent is <b>composition-only capable</b>: it is indexed so a workflow can reference it by
     * id as a sub-agent. Independent of HTTP exposure (which is driven by a published plan) — an agent can be both
     * HTTP-served and composable, or composition-only (composable with no published plan → not served over HTTP).
     */
    private boolean composable;

    private AgentAnalytics analytics;

    protected List<AgentChannel> channels;

    @Override
    @JsonIgnore
    public List<Plugin> getPlugins() {
        List<Plugin> bodyPlugins = standalone != null
            ? standalone.collectPlugins()
            : (workflow != null ? workflow.collectPlugins() : List.of());
        return Stream.of(
            Optional.ofNullable(this.getResources())
                .map(r -> r.stream().filter(Resource::isEnabled).map(Resource::getPlugins).flatMap(List::stream).toList())
                .orElse(List.of()),
            Optional.ofNullable(this.getListeners())
                .map(l -> l.stream().map(Listener::getPlugins).flatMap(List::stream).toList())
                .orElse(List.of()),
            bodyPlugins
        )
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public List<Listener> getListeners() {
        return listeners != null ? listeners : List.of();
    }
}
