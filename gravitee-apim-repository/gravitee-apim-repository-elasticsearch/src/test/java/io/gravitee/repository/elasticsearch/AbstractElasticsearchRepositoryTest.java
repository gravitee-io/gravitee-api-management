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
package io.gravitee.repository.elasticsearch;

import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.repository.elasticsearch.embedded.ElasticsearchNode;
import io.gravitee.repository.elasticsearch.spring.ElasticsearchRepositoryConfigurationTest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ElasticsearchRepositoryConfigurationTest.class })
public abstract class AbstractElasticsearchRepositoryTest {

    /**
     * ES node.
     */
    @Autowired
    private ElasticsearchNode embeddedNode;

    /**
     * Templating tool.
     */
    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    @Before
    public void setUp() throws Exception {
        this.indexSampleData();
    }

    /**
     * Perform bulk request
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void indexSampleData() throws InterruptedException, ExecutionException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse

        final Map<String, Object> data = new HashMap<>();
        final Instant now = Instant.now();
        data.put("indexName", "gravitee");
        data.put("indexDateToday", Date.from(now));
        data.put("indexDateYesterday", Date.from(now.minus(1, ChronoUnit.DAYS)));
        data.put("numberOfShards", 5);
        data.put("numberOfReplicas", 1);

        this.embeddedNode.getNode()
            .client()
            .admin()
            .indices()
            .putTemplate(
                new PutIndexTemplateRequest("gravitee")
                .source(this.freeMarkerComponent.generateFromTemplate("index-template-es-5x.ftl", data), XContentType.JSON)
            )
            .get();

        final String body = this.freeMarkerComponent.generateFromTemplate("bulk.json", data);
        String lines[] = body.split("\\r?\\n");
        for (int i = 0; i < lines.length - 1; i += 2) {
            String index = lines[i];
            String value = lines[i + 1];

            try {
                JsonNode node = mapper.readTree(index);
                JsonNode indexNode = node.get("index");

                this.embeddedNode.getNode()
                    .client()
                    .prepareIndex(indexNode.get("_index").asText(), indexNode.get("_type").asText(), indexNode.get("_id").asText())
                    .setSource(value, XContentType.JSON)
                    .setRefreshPolicy(IMMEDIATE)
                    .get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
