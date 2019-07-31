/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.elasticsearch.client;

import io.gravitee.elasticsearch.client.http.HttpClient;
import io.gravitee.elasticsearch.client.http.HttpClientConfiguration;
import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.elasticsearch.embedded.ElasticsearchNode;
import io.gravitee.elasticsearch.model.Health;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.Vertx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { HttpClientTest.TestConfig.class })
public class HttpClientTest {

    /**
     * Elasticsearch client.
     */
    @Autowired
    private Client client;

    /**
     * JSON mapper.
     */
    /*
    private final ObjectMapper mapper = new ObjectMapper();

    private final DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());
    private final DateTimeFormatter dtf2 = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    */

    @Test
    public void shouldGetVersion() throws InterruptedException, ExecutionException, IOException {
        Single<ElasticsearchInfo> info = client.getInfo();

        TestObserver<ElasticsearchInfo> observer = info.test();
        observer.awaitTerminalEvent();

        observer.assertNoErrors();
        observer.assertComplete();
        String esVersion = System.getProperty("elasticsearch");
        //Assert.assertEquals(esVersion, version + "x");
    }

    @Test
    public void shouldGetHealth() throws InterruptedException, ExecutionException, IOException {
        Single<Health> health = client.getClusterHealth();

        TestObserver<Health> observer = health.test();
        observer.awaitTerminalEvent();

        observer.assertNoErrors();
        observer.assertComplete();
        observer.assertValue(health1 -> "gravitee_test".equals(health1.getClusterName()));
    }

    /*
    @Test
    public void shouldSearch() throws ElasticsearchException, IOException {
        // do the call
        final Map<String, Object> parameter = new HashMap<>();
        parameter.put("indexDateToday", new Date());
        final String query = this.freeMarkerComponent.generateFromTemplate("esQuery.json", parameter);

        final Single<SearchResponse> result = this.client.search("_all", null, query);

        TestObserver<SearchResponse> observer = result.test();

        observer.assertNoErrors();
        observer.assertComplete();

        observer.assertValue(new Predicate<SearchResponse>() {
            @Override
            public boolean test(SearchResponse searchResponse) throws Exception {
                final Map<String, Object> data = new HashMap<>();
                data.put("took", searchResponse.getTook());
                data.put("index", "gravitee-" + dtf.format(new Date().toInstant()));
                data.put("today", dtf2.format(new Date().toInstant()));
                final String expectedResponse = freeMarkerComponent.generateFromTemplate("esResponse.json", data);
                final SearchResponse expectedEsSearchResponse = mapper.readValue(expectedResponse, SearchResponse.class);

                Assert.assertEquals(mapper.writeValueAsString(expectedEsSearchResponse), mapper.writeValueAsString(result));

                return true;
            }
        });
    }
    */

    @Configuration
    public static class TestConfig {

        @Bean
        public Vertx vertx() {
            return Vertx.vertx();
        }

        @Bean
        public Client reporter(HttpClientConfiguration clientConfiguration) {
            return new HttpClient(clientConfiguration);
        }

        @Bean
        public HttpClientConfiguration configuration(ElasticsearchNode elasticsearchNode) {
            HttpClientConfiguration elasticConfiguration = new HttpClientConfiguration();
            elasticConfiguration.setEndpoints(Collections.singletonList(new Endpoint("http://localhost:" + elasticsearchNode.getHttpPort())));
//            elasticConfiguration.setIngestPlugins(Arrays.asList("geoip"));
            return elasticConfiguration;
        }

        @Bean
        public ElasticsearchNode elasticsearchNode() {
            return new ElasticsearchNode();
        }
    }
}
