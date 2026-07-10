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
package io.gravitee.gateway.reactive.handlers.api.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.agent.StandaloneAgentDefinition;
import io.gravitee.definition.model.v4.agent.definition.AgentTool;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * Translates a first-class {@link io.gravitee.definition.model.v4.agent.AgentApi} into a <b>synthetic</b> proxy
 * {@link io.gravitee.definition.model.v4.Api} so an agent can be deployed and served through the reused
 * {@code DefaultApiReactor} pipeline (security/analytics/metrics/tracing/resources) without any change to that
 * shared pipeline — the reactable's {@code getDefinition()} then returns a real {@code model.v4.Api}, so the
 * platform processors (e.g. {@code MetricsProcessor}) that cast to it no longer fail.
 *
 * <p>The shell carries what the pipeline + runtime read: id/name/version, {@code type=AGENT} (so the agent reactor
 * factory claims it, not the proxy one), the agent's <b>HTTP</b> listeners (a composition-only agent — {@code
 * composable} with no published plan — has them stripped, so it is never HTTP-exposed), the plans, and — for a
 * {@code kind:standalone} agent — its <b>capabilities</b> translated into the shapes the existing runtime already
 * consumes: inline {@code tools} → synthetic endpoint groups (one per tool type; tools are {@code endpoint-connector}
 * plugins), inline {@code skills} → synthetic resources (skills are {@code resource} plugins), and the referenced
 * memory store flows through as an api-level resource ({@code workingMemory.ref}). With no tools, a single empty
 * placeholder endpoint group satisfies the proxy model's non-empty {@code endpointGroups} requirement.</p>
 *
 * @author GraviteeSource Team
 */
@CustomLog
public final class AgentApiSynthesizer {

    /** Name of the placeholder endpoint group — used when an agent declares no tools. */
    static final String SYNTHETIC_ENDPOINT_GROUP = "__agent__";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentApiSynthesizer() {}

    public static io.gravitee.definition.model.v4.Api synthesize(final io.gravitee.definition.model.v4.agent.AgentApi agent) {
        final io.gravitee.definition.model.v4.Api proxy = new io.gravitee.definition.model.v4.Api();
        proxy.setId(agent.getId());
        proxy.setName(agent.getName());
        proxy.setApiVersion(agent.getApiVersion());
        proxy.setType(ApiType.AGENT);
        proxy.setDefinitionVersion(DefinitionVersion.V4);
        proxy.setTags(agent.getTags());
        proxy.setProperties(agent.getProperties());
        // The agent exposes only tracing (AgentAnalytics); map it onto the generic Analytics the proxy pipeline expects.
        io.gravitee.definition.model.v4.analytics.Analytics analytics = new io.gravitee.definition.model.v4.analytics.Analytics();
        if (agent.getAnalytics() != null) {
            analytics.setTracing(agent.getAnalytics().getTracing());
        }
        proxy.setAnalytics(analytics);
        // Resources = the agent's declared (api-level) resources — incl. the memory store the agent references via
        // workingMemory.ref — plus one synthetic resource per inline skill (skills are `resource` plugins).
        proxy.setResources(resources(agent));

        // HTTP exposure is gated on a PUBLISHED plan; composition is gated on the `composable` flag. A composable
        // agent with no published plan is composition-only: it must deploy (to be resolvable as a
        // sub-agent) yet must never be served over HTTP. We strip any HTTP listener and attach a synthetic keyless
        // plan purely to satisfy the generic deploy gate — there is no HTTP listener left for it to secure. The
        // clean agent definition is untouched.
        final boolean hasPublishedPlan =
            agent.getPlans() != null &&
            agent
                .getPlans()
                .stream()
                .anyMatch(plan -> plan.getStatus() == PlanStatus.PUBLISHED);

        if (!hasPublishedPlan && agent.isComposable()) {
            log.info(
                "Agent [{}] is composable and has no published plan — deploying for composition use only; HTTP exposure is disabled.",
                agent.getId()
            );
            proxy.setListeners(List.of());
            proxy.setPlans(List.of(syntheticKeylessPlan()));
        } else {
            // Only HTTP listeners feed the proxy pipeline.
            proxy.setListeners(
                agent
                    .getListeners()
                    .stream()
                    .filter(listener -> listener.getType() == ListenerType.HTTP)
                    .toList()
            );
            if (agent.getPlans() != null) {
                proxy.setPlans(agent.getPlans());
            }
        }

        // Tools are `endpoint-connector` plugins (tool-http/tool-mcp/…): translate the agent's inline tools into
        // synthetic endpoint groups (one per tool type) so DefaultEndpointManager instantiates the ToolEndpointConnectors
        // and the existing ConnectorToolResolver wires them as LangChain4J tools. With no tools, an empty placeholder
        // group satisfies the proxy model's non-empty endpointGroups requirement.
        proxy.setEndpointGroups(toolEndpointGroups(agent));

        return proxy;
    }

    private static List<Resource> resources(final io.gravitee.definition.model.v4.agent.AgentApi agent) {
        final List<Resource> resources = new ArrayList<>();
        if (agent.getResources() != null) {
            resources.addAll(agent.getResources());
        }

        resources.addAll(skillsResources(agent));
        resources.addAll(channelsResources(agent));

        return resources;
    }

    /** The agent's api-level resources plus one synthetic resource per inline skill (skills are `resource` plugins). */
    private static List<Resource> skillsResources(final io.gravitee.definition.model.v4.agent.AgentApi agent) {
        final StandaloneAgentDefinition definition = agent.getStandalone();
        if (definition != null && definition.getSkills() != null) {
            return definition.getSkills().stream().map(agentSkill -> Resource.builder()
                    .type(agentSkill.getType())
                    .name(agentSkill.getName())
                    .configuration(serializeConfiguration(agentSkill.getConfiguration()))
                    .enabled(true)
                    .build()).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /** The agent's api-level resources plus one synthetic resource per channel (channels are `resource` plugins). */
    private static List<Resource> channelsResources(final io.gravitee.definition.model.v4.agent.AgentApi agent) {

        if (agent.getChannels() != null) {
            return agent.getChannels().stream().map(channel -> Resource.builder()
                    .type(channel.getType())
                    .name(channel.getName())
                    .configuration(serializeConfiguration(channel.getConfiguration()))
                    .enabled(true)
                    .build()).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /** One synthetic endpoint group per distinct tool {@code type}, each holding one endpoint per tool of that type. */
    private static List<EndpointGroup> toolEndpointGroups(final io.gravitee.definition.model.v4.agent.AgentApi agent) {
        final StandaloneAgentDefinition definition = agent.getStandalone();
        if (definition == null || definition.getTools() == null || definition.getTools().isEmpty()) {
            return List.of(placeholderEndpointGroup());
        }

        final Map<String, List<AgentTool>> toolsByType = new LinkedHashMap<>();
        for (final AgentTool tool : definition.getTools()) {
            toolsByType.computeIfAbsent(tool.getType(), type -> new ArrayList<>()).add(tool);
        }

        final List<EndpointGroup> groups = new ArrayList<>();
        toolsByType.forEach((type, tools) -> {
            final EndpointGroup group = new EndpointGroup();
            group.setName("__tools_" + type + "__");
            group.setType(type);
            final List<Endpoint> endpoints = new ArrayList<>();
            for (final AgentTool tool : tools) {
                final Endpoint endpoint = new Endpoint();
                endpoint.setName(tool.getName());
                endpoint.setType(type);
                endpoint.setConfiguration(serializeConfiguration(tool.getConfiguration()));
                endpoints.add(endpoint);
            }
            group.setEndpoints(endpoints);
            groups.add(group);
        });
        return groups;
    }

    private static EndpointGroup placeholderEndpointGroup() {
        final EndpointGroup group = new EndpointGroup();
        group.setName(SYNTHETIC_ENDPOINT_GROUP);
        group.setType("http-proxy");
        group.setEndpoints(List.of());
        return group;
    }

    /** Capability configuration is a parsed JSON {@code Object} in the clean model; connectors/resources want a JSON String. */
    private static String serializeConfiguration(final Object configuration) {
        if (configuration == null) {
            return "{}";
        }
        if (configuration instanceof String string) {
            return string;
        }
        try {
            return MAPPER.writeValueAsString(configuration);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize agent capability configuration", e);
        }
    }

    /**
     * Synthetic published keyless plan attached to a composition-only agent (a composable agent with no published
     * plan) solely to satisfy the generic deploy gate. It secures nothing — the synthetic carries no HTTP listener,
     * and the agent never attaches an HTTP reactor.
     */
    private static Plan syntheticKeylessPlan() {
        return Plan.builder()
            .id("internal")
            .name("Internal")
            .mode(PlanMode.STANDARD)
            .status(PlanStatus.PUBLISHED)
            .security(PlanSecurity.builder().type("key-less").build())
            .build();
    }
}
