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
package io.gravitee.apim.infra.domain_service.analytics_engine.definition;

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.*;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Pins the catalog fields that identify a person or a client: a query using one of them, as filter or facet, reads
 * the requests themselves, never the aggregates alone. A field added to the catalog fails here until it is classified.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class IdentifyingFieldsCatalogTest {

    private static final Set<FilterSpec.Name> IDENTIFYING = EnumSet.of(CONSUMER_IP, NATIVE_CLIENT_ID, AUTHZ_SUBJECT_ID);

    /** A device identifier read by EDGE_MANAGER for Edge governance: counted with the aggregates by decision. */
    private static final Set<FilterSpec.Name> AGGREGATES_BY_DECISION = EnumSet.of(EDGE_CLIENT);

    /** Reviewed: none holds a person or a client identifier. */
    private static final Set<FilterSpec.Name> NOT_IDENTIFYING = EnumSet.of(
        API,
        APPLICATION,
        PLAN,
        API_PRODUCT,
        API_TYPE,
        GATEWAY,
        TENANT,
        ZONE,
        HOST,
        ENTRYPOINT,
        HTTP_METHOD,
        HTTP_STATUS_CODE_GROUP,
        HTTP_STATUS,
        HTTP_PATH,
        HTTP_PATH_MAPPING,
        HTTP_ENDPOINT_RESPONSE_TIME,
        HTTP_GATEWAY_LATENCY,
        HTTP_GATEWAY_RESPONSE_TIME,
        HTTP_REQUEST_CONTENT_LENGTH,
        HTTP_RESPONSE_CONTENT_LENGTH,
        HTTP_USER_AGENT_OS_NAME,
        HTTP_USER_AGENT_DEVICE,
        GEO_IP_COUNTRY,
        GEO_IP_REGION,
        GEO_IP_CITY,
        GEO_IP_CONTINENT,
        MESSAGE_CONNECTOR_TYPE,
        MESSAGE_CONNECTOR_ID,
        MESSAGE_OPERATION_TYPE,
        MESSAGE_SIZE,
        MESSAGE_COUNT,
        MESSAGE_ERROR_COUNT,
        LLM_PROXY_MODEL,
        LLM_PROXY_PROVIDER,
        MCP_PROXY_METHOD,
        MCP_PROXY_TOOL,
        MCP_PROXY_RESOURCE,
        MCP_PROXY_PROMPT,
        EDGE_PROVIDER,
        EDGE_PROCESS,
        EDGE_TYPE,
        EDGE_VERSION,
        EDGE_MODEL,
        EDGE_TOOL,
        NATIVE_CONNECTION_STATUS,
        NATIVE_FAILURE_SIDE,
        NATIVE_CLIENT_SOFTWARE_NAME,
        NATIVE_TOPIC,
        NATIVE_OPERATION,
        AUTHZ_DECISION,
        AUTHZ_OPERATION,
        AUTHZ_SEARCH_TYPE,
        AUTHZ_STATUS,
        AUTHZ_CALLER,
        AUTHZ_ACTION,
        AUTHZ_RESOURCE_ID,
        AUTHZ_REASON,
        AUTHZ_PDP
    );

    private final AnalyticsDefinitionYAMLQueryService catalog = new AnalyticsDefinitionYAMLQueryService();

    @Test
    void should_classify_every_analytics_filter_of_the_catalog() {
        var analyticsFilters = catalog.getFilters(Set.of(Signal.ANALYTICS)).stream().map(FilterSpec::name).collect(toSet());

        assertThat(analyticsFilters).allSatisfy(name -> assertThat(classified()).as("classification of %s", name).contains(name));
    }

    @Test
    void should_classify_the_field_of_every_facet_of_the_catalog() {
        var facetFields = catalog
            .getApis()
            .stream()
            .map(ApiSpec::name)
            .flatMap(api -> catalog.getMetrics(api).stream())
            .map(MetricSpec::name)
            .flatMap(metric -> catalog.getFacets(metric).stream())
            .map(facet -> FilterSpec.Name.valueOf(facet.name().name()))
            .collect(toSet());

        assertThat(facetFields).allSatisfy(name -> assertThat(classified()).as("classification of %s", name).contains(name));
    }

    @Test
    void should_read_the_requests_for_the_identifying_fields_only() {
        assertThat(Arrays.stream(FilterSpec.Name.values()).filter(FilterSpec.Name::isIdentifying)).containsExactlyInAnyOrderElementsOf(
            IDENTIFYING
        );
        assertThat(
            Arrays.stream(FacetSpec.Name.values()).filter(FacetSpec.Name::isIdentifying).map(Enum::name)
        ).containsExactlyInAnyOrderElementsOf(IDENTIFYING.stream().map(Enum::name).toList());
    }

    private static Set<FilterSpec.Name> classified() {
        return Stream.of(IDENTIFYING, AGGREGATES_BY_DECISION, NOT_IDENTIFYING).flatMap(Set::stream).collect(toSet());
    }
}
