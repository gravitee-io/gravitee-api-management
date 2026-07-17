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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.elasticsearch.v4.shared.StatusCodeGroups;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import io.gravitee.repository.log.v4.model.connection.NativeFailureOriginRules;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;
import org.springframework.util.CollectionUtils;

public class SearchMetricsQueryAdapter {

    private static final String NATIVE_CONNECTION_STATUS_FIELD =
        RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.CONNECTION_STATUS;
    private static final String FAILURE_SIDE_FIELD = RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.FAILURE_SIDE;

    private SearchMetricsQueryAdapter() {}

    public static String adapt(MetricsQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("from", (query.getPage() - 1) * query.getSize());
        jsonContent.put("size", query.getSize());

        var esQuery = buildElasticQuery(query.getFilter());
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }
        jsonContent.put("sort", buildSort());

        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(MetricsQuery.Filter filter) {
        if (filter == null) {
            return null;
        }

        var mustFilterList = new ArrayList<JsonObject>();

        addApisFilter(filter, mustFilterList);

        addApiProductIdsFilter(filter, mustFilterList);

        addFromAndToFilters(filter, mustFilterList);

        addApplicationsFilter(filter, mustFilterList);

        addPlansFilter(filter, mustFilterList);

        addHttpMethodsFilter(filter, mustFilterList);

        addMcpMethodsFilter(filter, mustFilterList);

        addStatusesFilter(filter, mustFilterList);

        addStatusRangesFilter(filter, mustFilterList);

        addStatusCodeGroupsFilter(filter, mustFilterList);

        addEntrypointIdsFilter(filter, mustFilterList);

        addRequestIdsFilter(filter, mustFilterList);

        addTransactionIdsFilter(filter, mustFilterList);

        addUriFilter(filter, mustFilterList);

        addErrorKeysFilter(filter, mustFilterList);

        addResponseTimeRangesFilter(filter, mustFilterList);

        addLlmProxyModelsFilter(filter, mustFilterList);

        addLlmProxyProvidersFilter(filter, mustFilterList);

        addMcpProxyToolsFilter(filter, mustFilterList);

        addMcpProxyResourcesFilter(filter, mustFilterList);

        addMcpProxyPromptsFilter(filter, mustFilterList);

        addNativeConnectionStatusesFilter(filter, mustFilterList);

        addFailureOriginsFilter(filter, mustFilterList);

        if (!mustFilterList.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", new JsonArray(mustFilterList)));
        }

        return null;
    }

    private static void addApisFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApiIds())) {
            mustFilterList.add(buildV2AndV4Terms(RequestV2MetricsV4Fields.API_ID, filter.getApiIds()));
        }
    }

    private static void addApiProductIdsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApiProductIds())) {
            // API product ID exists only in the v4 metrics index — use buildV4Terms, not buildV2AndV4Terms
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.API_PRODUCT_ID, filter.getApiProductIds()));
        }
    }

    private static void addRequestIdsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getRequestIds())) {
            mustFilterList.add(buildV2AndV4Terms(RequestV2MetricsV4Fields.REQUEST_ID, filter.getRequestIds()));
        }
    }

    private static void addTransactionIdsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getTransactionIds())) {
            mustFilterList.add(buildV2AndV4Terms(RequestV2MetricsV4Fields.TRANSACTION_ID, filter.getTransactionIds()));
        }
    }

    private static void addUriFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        String uriKeyword = filter.getUri();
        if (uriKeyword != null && !uriKeyword.isBlank()) {
            String beginningSlash = uriKeyword.startsWith("/") ? "" : "/";
            String endingWildcard = uriKeyword.endsWith("*") ? "" : "*";

            String fullUri = beginningSlash + uriKeyword + endingWildcard;

            mustFilterList.add(JsonObject.of("wildcard", JsonObject.of(RequestV2MetricsV4Fields.URI, fullUri)));
        }
    }

    private static void addErrorKeysFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getErrorKeys())) {
            mustFilterList.add(JsonObject.of("terms", JsonObject.of(RequestV2MetricsV4Fields.ERROR_KEY, filter.getErrorKeys().toArray())));
        }
    }

    private static void addEntrypointIdsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getEntrypointIds())) {
            var termsFilter = JsonObject.of(
                "terms",
                JsonObject.of(RequestV2MetricsV4Fields.ENTRYPOINT_ID.v4Metrics(), filter.getEntrypointIds())
            );
            var fieldMissingFilter = JsonObject.of(
                "bool",
                JsonObject.of(
                    "must_not",
                    JsonObject.of("exists", JsonObject.of("field", RequestV2MetricsV4Fields.ENTRYPOINT_ID.v4Metrics()))
                )
            );
            mustFilterList.add(
                JsonObject.of("bool", JsonObject.of("should", JsonArray.of(termsFilter, fieldMissingFilter), "minimum_should_match", 1))
            );
        }
    }

    private static void addStatusesFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getStatuses())) {
            mustFilterList.add(JsonObject.of("terms", JsonObject.of(RequestV2MetricsV4Fields.STATUS, filter.getStatuses())));
        }
    }

    private static void addHttpMethodsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMethods())) {
            mustFilterList.add(
                buildV2AndV4Terms(RequestV2MetricsV4Fields.HTTP_METHOD, filter.getMethods().stream().map(HttpMethod::code).toList())
            );
        }
    }

    private static void addMcpMethodsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMcpMethods())) {
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.MCP_METHOD, filter.getMcpMethods()));
        }
    }

    private static void addPlansFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getPlanIds())) {
            mustFilterList.add(buildV2AndV4Terms(RequestV2MetricsV4Fields.PLAN_ID, filter.getPlanIds()));
        }
    }

    private static void addApplicationsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApplicationIds())) {
            mustFilterList.add(buildV2AndV4Terms(RequestV2MetricsV4Fields.APPLICATION_ID, filter.getApplicationIds()));
        }
    }

    private static void addStatusRangesFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getStatusRanges())) {
            if (filter.getStatusRanges().size() == 1) {
                var range = filter.getStatusRanges().getFirst();
                mustFilterList.add(StatusCodeGroups.rangeForBounds(RequestV2MetricsV4Fields.STATUS, range.getGte(), range.getLte()));
            } else {
                var ranges = new JsonArray();
                for (var range : filter.getStatusRanges()) {
                    ranges.add(StatusCodeGroups.rangeForBounds(RequestV2MetricsV4Fields.STATUS, range.getGte(), range.getLte()));
                }
                mustFilterList.add(JsonObject.of("bool", JsonObject.of("should", ranges, "minimum_should_match", 1)));
            }
        }
    }

    private static void addStatusCodeGroupsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getStatusCodeGroups())) {
            mustFilterList.add(StatusCodeGroups.shouldForGroups(RequestV2MetricsV4Fields.STATUS, filter.getStatusCodeGroups()));
        }
    }

    private static void addLlmProxyModelsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getLlmProxyModels())) {
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.LLM_PROXY_MODEL, filter.getLlmProxyModels()));
        }
    }

    private static void addLlmProxyProvidersFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getLlmProxyProviders())) {
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.LLM_PROXY_PROVIDER, filter.getLlmProxyProviders()));
        }
    }

    private static void addMcpProxyToolsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMcpProxyTools())) {
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.MCP_PROXY_TOOL, filter.getMcpProxyTools()));
        }
    }

    private static void addMcpProxyResourcesFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMcpProxyResources())) {
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.MCP_PROXY_RESOURCE, filter.getMcpProxyResources()));
        }
    }

    private static void addMcpProxyPromptsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMcpProxyPrompts())) {
            mustFilterList.add(buildV4Terms(RequestV2MetricsV4Fields.MCP_PROXY_PROMPT, filter.getMcpProxyPrompts()));
        }
    }

    private static void addNativeConnectionStatusesFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getNativeConnectionStatuses())) {
            // Native Kafka connection status only exists in v4-metrics documents, under additional-metrics
            mustFilterList.add(
                JsonObject.of("terms", JsonObject.of(NATIVE_CONNECTION_STATUS_FIELD, filter.getNativeConnectionStatuses().toArray()))
            );
        }
    }

    /**
     * Translates requested failure origins into boolean predicates over the error key and the
     * native connection status, mirroring the classification order of
     * {@link NativeFailureOriginRules} (client keys -> broker keys/prefixes -> internal -> phase
     * fallback). Requested origins are OR'ed together.
     */
    private static void addFailureOriginsFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (CollectionUtils.isEmpty(filter.getFailureOrigins())) {
            return;
        }
        var perOrigin = new ArrayList<JsonObject>();
        for (String origin : filter.getFailureOrigins()) {
            var predicate = failureOriginPredicate(origin);
            if (predicate != null) {
                perOrigin.add(predicate);
            }
        }
        if (!perOrigin.isEmpty()) {
            // Native-document guard: failure origins are a native Kafka concept — without it,
            // NONE/UNKNOWN would match successful/errored HTTP rows in a mixed scope.
            mustFilterList.add(
                JsonObject.of(
                    "bool",
                    JsonObject.of(
                        "must",
                        JsonArray.of(hasNativeConnectionStatus()),
                        "should",
                        new JsonArray(perOrigin),
                        "minimum_should_match",
                        1
                    )
                )
            );
        }
    }

    private static JsonObject failureOriginPredicate(String origin) {
        return switch (origin) {
            case "NONE" -> JsonObject.of(
                "bool",
                JsonObject.of("must_not", JsonArray.of(hasErrorKey(), internalStatus(), knownFailureSide()))
            );
            case "CLIENT_TO_GATEWAY" -> explicitSideOrHeuristic(
                NativeFailureOriginRules.FAILURE_SIDE_DOWNSTREAM,
                JsonObject.of(
                    "bool",
                    JsonObject.of(
                        "must",
                        JsonArray.of(hasErrorKey()),
                        "should",
                        JsonArray.of(
                            clientSideKeys(),
                            // Unclassified key reported during connection establishment: client side.
                            JsonObject.of(
                                "bool",
                                JsonObject.of(
                                    "must",
                                    JsonArray.of(connectionErrorStatus()),
                                    "must_not",
                                    JsonArray.of(brokerSideKeys(), internalErrorKey(), internalStatus())
                                )
                            )
                        ),
                        "minimum_should_match",
                        1
                    )
                )
            );
            case "GATEWAY_TO_BROKER" -> explicitSideOrHeuristic(
                NativeFailureOriginRules.FAILURE_SIDE_UPSTREAM,
                JsonObject.of("bool", JsonObject.of("must", JsonArray.of(hasErrorKey(), brokerSideKeys())))
            );
            case "GATEWAY_INTERNAL" -> explicitSideOrHeuristic(
                NativeFailureOriginRules.FAILURE_SIDE_INTERNAL,
                JsonObject.of(
                    "bool",
                    JsonObject.of(
                        "should",
                        JsonArray.of(
                            JsonObject.of(
                                "bool",
                                JsonObject.of(
                                    "must",
                                    JsonArray.of(
                                        hasErrorKey(),
                                        JsonObject.of(
                                            "bool",
                                            JsonObject.of(
                                                "should",
                                                JsonArray.of(internalErrorKey(), internalStatus()),
                                                "minimum_should_match",
                                                1
                                            )
                                        )
                                    ),
                                    "must_not",
                                    JsonArray.of(clientSideKeys(), brokerSideKeys())
                                )
                            ),
                            JsonObject.of(
                                "bool",
                                JsonObject.of("must", JsonArray.of(internalStatus()), "must_not", JsonArray.of(hasErrorKey()))
                            )
                        ),
                        "minimum_should_match",
                        1
                    )
                )
            );
            // Heuristically undecidable AND no explicit side written by the gateway.
            case "UNKNOWN" -> JsonObject.of(
                "bool",
                JsonObject.of(
                    "must",
                    JsonArray.of(hasErrorKey()),
                    "must_not",
                    JsonArray.of(
                        knownFailureSide(),
                        clientSideKeys(),
                        brokerSideKeys(),
                        internalErrorKey(),
                        internalStatus(),
                        connectionErrorStatus()
                    )
                )
            );
            default -> null;
        };
    }

    /**
     * The gateway-written failure side is authoritative when present; documents without one — or
     * with a side value this version does not know — fall back to the heuristic predicate,
     * mirroring the display path's fallback for unrecognized sides.
     */
    private static JsonObject explicitSideOrHeuristic(String side, JsonObject heuristic) {
        var explicit = JsonObject.of("term", JsonObject.of(FAILURE_SIDE_FIELD, side));
        var withoutKnownSide = JsonObject.of(
            "bool",
            JsonObject.of("must", JsonArray.of(heuristic), "must_not", JsonArray.of(knownFailureSide()))
        );
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(explicit, withoutKnownSide), "minimum_should_match", 1));
    }

    /** Matches documents whose failure side is one of the values this version can route explicitly. */
    private static JsonObject knownFailureSide() {
        return JsonObject.of(
            "terms",
            JsonObject.of(
                FAILURE_SIDE_FIELD,
                new JsonArray(
                    List.of(
                        NativeFailureOriginRules.FAILURE_SIDE_DOWNSTREAM,
                        NativeFailureOriginRules.FAILURE_SIDE_UPSTREAM,
                        NativeFailureOriginRules.FAILURE_SIDE_INTERNAL
                    )
                )
            )
        );
    }

    private static JsonObject hasNativeConnectionStatus() {
        return JsonObject.of("exists", JsonObject.of("field", NATIVE_CONNECTION_STATUS_FIELD));
    }

    private static JsonObject hasErrorKey() {
        return JsonObject.of("exists", JsonObject.of("field", RequestV2MetricsV4Fields.ERROR_KEY));
    }

    private static JsonObject clientSideKeys() {
        return JsonObject.of(
            "terms",
            JsonObject.of(RequestV2MetricsV4Fields.ERROR_KEY, NativeFailureOriginRules.CLIENT_SIDE_ERROR_KEYS.toArray())
        );
    }

    private static JsonObject brokerSideKeys() {
        var branches = new ArrayList<JsonObject>();
        branches.add(
            JsonObject.of(
                "terms",
                JsonObject.of(RequestV2MetricsV4Fields.ERROR_KEY, NativeFailureOriginRules.BROKER_SIDE_ERROR_KEYS.toArray())
            )
        );
        for (String prefix : NativeFailureOriginRules.BROKER_SIDE_ERROR_KEY_PREFIXES) {
            branches.add(JsonObject.of("prefix", JsonObject.of(RequestV2MetricsV4Fields.ERROR_KEY, prefix)));
        }
        return JsonObject.of("bool", JsonObject.of("should", new JsonArray(branches), "minimum_should_match", 1));
    }

    private static JsonObject internalErrorKey() {
        return JsonObject.of("term", JsonObject.of(RequestV2MetricsV4Fields.ERROR_KEY, NativeFailureOriginRules.UNKNOWN_SERVER_ERROR_KEY));
    }

    private static JsonObject internalStatus() {
        return JsonObject.of("term", JsonObject.of(NATIVE_CONNECTION_STATUS_FIELD, NativeFailureOriginRules.INTERNAL_ERROR_STATUS));
    }

    private static JsonObject connectionErrorStatus() {
        return JsonObject.of("term", JsonObject.of(NATIVE_CONNECTION_STATUS_FIELD, NativeFailureOriginRules.CONNECTION_ERROR_STATUS));
    }

    private static void addResponseTimeRangesFilter(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getResponseTimeRanges())) {
            var responseTimeRanges = new ArrayList<JsonObject>();
            filter
                .getResponseTimeRanges()
                .forEach(responseTimeRange -> {
                    var rangeJsonObject = new JsonObject();
                    if (responseTimeRange.getFrom() != null) {
                        rangeJsonObject.put("gte", responseTimeRange.getFrom());
                    }
                    if (responseTimeRange.getTo() != null) {
                        rangeJsonObject.put("lte", new Date(responseTimeRange.getTo()));
                    }

                    responseTimeRanges.add(
                        JsonObject.of("range", JsonObject.of(RequestV2MetricsV4Fields.GATEWAY_RESPONSE_TIME.v2Request(), rangeJsonObject))
                    );
                    responseTimeRanges.add(
                        JsonObject.of("range", JsonObject.of(RequestV2MetricsV4Fields.GATEWAY_RESPONSE_TIME.v4Metrics(), rangeJsonObject))
                    );
                });

            mustFilterList.add(buildShould(responseTimeRanges));
        }
    }

    private static void addFromAndToFilters(MetricsQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (filter.getFrom() != null || filter.getTo() != null) {
            var timestampJsonObject = new JsonObject();
            if (filter.getFrom() != null) {
                timestampJsonObject.put("gte", filter.getFrom());
            }
            if (filter.getTo() != null) {
                timestampJsonObject.put("lte", new Date(filter.getTo()));
            }
            mustFilterList.add(JsonObject.of("range", JsonObject.of(RequestV2MetricsV4Fields.TIMESTAMP, timestampJsonObject)));
        }
    }

    private static JsonArray buildSort() {
        return JsonArray.of(
            JsonObject.of(RequestV2MetricsV4Fields.TIMESTAMP, JsonObject.of("order", "desc")),
            JsonObject.of(RequestV2MetricsV4Fields.REQUEST_ID.v4Metrics(), JsonObject.of("order", "asc", "unmapped_type", "keyword"))
        );
    }

    private static JsonObject buildV4Terms(RequestV2MetricsV4Fields.Field field, Collection<?> value) {
        return JsonObject.of("terms", JsonObject.of(field.v4Metrics(), value.toArray()));
    }

    private static JsonObject buildV2AndV4Terms(RequestV2MetricsV4Fields.Field field, Collection<?> value) {
        var terms = new ArrayList<JsonObject>();
        terms.add(JsonObject.of("terms", JsonObject.of(field.v2Request(), value.toArray())));
        terms.add(JsonObject.of("terms", JsonObject.of(field.v4Metrics(), value.toArray())));
        return buildShould(terms);
    }

    private static JsonObject buildShould(List<JsonObject> terms) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(terms.toArray())));
    }
}
