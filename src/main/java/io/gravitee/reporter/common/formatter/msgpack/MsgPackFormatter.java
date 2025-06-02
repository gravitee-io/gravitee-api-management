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
package io.gravitee.reporter.common.formatter.msgpack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.gravitee.reporter.api.jackson.*;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.common.formatter.AbstractFormatter;
import io.vertx.core.buffer.Buffer;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MsgPackFormatter<T extends Reportable>
  extends AbstractFormatter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(
    MsgPackFormatter.class
  );

  private final ObjectMapper mapper = new MessagePackMapper();

  public MsgPackFormatter(final Rules rules) {
    if (rules != null && rules.containsRules()) {
      mapper.addMixIn(Reportable.class, FieldFilterMixin.class);
      mapper.addMixIn(Request.class, FieldFilterMixin.class);
      mapper.addMixIn(Response.class, FieldFilterMixin.class);
      mapper.addMixIn(EndpointStatus.class, FieldFilterMixin.class);
      mapper.addMixIn(Step.class, FieldFilterMixin.class);
      mapper.setFilterProvider(new FieldFilterProvider(rules));
    }

    SimpleModule module = new SimpleModule();
    module.addSerializer(HttpHeaders.class, new HttpHeadersSerializer(rules));
    module.addDeserializer(
      AdditionalMetric.class,
      new AdditionalMetricDeserialization()
    );
    mapper.registerModule(module);

    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  public Buffer format0(T data) {
    try {
      return Buffer.buffer(mapper.writeValueAsBytes(data));
    } catch (JsonProcessingException e) {
      LOG.error("Unexpected error while formatting data", e);
      return null;
    }
  }
}
