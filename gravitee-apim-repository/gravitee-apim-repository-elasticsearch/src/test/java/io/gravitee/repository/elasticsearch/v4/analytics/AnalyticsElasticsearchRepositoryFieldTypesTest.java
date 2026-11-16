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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.configuration.IndexMode;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Where the repository learns the type of a field: Elastic Cloud Serverless answers {@code _mapping/field} with a 410, so
 * data streams are asked through {@code _field_caps}. Mocked, because the containers answer both.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsElasticsearchRepositoryFieldTypesTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
    private static final String ENTRYPOINT_ID = "entrypoint-id";

    private Client client;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(Client.class);
        var emptyResponse = new SearchResponse();
        emptyResponse.setTimedOut(false);
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(emptyResponse));
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("keyword")));
        when(client.getFieldCapabilities(anyString(), anyString())).thenAnswer(invocation ->
            Single.just(Map.of(invocation.<String>getArgument(1), Set.of("keyword")))
        );
    }

    static Stream<Arguments> operationsReadingAFieldType() {
        return Stream.of(
            operation("requests count", cut ->
                cut.searchRequestsCount(QUERY_CONTEXT, new RequestsCountQuery(Optional.empty(), Optional.empty(), Optional.empty()))
            ),
            operation("average connection duration", cut ->
                cut.searchAverageConnectionDuration(
                    QUERY_CONTEXT,
                    new AverageConnectionDurationQuery(Optional.empty(), Optional.empty(), Optional.empty())
                )
            ),
            operation("response status ranges", cut ->
                cut.searchResponseStatusRanges(QUERY_CONTEXT, new ResponseStatusQueryCriteria(List.of(), null, null, List.of()))
            ),
            operation("message measures filtered on a connection dimension", cut ->
                cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery())
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operationsReadingAFieldType")
    void should_never_ask_the_field_mapping_of_data_streams(Consumer<AnalyticsElasticsearchRepository> operation) {
        operation.accept(repository(IndexMode.DATA_STREAM));

        verify(client, never()).getFieldTypes(anyString(), anyString());
        verify(client, atLeastOnce()).getFieldCapabilities(anyString(), anyString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operationsReadingAFieldType")
    void should_keep_asking_the_field_mapping_of_daily_indices(Consumer<AnalyticsElasticsearchRepository> operation) {
        operation.accept(repository(IndexMode.DAILY));

        verify(client, atLeastOnce()).getFieldTypes(anyString(), anyString());
        verify(client, never()).getFieldCapabilities(anyString(), anyString());
    }

    @Test
    void should_keep_asking_the_field_mapping_of_ilm_indices() {
        repository(IndexMode.ILM).searchRequestsCount(
            QUERY_CONTEXT,
            new RequestsCountQuery(Optional.empty(), Optional.empty(), Optional.empty())
        );

        verify(client, atLeastOnce()).getFieldTypes(anyString(), anyString());
        verify(client, never()).getFieldCapabilities(anyString(), anyString());
    }

    @ParameterizedTest
    @EnumSource(value = FieldType.class)
    void should_group_by_the_entrypoint_as_its_field_capabilities_report_it(FieldType fieldType) {
        when(client.getFieldCapabilities(anyString(), anyString())).thenReturn(Single.just(Map.of(ENTRYPOINT_ID, fieldType.types)));

        repository(IndexMode.DATA_STREAM).searchRequestsCount(
            QUERY_CONTEXT,
            new RequestsCountQuery(Optional.empty(), Optional.empty(), Optional.empty())
        );

        assertThat(lastQuery()).contains("\"field\":\"" + fieldType.groupedField + "\"");
    }

    enum FieldType {
        KEYWORD_EVERYWHERE(Set.of("keyword"), ENTRYPOINT_ID),
        TEXT_SOMEWHERE(Set.of("keyword", "text"), ENTRYPOINT_ID + ".keyword");

        private final Set<String> types;
        private final String groupedField;

        FieldType(Set<String> types, String groupedField) {
            this.types = types;
            this.groupedField = groupedField;
        }
    }

    private AnalyticsElasticsearchRepository repository(IndexMode mode) {
        var configuration = Mockito.mock(RepositoryConfiguration.class);
        when(configuration.hasCrossClusterMapping()).thenReturn(false);
        var indexNames = Mockito.mock(IndexNameGenerator.class);
        when(indexNames.getWildcardIndexName(any(), any(), any())).thenReturn("gravitee-v4-metrics");
        when(indexNames.getIndexName(any(), any(), anyLong(), anyLong(), any())).thenReturn("gravitee-v4-message-metrics");

        var mockClient = client;
        return new AnalyticsElasticsearchRepository(configuration) {
            {
                this.client = mockClient;
                this.indexNameGenerator = indexNames;
                this.indexMode = mode;
            }
        };
    }

    private String lastQuery() {
        var queries = ArgumentCaptor.forClass(String.class);
        verify(client, atLeastOnce()).search(anyString(), any(), queries.capture());
        return queries.getAllValues().getLast().replaceAll("\\s", "");
    }

    private static Arguments operation(String name, Consumer<AnalyticsElasticsearchRepository> operation) {
        return Arguments.of(Named.of(name, operation));
    }

    private static MeasuresQuery planFilteredQuery() {
        var from = Instant.now().minus(1, ChronoUnit.HOURS);
        return new MeasuresQuery(
            new TimeRange(from, from.plus(1, ChronoUnit.HOURS)),
            List.of(new Filter(Filter.Name.PLAN, Filter.Operator.IN, List.of("gold"))),
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT)))
        );
    }
}
