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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageFacetsQueryAdapterTest extends AbstractQueryAdapterTest {

    private final MessageFacetsQueryAdapter adapter = new MessageFacetsQueryAdapter();

    private FacetsQuery sortedQuery(List<Facet> facets, Integer limit) {
        return new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(
                    Metric.MESSAGES,
                    Set.of(Measure.COUNT),
                    List.of(new MetricMeasuresQuery.Sort(Measure.COUNT, MetricMeasuresQuery.Sort.Order.DESC))
                )
            ),
            facets,
            limit,
            List.of()
        );
    }

    private FacetsQuery query(List<Facet> facets, Integer limit) {
        return new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT))),
            facets,
            limit,
            List.of()
        );
    }

    @Test
    void should_bucket_on_the_requested_dimension_and_hang_the_measures_off_it() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of(Facet.MESSAGE_OPERATION_TYPE), 10), Set.of("req-1")));

        var bucket = json.at("/aggs").elements().next();
        assertThat(bucket.at("/terms/field").asText()).isEqualTo("operation");
        assertThat(bucket.at("/terms/size").asInt()).isEqualTo(10);
        assertThat(bucket.at("/aggs").isEmpty()).isFalse();
    }

    /**
     * Asserted by aggregation name, not by position: the response side resolves each child strictly
     * by name, so walking the tree positionally would pass even if every level were keyed the same.
     */
    @Test
    void should_nest_dimensions_outermost_first() throws JsonProcessingException {
        var facets = List.of(Facet.MESSAGE_CONNECTOR_TYPE, Facet.MESSAGE_OPERATION_TYPE);

        var json = JSON.readTree(adapter.adapt(query(facets, null), Set.of("req-1")));

        var outer = json.at("/aggs/MESSAGES#MESSAGE_CONNECTOR_TYPE");
        assertThat(outer.isMissingNode()).isFalse();
        assertThat(outer.at("/terms/field").asText()).isEqualTo("connector-type");

        var inner = outer.at("/aggs/MESSAGES#MESSAGE_OPERATION_TYPE");
        assertThat(inner.isMissingNode()).isFalse();
        assertThat(inner.at("/terms/field").asText()).isEqualTo("operation");

        // Each level is keyed by its own facet: the parent's name must not reappear underneath it.
        assertThat(outer.at("/aggs").has("MESSAGES#MESSAGE_CONNECTOR_TYPE")).isFalse();
        // Measures hang off the innermost bucket, not the outer one.
        assertThat(inner.at("/aggs").isEmpty()).isFalse();
    }

    /**
     * Elasticsearch rejects the whole search when an order path names an aggregation absent from the
     * level declaring it, and the measures only exist on the leaf.
     */
    @Test
    void should_order_and_size_only_the_innermost_bucket() throws JsonProcessingException {
        var facets = List.of(Facet.MESSAGE_CONNECTOR_TYPE, Facet.MESSAGE_OPERATION_TYPE);

        var json = JSON.readTree(adapter.adapt(sortedQuery(facets, 5), Set.of("req-1")));

        var outer = json.at("/aggs/MESSAGES#MESSAGE_CONNECTOR_TYPE");
        assertThat(outer.at("/terms").has("order")).isFalse();
        assertThat(outer.at("/terms").has("size")).isFalse();

        var inner = outer.at("/aggs/MESSAGES#MESSAGE_OPERATION_TYPE");
        assertThat(inner.at("/terms/order/MESSAGES#COUNT").asText()).isEqualTo("desc");
        assertThat(inner.at("/terms/size").asInt()).isEqualTo(5);
    }

    @Test
    void should_order_the_single_bucket_when_only_one_dimension_is_requested() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(sortedQuery(List.of(Facet.MESSAGE_OPERATION_TYPE), 5), Set.of("req-1")));

        var bucket = json.at("/aggs/MESSAGES#MESSAGE_OPERATION_TYPE");
        assertThat(bucket.at("/terms/order/MESSAGES#COUNT").asText()).isEqualTo("desc");
        assertThat(bucket.at("/terms/size").asInt()).isEqualTo(5);
    }

    /**
     * The message index carries no connection dimensions, so the second phase can only be narrowed
     * by the request ids the first phase resolved.
     */
    @Test
    void should_restrict_the_query_to_the_resolved_request_ids() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of(Facet.MESSAGE_OPERATION_TYPE), null), Set.of("req-1", "req-2")));

        var filters = json.at("/query/bool/filter").toString();
        assertThat(filters).contains("request-id").contains("req-1").contains("req-2");
    }

    @Test
    void should_leave_elasticsearch_its_default_size_when_no_limit_is_asked_for() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of(Facet.MESSAGE_CONNECTOR_ID), null), Set.of("req-1")));

        var bucket = json.at("/aggs").elements().next();
        assertThat(bucket.at("/terms").has("size")).isFalse();
    }

    /**
     * Message documents carry their own {@code api-id}, so the API scope is applied on them directly
     * rather than being inherited from whatever request ids the first phase happened to resolve.
     */
    @Test
    void should_scope_the_message_query_to_the_api_itself() throws JsonProcessingException {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(new Filter(Filter.Name.API, Filter.Operator.EQ, "api-1")),
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT))),
            List.of(Facet.MESSAGE_OPERATION_TYPE),
            null,
            List.of()
        );

        var json = JSON.readTree(adapter.adapt(query, Set.of("req-1")));

        assertThat(json.at("/query/bool/filter").toString()).contains("api-id").contains("api-1");
    }

    @Test
    void should_aggregate_nothing_when_no_dimension_is_requested() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of(), null), Set.of("req-1")));

        assertThat(json.at("/aggs").isEmpty()).isTrue();
    }
}
