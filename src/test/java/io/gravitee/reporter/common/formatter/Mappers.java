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
package io.gravitee.reporter.common.formatter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.gateway.api.http.DefaultHttpHeaders;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.jackson.AdditionalMetricDeserialization;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.msgpack.jackson.dataformat.MessagePackMapper;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Mappers {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final JsonLinesMapper JSON_LINES = new JsonLinesMapper();
  public static final ObjectMapper MESSAGE_PACK = new MessagePackMapper();

  static {
    var resolver = new SimpleAbstractTypeResolver();
    resolver.addMapping(HttpHeaders.class, DefaultHttpHeaders.class);
    var module = new SimpleModule();
    module.addDeserializer(
      AdditionalMetric.class,
      new AdditionalMetricDeserialization()
    );
    module.setAbstractTypes(resolver);
    JSON.registerModule(module);
    JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static class JsonLinesMapper {

    public List<JsonNode> readLines(byte[] jsonLines) throws IOException {
      return JSON
        .readerFor(JsonNode.class)
        .readValues(jsonLines)
        .readAll()
        .stream()
        .map(JsonNode.class::cast)
        .collect(Collectors.toList());
    }

    public List<JsonNode> readLines(String jsonLines) throws IOException {
      return readLines(jsonLines.getBytes());
    }
  }
}
