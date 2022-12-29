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
package io.gravitee.reporter.elasticsearch.indexer;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class AbstractIndexerTest {

    @InjectMocks
    private AbstractIndexer indexer = new TestIndexer();

    @Test
    public void shouldIndexReportable_validRemoteAddress_ipv4() {
        Metrics metrics = Metrics.on(Instant.now().toEpochMilli()).build();
        metrics.setRemoteAddress("72.16.254.1");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        assertThat(data.getMap()).containsEntry("remoteAddress", "72.16.254.1");
    }

    @Test
    public void shouldIndexReportable_validRemoteAddress_ipv6() {
        Metrics metrics = Metrics.on(Instant.now().toEpochMilli()).build();
        metrics.setRemoteAddress("2001:db8:0:1234:0:567:8:1");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        assertThat(data.getMap()).containsEntry("remoteAddress", "2001:db8:0:1234:0:567:8:1");
    }

    @Test
    public void shouldIndexReportable_invalidRemoteAddress() {
        Metrics metrics = Metrics.on(Instant.now().toEpochMilli()).build();
        metrics.setRemoteAddress("remoteAddress");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        assertThat(data.getMap()).containsEntry("remoteAddress", "0.0.0.0");
    }

    private static class TestIndexer extends AbstractIndexer {

        @Override
        protected Buffer generateData(String templateName, Map<String, Object> data) {
            return JsonObject.mapFrom(data.get("metrics")).toBuffer();
        }
    }
}
