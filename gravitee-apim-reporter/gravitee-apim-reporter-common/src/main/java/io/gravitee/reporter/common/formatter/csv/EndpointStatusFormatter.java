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
package io.gravitee.reporter.common.formatter.csv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.vertx.core.buffer.Buffer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointStatusFormatter
  extends SingleValueFormatter<EndpointStatus> {

  private static final Logger LOG = LoggerFactory.getLogger(
    EndpointStatusFormatter.class
  );

  private final ObjectMapper mapper = new ObjectMapper();

  public EndpointStatusFormatter() {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  public Buffer format0(EndpointStatus status) {
    final Buffer buffer = Buffer.buffer();

    appendString(buffer, status.getId());
    appendString(buffer, status.getApi());
    appendString(buffer, status.getApiName());
    appendString(buffer, status.getEndpoint());
    appendInt(buffer, status.getState());
    appendBoolean(buffer, status.isAvailable());
    appendBoolean(buffer, status.isSuccess());
    appendBoolean(buffer, status.isTransition());
    appendLong(buffer, status.getResponseTime());
    append(buffer, status.getSteps());

    return buffer;
  }

  private void append(Buffer buffer, List<Step> steps) {
    if (steps != null && !steps.isEmpty()) {
      Step last = steps.get(0);
      appendString(buffer, last.getName());
      appendBoolean(buffer, last.isSuccess());
      appendLong(buffer, last.getResponseTime());
      appendString(buffer, last.getMessage(), true);

      try {
        appendString(buffer, mapper.writeValueAsString(last.getRequest()));
      } catch (JsonProcessingException e) {
        LOG.error("Unexpected error while writing request as JSON", e);
      }

      try {
        appendString(buffer, mapper.writeValueAsString(last.getResponse()));
      } catch (JsonProcessingException e) {
        LOG.error("Unexpected error while writing response as JSON", e);
      }
    } else {
      appendEmpty(buffer);
      appendEmpty(buffer);
      appendEmpty(buffer);
      appendEmpty(buffer);
      appendEmpty(buffer);
      appendEmpty(buffer);
    }
  }
}
