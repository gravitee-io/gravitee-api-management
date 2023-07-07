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
package io.gravitee.repository.elasticsearch.spring;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.vertx.core.buffer.Buffer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHydrator {

    Client client;
    FreeMarkerComponent freeMarkerComponent;
    String elasticsearchVersion;

    public DatabaseHydrator(Client client, FreeMarkerComponent freeMarkerComponent, String elasticsearchVersion) {
        this.client = client;
        this.freeMarkerComponent = freeMarkerComponent;
        this.elasticsearchVersion = elasticsearchVersion;
    }

    @PostConstruct
    public void indexSampleData() throws InterruptedException {
        String elasticMajorVersion = elasticsearchVersion.split("\\.")[0];

        final Map<String, Object> indexTemplateData = new HashMap<>();
        indexTemplateData.put("numberOfShards", 5);
        indexTemplateData.put("numberOfReplicas", 1);
        indexTemplateData.put("refreshInterval", "1s");

        if ("5".equals(elasticMajorVersion)) {
            indexTemplateData.put("indexName", "gravitee");
            client
                .putTemplate("gravitee", freeMarkerComponent.generateFromTemplate("es5x/mapping/index-template.ftl", indexTemplateData))
                .test()
                .await();
        } else {
            for (String type : new String[] { "request", "monitor", "health", "log" }) {
                indexTemplateData.put("indexName", "gravitee-" + type);
                client
                    .putTemplate(
                        "gravitee-" + type,
                        freeMarkerComponent.generateFromTemplate(
                            "es" + elasticMajorVersion + "x/mapping/index-template-" + type + ".ftl",
                            indexTemplateData
                        )
                    )
                    .test()
                    .await();
            }
        }

        final Instant now = Instant.now();
        final Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        DateTimeFormatter formatterWithDash = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
        DateTimeFormatter formatterWithDot = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());

        final Map<String, Object> bulkData = new HashMap<>();
        bulkData.put("dateToday", formatterWithDash.format(now));
        bulkData.put("dateYesterday", formatterWithDash.format(yesterday));

        String indexTemplate = "\"_index\" : \"gravitee-%s-%s\"";
        if ("5".equals(elasticMajorVersion) || "6".equals(elasticMajorVersion)) {
            indexTemplate += ", \"_type\" : \"%s\"";
        }

        String todayWithDot = formatterWithDot.format(now);
        bulkData.put("indexHealthToday", String.format(indexTemplate, "health", todayWithDot, "health"));
        bulkData.put("indexRequestToday", String.format(indexTemplate, "request", todayWithDot, "request"));
        bulkData.put("indexMonitorToday", String.format(indexTemplate, "monitor", todayWithDot, "monitor"));
        bulkData.put("indexLogToday", String.format(indexTemplate, "log", todayWithDot, "log"));

        String yesterdayWithDot = formatterWithDot.format(yesterday);
        bulkData.put("indexHealthYesterday", String.format(indexTemplate, "health", yesterdayWithDot, "health"));
        bulkData.put("indexRequestYesterday", String.format(indexTemplate, "request", yesterdayWithDot, "request"));
        bulkData.put("indexMonitorYesterday", String.format(indexTemplate, "monitor", yesterdayWithDot, "monitor"));
        bulkData.put("indexLogYesterday", String.format(indexTemplate, "log", yesterdayWithDot, "log"));

        final String body = freeMarkerComponent.generateFromTemplate("bulk.ftl", bulkData);
        client.bulk(Collections.singletonList(Buffer.buffer(body)), true).test().await();
    }
}
