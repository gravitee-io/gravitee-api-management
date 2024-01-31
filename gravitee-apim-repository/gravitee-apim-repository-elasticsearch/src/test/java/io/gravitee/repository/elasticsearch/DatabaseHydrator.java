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

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.buffer.Buffer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class DatabaseHydrator {

    private static final DateTimeFormatter FORMATTER_WITH_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FORMATTER_WITH_DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());

    Client client;
    FreeMarkerComponent freeMarkerComponent;
    String elasticMajorVersion;

    public DatabaseHydrator(Client client, FreeMarkerComponent freeMarkerComponent, String elasticsearchVersion) {
        this.client = client;
        this.freeMarkerComponent = freeMarkerComponent;
        this.elasticMajorVersion = elasticsearchVersion.split("\\.")[0];
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
            "v4-message-metrics"
        );
        createTemplate(indexTypes).andThen(Single.defer(() -> client.bulk(prepareData(indexTypes), true))).ignoreElement().blockingAwait();
    }

    private Completable createTemplate(List<String> types) {
        return Flowable
            .fromIterable(types)
            .map(type -> {
                String indexName = "gravitee-" + type;
                Map<String, Object> data = Map.ofEntries(
                    Map.entry("numberOfShards", 5),
                    Map.entry("numberOfReplicas", 1),
                    Map.entry("refreshInterval", "1s"),
                    Map.entry("indexName", indexName)
                );
                var filename = "es" + elasticMajorVersion + "x/mapping/index-template-" + type + ".ftl";
                return Map.entry(indexName, freeMarkerComponent.generateFromTemplate(filename, data));
            })
            .flatMapCompletable(entry -> {
                if (elasticMajorVersion.equals("7")) {
                    return client.putTemplate(entry.getKey(), entry.getValue());
                }
                return client.putIndexTemplate(entry.getKey(), entry.getValue());
            });
    }

    private List<Buffer> prepareData(List<String> types) {
        final Instant now = Instant.now();
        final Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        final String dateToday = FORMATTER_WITH_DASH.format(now);
        final String dateYesterday = FORMATTER_WITH_DASH.format(yesterday);

        final String todayWithDot = FORMATTER_WITH_DOT.format(now);
        final String yesterdayWithDot = FORMATTER_WITH_DOT.format(yesterday);

        return types
            .stream()
            .map(type -> {
                Map<String, Object> data = Map.ofEntries(
                    Map.entry("dateToday", dateToday),
                    Map.entry("dateYesterday", dateYesterday),
                    Map.entry("indexNameToday", indexTemplate(type, todayWithDot)),
                    Map.entry("indexNameTodayEntrypoint", indexTemplate(type, todayWithDot, "entrypoint")),
                    Map.entry("indexNameTodayEndpoint", indexTemplate(type, todayWithDot, "endpoint")),
                    Map.entry("indexNameYesterday", indexTemplate(type, yesterdayWithDot)),
                    Map.entry("indexNameYesterdayEntrypoint", indexTemplate(type, yesterdayWithDot, "entrypoint")),
                    Map.entry("indexNameYesterdayEndpoint", indexTemplate(type, yesterdayWithDot, "endpoint"))
                );
                var filename = type + ".ftl";
                return freeMarkerComponent.generateFromTemplate(filename, data);
            })
            .map(Buffer::buffer)
            .toList();
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
