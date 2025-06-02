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
package io.gravitee.reporter.common.formatter.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.api.jackson.*;
import io.gravitee.reporter.common.formatter.AbstractFormatter;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonFormatter<T extends Reportable> extends AbstractFormatter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(
    JsonFormatter.class
  );

  private final ObjectMapper mapper;

  public JsonFormatter(final Rules rules) {
    mapper = JacksonUtils.mapper(rules);
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
