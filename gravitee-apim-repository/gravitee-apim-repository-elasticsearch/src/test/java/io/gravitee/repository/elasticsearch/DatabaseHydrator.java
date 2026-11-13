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
package io.gravitee.repository.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.elasticsearch.configuration.IndexMode;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;

public class DatabaseHydrator {

    /** How far back {@code dateDaysAgo} and {@code indexNameDaysAgo} reach: past the 30-day windows the tests query. */
    private static final int MAX_DAYS_AGO = 60;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    Client client;
    FreeMarkerComponent freeMarkerComponent;
    AnatlyticsDatabase anatlyticsDatabase;
    private final TimeProvider timeProvider;
    private final IndexMode indexMode;
    private final WebClient webClient;
    private final RepositoryConfiguration configuration;

    public DatabaseHydrator(
        Client client,
        FreeMarkerComponent freeMarkerComponent,
        AnatlyticsDatabase anatlyticsDatabase,
        TimeProvider timeProvider,
        IndexMode indexMode,
        WebClient webClient,
        RepositoryConfiguration configuration
    ) {
        this.client = client;
        this.freeMarkerComponent = freeMarkerComponent;
        this.anatlyticsDatabase = anatlyticsDatabase;
        this.timeProvider = timeProvider;
        this.indexMode = indexMode;
        this.webClient = webClient;
        this.configuration = configuration;
    }

    @PostConstruct
    public void indexSampleData() {
        List<String> indexTypes = List.of(
            "health",
            "request",
            "monitor",
            "log",
            "v4-log",
            "v4-metrics",
            "v4-message-log",
            "v4-message-metrics",
            "event-metrics",
            "decisions"
        );
        if (indexMode == IndexMode.DATA_STREAM && "7".equals(anatlyticsDatabase.getDatabaseMajorVersion())) {
            throw new IllegalStateException("The data stream fixtures are written for Elasticsearch 8 and later, and OpenSearch.");
        }
        final Completable sampleData = indexMode == IndexMode.DATA_STREAM
            ? loadDataStreams(indexTypes)
            : Single.defer(() -> client.bulk(prepareData(indexTypes), true)).ignoreElement();
        createTemplate(indexTypes).andThen(sampleData).blockingAwait();
    }

    private Completable loadDataStreams(List<String> types) {
        return Flowable.fromIterable(types).concatMapCompletable(type -> {
            final String dataStream = "gravitee-" + type;
            return Flowable.fromIterable(DataStreamBulk.of(render(type), dataStream)).concatMapCompletable(bulk ->
                client
                    .bulk(Buffer.buffer(bulk), true)
                    .doOnSuccess(response -> {
                        if (Boolean.TRUE.equals(response.getErrors())) {
                            throw new IllegalStateException("The " + dataStream + " data stream refused sample documents: " + bulk);
                        }
                    })
                    .ignoreElement()
                    .andThen(rollover(dataStream))
            );
        });
    }

    private Completable rollover(String dataStream) {
        return Completable.defer(() ->
            Completable.fromCompletionStage(
                webClient
                    .requestAbs(HttpMethod.POST, configuration.getEndpoints().getFirst().getUrl() + "/" + dataStream + "/_rollover")
                    .basicAuthentication(configuration.getUsername(), configuration.getPassword())
                    .send()
                    .map(response -> {
                        if (response.statusCode() != 200) {
                            throw new IllegalStateException("Cannot roll " + dataStream + " over: " + response.bodyAsString());
                        }
                        return response;
                    })
                    .toCompletionStage()
            )
        );
    }

    private Completable createTemplate(List<String> types) {
        return Flowable.fromIterable(types)
            .map(type -> {
                String indexName = "gravitee-" + type;
                Map<String, Object> data = Map.ofEntries(
                    Map.entry("numberOfShards", 5),
                    Map.entry("numberOfReplicas", 1),
                    Map.entry("refreshInterval", "1s"),
                    Map.entry("indexName", indexName),
                    Map.entry("indexBody", true)
                );
                String filename;
                if (this.anatlyticsDatabase.getDatabaseType() == AnatlyticsDatabase.DatabaseType.ELASTICSEARCH) {
                    filename = "es" + this.anatlyticsDatabase.getDatabaseMajorVersion() + "x";
                } else {
                    filename = "opensearch";
                }
                filename += "/mapping/index-template-" + type + ".ftl";
                final String template = freeMarkerComponent.generateFromTemplate(filename, data);
                return Map.entry(
                    indexName,
                    indexMode == IndexMode.DATA_STREAM && !isDataStream(indexName) ? asDataStreamTemplate(template) : template
                );
            })
            .flatMapCompletable(entry -> {
                if (this.anatlyticsDatabase.getDatabaseType() == AnatlyticsDatabase.DatabaseType.ELASTICSEARCH) {
                    if (this.anatlyticsDatabase.getDatabaseMajorVersion().equals("7") && !isDataStream(entry.getKey())) {
                        return client.putTemplate(entry.getKey(), entry.getValue());
                    }
                    return client.putIndexTemplate(entry.getKey(), entry.getValue());
                } else {
                    //OpenSearch
                    return client.putIndexTemplate(entry.getKey(), entry.getValue());
                }
            });
    }

    private List<Buffer> prepareData(List<String> types) {
        return types.stream().map(this::render).map(Buffer::buffer).toList();
    }

    private String render(String type) {
        Map<String, Object> data = new HashMap<>();

        if (isDataStream(type)) {
            data.putIfAbsent("index", indexTemplate(type, this.timeProvider.getTodayWithDot()));
            this.timeProvider.setTimestamps(data);
        } else {
            data = Map.ofEntries(
                Map.entry("dateToday", this.timeProvider.getDateToday()),
                Map.entry("dateYesterday", this.timeProvider.getDateYesterday()),
                Map.entry("dateTimeToday", this.timeProvider.getDateTimeToday()),
                Map.entry("dateTimeYesterday", this.timeProvider.getDateTimeYesterday()),
                Map.entry("trafficDateTimeToday", this.timeProvider.getTrafficDateTimeToday()),
                Map.entry("trafficDateTimeYesterday", this.timeProvider.getTrafficDateTimeYesterday()),
                Map.entry("indexNameToday", indexTemplate(type, this.timeProvider.getTodayWithDot())),
                Map.entry("indexNameTodayEntrypoint", indexTemplate(type, this.timeProvider.getTodayWithDot(), "entrypoint")),
                Map.entry("indexNameTodayEndpoint", indexTemplate(type, this.timeProvider.getTodayWithDot(), "endpoint")),
                Map.entry("indexNameYesterday", indexTemplate(type, this.timeProvider.getYesterdayWithDot())),
                Map.entry("indexNameYesterdayEntrypoint", indexTemplate(type, this.timeProvider.getYesterdayWithDot(), "entrypoint")),
                Map.entry("indexNameYesterdayEndpoint", indexTemplate(type, this.timeProvider.getYesterdayWithDot(), "endpoint")),
                Map.entry("dateDaysAgo", daysAgo(this.timeProvider::dateDaysAgo)),
                Map.entry("indexNameDaysAgo", daysAgo(days -> indexTemplate(type, this.timeProvider.dateWithDotDaysAgo(days))))
            );
        }
        var filename = type + ".ftl";
        return freeMarkerComponent.generateFromTemplate(filename, data);
    }

    private static String asDataStreamTemplate(String template) {
        try {
            final ObjectNode json = (ObjectNode) MAPPER.readTree(template);
            json.putObject("data_stream");
            return MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read the index template: " + template, e);
        }
    }

    /** Keyed by the number of days, as a string: {@code ${dateDaysAgo["11"]}} in a template. */
    private static Map<String, String> daysAgo(IntFunction<String> format) {
        return IntStream.rangeClosed(0, MAX_DAYS_AGO).boxed().collect(Collectors.toMap(String::valueOf, format::apply));
    }

    private static boolean isDataStream(String typeOrIndexName) {
        return Arrays.stream(Type.TYPES).filter(Type::isDataStream).map(Type::getType).anyMatch(typeOrIndexName::contains);
    }

    private String indexTemplate(String type, String date) {
        return indexTemplate(type, date, null);
    }

    private String indexTemplate(String type, String date, String suffix) {
        if (StringUtils.isNotEmpty(suffix)) {
            return String.format("\"_index\" : \"gravitee-%s-%s-%s\"", type, date, suffix);
        }
        return String.format("\"_index\" : \"gravitee-%s-%s\"", type, date);
    }
}
