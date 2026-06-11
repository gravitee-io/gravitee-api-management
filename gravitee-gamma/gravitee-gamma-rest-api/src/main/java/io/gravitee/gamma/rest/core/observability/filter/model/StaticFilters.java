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
package io.gravitee.gamma.rest.core.observability.filter.model;

import static io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator.EQ;
import static io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator.GTE;
import static io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator.IN;
import static io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator.LTE;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec.EnumValue;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec.Range;
import java.util.List;
import java.util.Set;

/**
 * Host-owned filters with a <b>fixed definition</b> that modules cannot extend (unlike
 * {@link ExtensibleFilters}). This is the <b>authoritative unified observability vocabulary</b>
 * (GMA-422): the single source of truth for the canonical filter names, types, operators, and the
 * two discovery axes ({@link Signal} × {@link ApiType}) shared by the logs and analytics surfaces.
 *
 * <p>Names are reconciled across the legacy logs and analytics engines (see GMA-422): e.g. the
 * gateway response time is {@code HTTP_GATEWAY_RESPONSE_TIME} (not the logs-side {@code RESPONSE_TIME}),
 * the MCP method is {@code MCP_PROXY_METHOD} (not {@code MCP_METHOD}), and request-path filtering is
 * {@code URI} (the v4-populated field) — the analytics {@code HTTP_PATH} is dropped (empty on v4) and
 * {@code HTTP_PATH_MAPPING} is an analytics facet, not a filter, so neither appears here.
 *
 * <p>Operators advertised here are restricted to what the v4 analytics/logs engines actually
 * translate today: {@code EQ, IN} for KEYWORD/ENUM/STRING and {@code EQ, GTE, LTE} for NUMBER.
 * {@code NOT_IN} is intentionally absent until a translator supports it.
 *
 * <p>{@code signals} reflect what is served today (logs + analytics); traces join in a later lot.
 * The trace explorer keeps its own separate registry, so the signal sets here are unaffected by it.
 *
 * <p>Conceptually host-internal: although the enum is public (the registry, in the infra layer,
 * needs it), modules have no way to contribute to these — the contribution map is keyed by
 * {@link ExtensibleFilters}, and the registry rejects any module filter whose name collides with a
 * host-owned name (see {@link CommonFilters}).
 *
 * @author GraviteeSource Team
 */
public enum StaticFilters {
    // --- Global / cross-cutting ----------------------------------------------------------------
    API("API", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, ApiType.ALL),
    APPLICATION("Application", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Defs.APP_TYPES),
    PLAN("Plan", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Defs.APP_TYPES),
    API_PRODUCT("API Product", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.HTTP_PROXY)),
    GATEWAY("Gateway", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.GATEWAY_TYPES),
    TENANT("Tenant", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.GATEWAY_TYPES),
    ZONE("Zone", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.GATEWAY_TYPES),

    // --- HTTP -----------------------------------------------------------------------------------
    HTTP_METHOD("HTTP Method", FilterType.ENUM, Defs.EQ_IN, Defs.HTTP_METHODS, null, Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_STATUS("Status Code", FilterType.NUMBER, Defs.NUMBER_OPS, null, new Range(100, 599), Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_STATUS_CODE_GROUP(
        "Status Code Group",
        FilterType.ENUM,
        Defs.EQ_IN,
        Defs.STATUS_CODE_GROUPS,
        null,
        Defs.LOGS_ANALYTICS,
        Defs.HTTP_LLM_MCP
    ),
    URI("HTTP Path", FilterType.STRING, Defs.EQ_ONLY, null, null, Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP),
    HOST("Host", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_GATEWAY_RESPONSE_TIME(
        "Gateway Response Time",
        FilterType.NUMBER,
        Defs.NUMBER_OPS,
        null,
        null,
        Defs.LOGS_ANALYTICS,
        Defs.HTTP_LLM_MCP
    ),
    HTTP_GATEWAY_LATENCY("Latency", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_ENDPOINT_RESPONSE_TIME(
        "Endpoint Response Time",
        FilterType.NUMBER,
        Defs.NUMBER_OPS,
        null,
        null,
        Defs.ANALYTICS,
        Defs.HTTP_LLM_MCP
    ),
    HTTP_REQUEST_CONTENT_LENGTH("Request Size", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_RESPONSE_CONTENT_LENGTH("Response Size", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    GEO_IP_COUNTRY("Country", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    GEO_IP_REGION("Region", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    GEO_IP_CITY("City", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    GEO_IP_CONTINENT("Continent", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    CONSUMER_IP("Consumer IP", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_USER_AGENT_OS_NAME("User Agent OS", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    HTTP_USER_AGENT_DEVICE("User Agent Device", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP),
    ERROR_KEY("Error Key", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP),
    REQUEST_ID("Request ID", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS, Defs.HTTP_LLM_MCP),
    TRANSACTION_ID("Transaction ID", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS, Defs.HTTP_LLM_MCP),

    // --- LLM ------------------------------------------------------------------------------------
    LLM_PROXY_MODEL("LLM Model", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.LLM)),
    LLM_PROXY_PROVIDER("LLM Provider", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.LLM)),

    // --- MCP ------------------------------------------------------------------------------------
    MCP_PROXY_METHOD("MCP Method", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.MCP)),
    MCP_PROXY_TOOL("MCP Tool", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.MCP)),
    MCP_PROXY_RESOURCE("MCP Resource", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.MCP)),
    MCP_PROXY_PROMPT("MCP Prompt", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Set.of(ApiType.MCP)),

    // --- Message --------------------------------------------------------------------------------
    MESSAGE_OPERATION_TYPE(
        "Operation",
        FilterType.ENUM,
        Defs.EQ_IN,
        Defs.MESSAGE_OPERATIONS,
        null,
        Defs.ANALYTICS,
        Set.of(ApiType.MESSAGE)
    ),
    MESSAGE_CONNECTOR_TYPE("Connector Type", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.MESSAGE)),
    MESSAGE_SIZE("Message Size", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Set.of(ApiType.MESSAGE)),
    MESSAGE_COUNT("Message Count", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Set.of(ApiType.MESSAGE)),
    MESSAGE_ERROR_COUNT("Errors", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Set.of(ApiType.MESSAGE)),

    // --- Native (Kafka) -------------------------------------------------------------------------
    NATIVE_CONNECTION_STATUS(
        "Native Connection Status",
        FilterType.ENUM,
        Defs.EQ_IN,
        Defs.NATIVE_CONNECTION_STATUSES,
        null,
        Defs.ANALYTICS,
        Set.of(ApiType.NATIVE)
    ),

    // --- Edge -----------------------------------------------------------------------------------
    EDGE_PROVIDER("Edge Provider", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.EDGE)),
    EDGE_PROCESS("Edge Process", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.EDGE)),
    EDGE_CLIENT("Edge Client", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.EDGE)),
    EDGE_TYPE("Edge Type", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.EDGE));

    private final String label;
    private final FilterType type;
    private final List<FilterOperator> operators;
    private final List<EnumValue> enumValues;
    private final Range range;
    private final Set<Signal> signals;
    private final Set<ApiType> apiTypes;

    StaticFilters(
        String label,
        FilterType type,
        List<FilterOperator> operators,
        List<EnumValue> enumValues,
        Range range,
        Set<Signal> signals,
        Set<ApiType> apiTypes
    ) {
        this.label = label;
        this.type = type;
        this.operators = operators;
        this.enumValues = enumValues;
        this.range = range;
        this.signals = signals;
        this.apiTypes = apiTypes;
    }

    /** Stable filter name echoed on the wire (the enum constant name). */
    public String filterName() {
        return name();
    }

    public FilterSpec toSpec() {
        return new FilterSpec(name(), label, type, operators, enumValues, range, signals, apiTypes);
    }

    /**
     * Shared constants for the catalog above. They live in a nested class (not as enum static
     * fields) because Java forbids an enum constant's constructor from referencing the enum's own
     * static fields — referencing a separate class's static members is fine.
     */
    private static final class Defs {

        private static final List<FilterOperator> EQ_IN = List.of(EQ, IN);
        private static final List<FilterOperator> EQ_ONLY = List.of(EQ);
        private static final List<FilterOperator> NUMBER_OPS = List.of(EQ, GTE, LTE);

        private static final Set<Signal> LOGS = Set.of(Signal.LOGS);
        private static final Set<Signal> ANALYTICS = Set.of(Signal.ANALYTICS);
        private static final Set<Signal> LOGS_ANALYTICS = Set.of(Signal.LOGS, Signal.ANALYTICS);

        private static final Set<ApiType> HTTP_LLM_MCP = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP);
        private static final Set<ApiType> APP_TYPES = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.MESSAGE, ApiType.NATIVE);
        private static final Set<ApiType> GATEWAY_TYPES = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.EDGE);

        private static final List<EnumValue> HTTP_METHODS = List.of(
            self("CONNECT"),
            self("DELETE"),
            self("GET"),
            self("HEAD"),
            self("OPTIONS"),
            self("PATCH"),
            self("POST"),
            self("PUT"),
            self("TRACE"),
            new EnumValue("OTHER", "Other")
        );

        private static final List<EnumValue> STATUS_CODE_GROUPS = List.of(
            new EnumValue("1XX", "1xx Informational"),
            new EnumValue("2XX", "2xx Success"),
            new EnumValue("3XX", "3xx Redirection"),
            new EnumValue("4XX", "4xx Client Error"),
            new EnumValue("5XX", "5xx Server Error")
        );

        private static final List<EnumValue> MESSAGE_OPERATIONS = List.of(self("Publish"), self("Subscribe"));

        private static final List<EnumValue> NATIVE_CONNECTION_STATUSES = List.of(
            new EnumValue("CONNECTED", "Connected"),
            new EnumValue("DISCONNECTED", "Disconnected"),
            new EnumValue("CONNECTION_ERROR", "Connection error"),
            new EnumValue("SESSION_ERROR", "Session error"),
            new EnumValue("INTERNAL_ERROR", "Internal error")
        );

        /** Enum value whose display label is identical to its wire value. */
        private static EnumValue self(String value) {
            return new EnumValue(value, value);
        }

        private Defs() {}
    }
}
