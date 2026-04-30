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
package io.gravitee.gamma.module.apim.core.tracing.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.module.apim.core.tracing.model.EdgeSpan;
import io.gravitee.gamma.module.apim.core.tracing.model.TracingEdge;
import io.gravitee.gamma.module.apim.core.tracing.model.TracingGraph;
import io.gravitee.gamma.module.apim.core.tracing.model.TracingNode;
import io.gravitee.node.api.opentelemetry.query.model.Trace;
import io.gravitee.node.api.opentelemetry.query.model.TraceSpan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure-function builder: takes a {@link Trace} and produces a {@link TracingGraph} by classifying spans as agent/LLM/MCP nodes from
 * OTel attributes. No I/O, no Spring.
 */
@DomainService
public class TracingGraphBuilder {

    private static final Set<String> LLM_OPERATION_NAMES = Set.of("chat", "generate_content", "text_completion", "stream_generate_content");

    public TracingGraph buildTracingGraph(Trace trace) {
        Map<String, TracingNode> nodes = new LinkedHashMap<>();
        Map<String, List<EdgeSpan>> edgesMap = new LinkedHashMap<>();
        Map<String, Set<String>> mcpToolsPerServer = new HashMap<>();
        Map<String, long[]> llmTokenTotals = new HashMap<>();

        String agentNodeId = "agent";
        String agentLabel = "Agent";
        String agentSubtitle = null;
        boolean foundAgentSpan = false;

        for (TraceSpan span : trace.spans()) {
            Map<String, String> attrs = span.attributes();
            if (isAgentSpan(span.operationName(), attrs)) {
                String name = attrs.get("gen_ai.agent.name");
                if (name == null) name = attrs.get("agent.name");
                String id = attrs.get("gen_ai.agent.id");
                if (id == null) id = attrs.get("agent.id");

                if (name != null) agentLabel = name;
                if (id != null) agentNodeId = id;

                String provider = attrs.get("gen_ai.provider.name");
                String model = attrs.get("gen_ai.request.model");
                if (provider != null && model != null) {
                    agentSubtitle = capitalize(provider) + " · " + model;
                } else if (provider != null) {
                    agentSubtitle = capitalize(provider);
                }
                foundAgentSpan = true;
                break;
            }
        }

        if (!foundAgentSpan) {
            for (TraceSpan span : trace.spans()) {
                String serviceName = span.serviceName();
                if (serviceName != null && serviceName.contains("langchain4j")) {
                    agentLabel = serviceName;
                    agentSubtitle = "Langchain4j · Java";
                    break;
                }
                if (serviceName != null && serviceName.contains("agent")) {
                    agentLabel = serviceName;
                    agentSubtitle = serviceName;
                    break;
                }
            }
        }

        nodes.put(agentNodeId, new TracingNode(agentNodeId, "agent", agentLabel, agentSubtitle));

        boolean hasErrors = false;

        for (TraceSpan span : trace.spans()) {
            String serviceName = span.serviceName();
            String operation = span.operationName();
            Map<String, String> attrs = span.attributes();

            if (isAgentSpan(operation, attrs)) {
                continue;
            } else if (isMcpSpan(attrs)) {
                String mcpId = getMcpNodeId(attrs, serviceName);
                String serverName = attrs.get("mcp.server.name");
                if (serverName == null) serverName = serviceName != null ? serviceName : "MCP Server";
                String transport = attrs.get("network.transport");
                if (transport == null) transport = attrs.get("mcp.transport");
                String toolName = attrs.get("gen_ai.tool.name");
                if (toolName == null) toolName = attrs.get("mcp.tool.name");

                if (toolName != null) {
                    mcpToolsPerServer.computeIfAbsent(mcpId, k -> new LinkedHashSet<>()).add(toolName);
                }

                if (!nodes.containsKey(mcpId)) {
                    String subtitle = transport != null ? capitalize(transport) : null;
                    Map<String, String> metadata = new LinkedHashMap<>();
                    if (transport != null) metadata.put("transport", transport);
                    nodes.put(mcpId, new TracingNode(mcpId, "mcp_server", serverName, subtitle, null, metadata));
                }

                String spanStatus = getStatus(attrs);
                if ("error".equals(spanStatus)) hasErrors = true;

                String edgeKey = agentNodeId + "->" + mcpId;
                edgesMap
                    .computeIfAbsent(edgeKey, k -> new ArrayList<>())
                    .add(new EdgeSpan(operation, span.durationNanos(), spanStatus, toolName, null));
            } else if (isLlmSpan(attrs)) {
                String llmId = getLlmNodeId(attrs);
                String model = attrs.get("gen_ai.request.model");
                if (model == null) model = attrs.getOrDefault("llm.model", "LLM");
                String provider = attrs.get("gen_ai.provider.name");
                if (provider == null) provider = attrs.get("llm.provider");

                if (!nodes.containsKey(llmId)) {
                    String subtitle = buildLlmSubtitle(provider, model);
                    Map<String, String> metadata = new LinkedHashMap<>();
                    if (provider != null) metadata.put("provider", provider);
                    metadata.put("model", model);
                    nodes.put(llmId, new TracingNode(llmId, "llm", model, subtitle, null, metadata));
                }

                Integer tokens = parseTokens(attrs);
                if (tokens != null) {
                    llmTokenTotals.computeIfAbsent(llmId, k -> new long[] { 0 })[0] += tokens;
                }

                String spanStatus = getStatus(attrs);
                if ("error".equals(spanStatus)) hasErrors = true;

                String edgeKey = agentNodeId + "->" + llmId;
                edgesMap
                    .computeIfAbsent(edgeKey, k -> new ArrayList<>())
                    .add(new EdgeSpan(operation, span.durationNanos(), spanStatus, null, tokens));
            }
        }

        for (Map.Entry<String, Set<String>> entry : mcpToolsPerServer.entrySet()) {
            String mcpId = entry.getKey();
            TracingNode existing = nodes.get(mcpId);
            if (existing != null) {
                int toolCount = entry.getValue().size();
                String transport = existing.metadata().get("transport");
                String subtitle =
                    (transport != null ? capitalize(transport) : "") + " · " + toolCount + " tool" + (toolCount > 1 ? "s" : "");
                nodes.put(
                    mcpId,
                    new TracingNode(mcpId, existing.type(), existing.label(), subtitle.trim(), existing.status(), existing.metadata())
                );
            }
        }

        for (Map.Entry<String, long[]> entry : llmTokenTotals.entrySet()) {
            String llmId = entry.getKey();
            TracingNode existing = nodes.get(llmId);
            if (existing != null && entry.getValue()[0] > 0) {
                String base = existing.subtitle() != null ? existing.subtitle() : existing.label();
                String subtitle = base + " · " + entry.getValue()[0] + " tokens";
                nodes.put(
                    llmId,
                    new TracingNode(llmId, existing.type(), existing.label(), subtitle, existing.status(), existing.metadata())
                );
            }
        }

        TracingNode agentNode = nodes.get(agentNodeId);
        String agentStatus = hasErrors ? "error" : "healthy";
        nodes.put(
            agentNodeId,
            new TracingNode(agentNodeId, agentNode.type(), agentNode.label(), agentNode.subtitle(), agentStatus, agentNode.metadata())
        );

        List<TracingEdge> edges = new ArrayList<>();
        for (Map.Entry<String, List<EdgeSpan>> entry : edgesMap.entrySet()) {
            String[] parts = entry.getKey().split("->");
            edges.add(new TracingEdge(parts[0], parts[1], entry.getValue()));
        }

        return new TracingGraph(trace.traceId(), trace.durationNanos(), new ArrayList<>(nodes.values()), edges);
    }

    private boolean isAgentSpan(String operation, Map<String, String> attrs) {
        String opName = attrs.get("gen_ai.operation.name");
        if ("invoke_agent".equals(opName)) return true;
        return operation != null && operation.startsWith("invoke_agent ");
    }

    private boolean isMcpSpan(Map<String, String> attrs) {
        if (attrs.containsKey("mcp.method.name")) return true;
        return attrs.containsKey("gen_ai.tool.name") && "execute_tool".equals(attrs.get("gen_ai.operation.name"));
    }

    private boolean isLlmSpan(Map<String, String> attrs) {
        String opName = attrs.get("gen_ai.operation.name");
        return opName != null && LLM_OPERATION_NAMES.contains(opName);
    }

    private String getMcpNodeId(Map<String, String> attrs, String serviceName) {
        String serverName = attrs.get("mcp.server.name");
        if (serverName != null) return "mcp-" + serverName;
        if (serviceName != null) return "mcp-" + serviceName;
        return "mcp-server";
    }

    private String getLlmNodeId(Map<String, String> attrs) {
        String model = attrs.get("gen_ai.request.model");
        if (model == null) model = attrs.getOrDefault("llm.model", "unknown");
        return "llm-" + model.replaceAll("[^a-zA-Z0-9-]", "-");
    }

    private String getStatus(Map<String, String> attrs) {
        String status = attrs.get("otel.status_code");
        return status != null ? status.toLowerCase() : "ok";
    }

    private Integer parseTokens(Map<String, String> attrs) {
        String inputTokens = attrs.get("gen_ai.usage.input_tokens");
        String outputTokens = attrs.get("gen_ai.usage.output_tokens");
        if (inputTokens == null) inputTokens = attrs.get("gen_ai.usage.prompt_tokens");
        if (outputTokens == null) outputTokens = attrs.get("gen_ai.usage.completion_tokens");

        if (inputTokens != null || outputTokens != null) {
            try {
                int total = 0;
                if (inputTokens != null) total += Integer.parseInt(inputTokens);
                if (outputTokens != null) total += Integer.parseInt(outputTokens);
                return total;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String buildLlmSubtitle(String provider, String model) {
        if (provider != null) {
            return formatProvider(provider) + " · " + model;
        }
        return model;
    }

    private String formatProvider(String provider) {
        if (provider == null) return "";
        return Arrays.stream(provider.split("[-_]"))
            .map(this::capitalize)
            .reduce((a, b) -> a + " " + b)
            .orElse(provider);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
