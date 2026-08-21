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

import static io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator.CONTAINS;
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
 * translate today: {@code EQ, IN} for KEYWORD/ENUM and for the identifier-shaped STRING filters
 * ({@link #PDP}, {@link #MATCHED_POLICY}), {@code EQ} for the free-text ones, {@code CONTAINS} where
 * a fragment is the only usable input ({@link #PAYLOAD}, {@link #REASON}), {@code EQ, GTE, LTE} for
 * measured NUMBER filters and {@code EQ, IN} for {@link #POLICY_VERSION}, which is a discrete
 * generation rather than a measurement. {@code NOT_IN} is intentionally absent until a translator
 * supports it.
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
    TENANT("Tenant", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Defs.GATEWAY_TYPES),
    ZONE("Zone", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.GATEWAY_TYPES),

    // Every API kind but not the decision scope: a decision document carries no entrypoint, so the
    // decision search cannot apply this and would refuse it.
    ENTRYPOINT("Entrypoint", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, ApiType.API_KINDS),

    // --- HTTP -----------------------------------------------------------------------------------
    HTTP_METHOD("HTTP Method", FilterType.ENUM, Defs.EQ_IN, Defs.HTTP_METHODS, null, Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_STATUS("Status Code", FilterType.NUMBER, Defs.NUMBER_OPS, null, new Range(100, 599), Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_STATUS_CODE_GROUP(
        "Status Code Group",
        FilterType.ENUM,
        Defs.EQ_IN,
        Defs.STATUS_CODE_GROUPS,
        null,
        Defs.LOGS_ANALYTICS,
        Defs.HTTP_LLM_MCP_A2A
    ),
    URI("HTTP Path", FilterType.STRING, Defs.EQ_ONLY, null, null, Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HOST("Host", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_GATEWAY_RESPONSE_TIME(
        "Gateway Response Time",
        FilterType.NUMBER,
        Defs.NUMBER_OPS,
        null,
        null,
        Defs.LOGS_ANALYTICS,
        Defs.HTTP_LLM_MCP_A2A
    ),
    HTTP_GATEWAY_LATENCY("Latency", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_ENDPOINT_RESPONSE_TIME(
        "Endpoint Response Time",
        FilterType.NUMBER,
        Defs.NUMBER_OPS,
        null,
        null,
        Defs.ANALYTICS,
        Defs.HTTP_LLM_MCP_A2A
    ),
    HTTP_REQUEST_CONTENT_LENGTH("Request Size", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_RESPONSE_CONTENT_LENGTH("Response Size", FilterType.NUMBER, Defs.NUMBER_OPS, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    GEO_IP_COUNTRY("Country", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    GEO_IP_REGION("Region", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    GEO_IP_CITY("City", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    GEO_IP_CONTINENT("Continent", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    CONSUMER_IP("Consumer IP", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_USER_AGENT_OS_NAME("User Agent OS", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    HTTP_USER_AGENT_DEVICE("User Agent Device", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.HTTP_LLM_MCP_A2A),
    ERROR_KEY("Error Key", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS_ANALYTICS, Defs.HTTP_LLM_MCP_A2A_NATIVE),
    REQUEST_ID("Request ID", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS, Defs.REQUEST_BEARING_KINDS),
    TRANSACTION_ID("Transaction ID", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.LOGS, Defs.HTTP_LLM_MCP_A2A_NATIVE),
    PAYLOAD("Payload content", FilterType.STRING, Defs.CONTAINS_ONLY, null, null, Defs.LOGS, Defs.HTTP_LLM_MCP_A2A),

    // --- Authz decisions ------------------------------------------------------------------------
    // The AUTHZ_ prefix is required: ObservabilityAnalyticsDataPortAdapter resolves these onto APIM's
    // FilterSpec.Name by bare valueOf, so both names must match. Logs-only filters route symbolically
    // and keep their unprefixed names.
    AUTHZ_DECISION("Decision", FilterType.ENUM, Defs.EQ_IN, Defs.DECISIONS, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),
    // STRING, not KEYWORD: these are open sets (any principal, any resource a policy names), so there
    // is nothing to suggest from and the picker would offer an empty dropdown. The indexed fields are
    // keyword, so exact match is the honest operator; CONTAINS would need a wildcard query.
    AUTHZ_SUBJECT_ID("Subject", FilterType.STRING, Defs.EQ_ONLY, null, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),
    AUTHZ_ACTION("Action", FilterType.STRING, Defs.EQ_ONLY, null, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),
    AUTHZ_RESOURCE_ID("Resource", FilterType.STRING, Defs.EQ_ONLY, null, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),
    AUTHZ_CALLER("Caller kind", FilterType.ENUM, Defs.EQ_IN, Defs.AUTHZ_CALLERS, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),
    AUTHZ_STATUS("Outcome status", FilterType.ENUM, Defs.EQ_IN, Defs.AUTHZ_STATUSES, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),
    AUTHZ_OPERATION("Operation", FilterType.ENUM, Defs.EQ_IN, Defs.AUTHZ_OPERATIONS, null, Defs.LOGS_ANALYTICS, Defs.DECISION_RECORDS),

    // STRING for the same reason as SUBJECT above: KEYWORD promises a value list, and the filter
    // data port only serves that for the fields it knows. A PDP id is an open set, so the picker
    // would ask for values, get a 400, and sit on "Loading..." forever.
    PDP("PDP Gateway", FilterType.STRING, Defs.EQ_IN, null, null, Defs.LOGS, Defs.DECISION_RECORDS),
    MATCHED_POLICY("Matched policy", FilterType.STRING, Defs.EQ_IN, null, null, Defs.LOGS, Defs.DECISION_RECORDS),
    // Exact generations only. "Before/after a policy change" is the time range's job, and range
    // support here would mean threading operators through a query model that carries none.
    POLICY_VERSION("Policy version", FilterType.NUMBER, Defs.EQ_IN, null, null, Defs.LOGS, Defs.DECISION_RECORDS),

    // Two constants, because FilterSpec carries one operator list for all signals and the analytics
    // engine has no CONTAINS.
    REASON("Reason", FilterType.STRING, Defs.CONTAINS_ONLY, null, null, Defs.LOGS, Defs.DECISION_RECORDS),
    AUTHZ_REASON("Reason", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Defs.DECISION_RECORDS),

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
        Defs.LOGS_ANALYTICS,
        Set.of(ApiType.NATIVE)
    ),
    /**
     * Derived (never stored in ES): the logs search translates each requested origin into a
     * boolean predicate over the error key + native connection status, mirroring the
     * classification of {@code FailureOriginClassifier}/{@code NativeFailureOriginRules}.
     * LOGS-only until the analytics engine learns the same translation.
     */
    FAILURE_ORIGIN("Failure Origin", FilterType.ENUM, Defs.EQ_IN, Defs.FAILURE_ORIGINS, null, Defs.LOGS, Set.of(ApiType.NATIVE)),
    /**
     * Analytics counterpart of {@link #FAILURE_ORIGIN}: the side the gateway <em>stored</em>, rather
     * than the origin the logs derive from the error key. Two names for one idea is a smell, but the
     * alternative is worse — the analytics engine cannot replay the classification rules inside an
     * aggregation, and advertising the logs' vocabulary over raw values would lie about what is
     * being grouped. They agree in practice: the classifier trusts the stored side first.
     */
    NATIVE_FAILURE_SIDE(
        "Failure Side",
        FilterType.ENUM,
        Defs.EQ_IN,
        Defs.NATIVE_FAILURE_SIDES,
        null,
        Defs.ANALYTICS,
        Set.of(ApiType.NATIVE)
    ),
    /** Kafka {@code client.id} — the only attribution left once a connection fails before authenticating. */
    NATIVE_CLIENT_ID("Kafka Client ID", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.NATIVE)),
    /**
     * Dimensions of the Kafka event metrics, ANALYTICS-only: they live in the {@code event-metrics} data
     * stream, which the logs signal does not read. Both are KEYWORD rather than ENUM — a topic name
     * is unbounded, and the set of Kafka protocol operations grows with the protocol. Neither is
     * resolvable from the management database, so the filter bar offers no value suggestions for
     * them until a value lookup over the event-metrics index is wired in; faceting (Top topics, Top
     * operations) works regardless.
     */
    NATIVE_TOPIC("Kafka Topic", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.NATIVE)),
    NATIVE_OPERATION("Kafka Operation", FilterType.KEYWORD, Defs.EQ_IN, null, null, Defs.ANALYTICS, Set.of(ApiType.NATIVE)),

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
        private static final List<FilterOperator> CONTAINS_ONLY = List.of(CONTAINS);
        private static final List<FilterOperator> NUMBER_OPS = List.of(EQ, GTE, LTE);

        private static final Set<Signal> LOGS = Set.of(Signal.LOGS);
        private static final Set<Signal> ANALYTICS = Set.of(Signal.ANALYTICS);
        private static final Set<Signal> LOGS_ANALYTICS = Set.of(Signal.LOGS, Signal.ANALYTICS);

        private static final Set<ApiType> HTTP_LLM_MCP_A2A = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.A2A);
        private static final Set<ApiType> HTTP_LLM_MCP_A2A_NATIVE = Set.of(
            ApiType.HTTP_PROXY,
            ApiType.LLM,
            ApiType.MCP,
            ApiType.A2A,
            ApiType.NATIVE
        );
        private static final Set<ApiType> APP_TYPES = Set.of(
            ApiType.HTTP_PROXY,
            ApiType.LLM,
            ApiType.MCP,
            ApiType.A2A,
            ApiType.MESSAGE,
            ApiType.NATIVE
        );
        private static final Set<ApiType> GATEWAY_TYPES = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.A2A, ApiType.EDGE);
        private static final Set<ApiType> DECISION_RECORDS = Set.of(ApiType.AUTHZ_DECISION);

        /**
         * Every kind whose rows carry a request id, decisions included. Named rather than
         * {@link ApiType#ALL}: that would also advertise the filter for MESSAGE and EDGE, which the
         * logs search does not serve.
         */
        private static final Set<ApiType> REQUEST_BEARING_KINDS = Set.of(
            ApiType.HTTP_PROXY,
            ApiType.LLM,
            ApiType.MCP,
            ApiType.A2A,
            ApiType.NATIVE,
            ApiType.AUTHZ_DECISION
        );

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

        private static final List<EnumValue> FAILURE_ORIGINS = List.of(
            new EnumValue("NONE", "No failure"),
            new EnumValue("CLIENT_TO_GATEWAY", "Client \u2194 Gateway"),
            new EnumValue("GATEWAY_TO_BROKER", "Gateway \u2194 Broker"),
            new EnumValue("GATEWAY_INTERNAL", "Gateway internal"),
            new EnumValue("UNKNOWN", "Undetermined")
        );

        /**
         * Raw values written by the gateway. Labels borrow the logs' user-facing wording so the two
         * screens read the same, even though the underlying vocabularies differ.
         */
        private static final List<EnumValue> NATIVE_FAILURE_SIDES = List.of(
            new EnumValue("DOWNSTREAM", "Client \u2194 Gateway"),
            new EnumValue("UPSTREAM", "Gateway \u2194 Broker"),
            new EnumValue("INTERNAL", "Gateway internal")
        );

        private static final List<EnumValue> NATIVE_CONNECTION_STATUSES = List.of(
            new EnumValue("CONNECTED", "Connected"),
            new EnumValue("DISCONNECTED", "Disconnected"),
            new EnumValue("CONNECTION_ERROR", "Connection error"),
            new EnumValue("SESSION_ERROR", "Session error"),
            new EnumValue("INTERNAL_ERROR", "Internal error")
        );

        /**
         * What produced the decision, as the reporter writes it (lowercase wire values). {@code reactor}
         * is deliberately absent: it was renamed to {@code gateway} in gravitee-reporter-api 2.7.1, so
         * only pre-2.7.1 documents carry it and nothing emits it today.
         */
        private static final List<EnumValue> AUTHZ_CALLERS = List.of(
            new EnumValue("pep", "PEP policy"),
            new EnumValue("gateway", "Gateway"),
            new EnumValue("authzen", "AuthZEN endpoint"),
            new EnumValue("unknown", "Unknown")
        );

        private static final List<EnumValue> AUTHZ_STATUSES = List.of(
            new EnumValue("success", "Success"),
            new EnumValue("error", "Error"),
            new EnumValue("not-ready", "Not ready")
        );

        private static final List<EnumValue> AUTHZ_OPERATIONS = List.of(
            new EnumValue("evaluate", "Evaluation"),
            new EnumValue("search", "Search")
        );

        private static final List<EnumValue> DECISIONS = List.of(
            new EnumValue("PERMIT", "Permit"),
            new EnumValue("FORBID", "Forbid"),
            new EnumValue("NOT_APPLICABLE", "Not applicable")
        );

        /** Enum value whose display label is identical to its wire value. */
        private static EnumValue self(String value) {
            return new EnumValue(value, value);
        }

        private Defs() {}
    }
}
