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
package io.gravitee.reporter.file.formatter.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.Maps;
import io.gravitee.common.utils.UUID;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.vertx.core.buffer.Buffer;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonFormatterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldRenameField() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setRenameFields(Maps.<String, String>builder().put("application", "app").build());

        JsonFormatter<Metrics> formatter = new JsonFormatter<>(rules);

        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setApi("my-api");
        metrics.setApplication("my-application");
        metrics.setRemoteAddress("0.0.0.0");

        Buffer payload = formatter.format(metrics);

        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertFalse(node.has("application"));
        Assert.assertTrue(node.has("app"));
    }

    @Test
    public void shouldFilterField() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setExcludeFields(Collections.singleton("application"));

        JsonFormatter<Metrics> formatter = new JsonFormatter<>(rules);

        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setApi("my-api");
        metrics.setApplication("my-application");
        metrics.setRemoteAddress("0.0.0.0");

        Buffer payload = formatter.format(metrics);

        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertFalse(node.has("application"));
    }

    @Test
    public void shouldExcludeAllFields() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setExcludeFields(Collections.singleton("*"));

        JsonFormatter<Metrics> formatter = new JsonFormatter<>(rules);

        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setApi("my-api");
        metrics.setApplication("my-application");
        metrics.setRemoteAddress("0.0.0.0");

        Buffer payload = formatter.format(metrics);

        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertEquals(0, node.size());
    }

    @Test
    public void shouldExcludeAndIncludeSameField() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setExcludeFields(Collections.singleton("api"));
        rules.setIncludeFields(Collections.singleton("api"));

        JsonFormatter<Metrics> formatter = new JsonFormatter<>(rules);

        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setApi("my-api");
        metrics.setApplication("my-application");
        metrics.setRemoteAddress("0.0.0.0");

        Buffer payload = formatter.format(metrics);

        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertTrue(node.has("api"));
    }

    @Test
    public void shouldExcludeAllFields_butIncludeOneField() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setExcludeFields(Collections.singleton("*"));
        rules.setIncludeFields(Collections.singleton("api"));

        JsonFormatter<Metrics> formatter = new JsonFormatter<>(rules);

        Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        metrics.setApi("my-api");
        metrics.setApplication("my-application");
        metrics.setRemoteAddress("0.0.0.0");

        Buffer payload = formatter.format(metrics);

        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertEquals(1, node.size());
        Assert.assertTrue(node.has("api"));
    }

    @Test
    public void shouldExcludeNestedFields() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setExcludeFields(Collections.singleton("clientRequest"));

        JsonFormatter<Log> formatter = new JsonFormatter<>(rules);

        Log log = new Log(System.currentTimeMillis());
        log.setApi("my-api");
        log.setRequestId(UUID.random().toString());

        Request request = new Request();
        request.setUri("http://gravitee.io");
        log.setClientRequest(request);

        Response response = new Response();
        response.setStatus(HttpStatusCode.OK_200);

        log.setClientResponse(response);

        Buffer payload = formatter.format(log);

        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertEquals(4, node.size());
        Assert.assertFalse(node.has("clientRequest"));
    }

    @Test
    public void shouldExcludeNestedProperty() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setExcludeFields(Collections.singleton("clientRequest.uri"));

        JsonFormatter<Log> formatter = new JsonFormatter<>(rules);

        Log log = new Log(System.currentTimeMillis());
        log.setApi("my-api");
        log.setRequestId(UUID.random().toString());

        Request request = new Request();
        request.setUri("http://gravitee.io");
        log.setClientRequest(request);

        Response response = new Response();
        response.setStatus(HttpStatusCode.OK_200);

        log.setClientResponse(response);

        Buffer payload = formatter.format(log);
        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertEquals(5, node.size());
        Assert.assertTrue(node.has("clientRequest"));
        Assert.assertEquals(0, node.get("clientRequest").size());
    }

    @Test
    public void shouldRenameNestedPrgetExcludeFieldsoperty() throws JsonProcessingException {
        Rules rules = new Rules();
        rules.setRenameFields(Maps.<String, String>builder().put("clientRequest.uri", "path").build());

        JsonFormatter<Log> formatter = new JsonFormatter<>(rules);

        Log log = new Log(System.currentTimeMillis());
        log.setApi("my-api");
        log.setRequestId(UUID.random().toString());

        Request request = new Request();
        request.setUri("http://gravitee.io");
        log.setClientRequest(request);

        Response response = new Response();
        response.setStatus(HttpStatusCode.OK_200);

        log.setClientResponse(response);

        Buffer payload = formatter.format(log);
        JsonNode node = mapper.readTree(payload.toString());
        Assert.assertEquals(5, node.size());
        Assert.assertTrue(node.has("clientRequest"));
        Assert.assertEquals(1, node.get("clientRequest").size());
        Assert.assertFalse(node.get("clientRequest").has("uri"));
        Assert.assertTrue(node.get("clientRequest").has("path"));
    }
}
