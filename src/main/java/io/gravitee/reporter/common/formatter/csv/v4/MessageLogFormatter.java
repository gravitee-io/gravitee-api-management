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
package io.gravitee.reporter.common.formatter.csv.v4;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.common.formatter.csv.SingleValueFormatter;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageLogFormatter extends SingleValueFormatter<MessageLog> {

  private static final Logger LOG = LoggerFactory.getLogger(
    MessageLogFormatter.class
  );

  private final ObjectMapper mapper = new ObjectMapper();

  public MessageLogFormatter() {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  protected Buffer format0(MessageLog log) {
    final Buffer buffer = Buffer.buffer();

    appendString(buffer, log.getRequestId());
    appendString(buffer, log.getApiId());
    appendString(buffer, log.getApiName());
    appendString(buffer, log.getCorrelationId());
    appendString(buffer, log.getParentCorrelationId());
    appendString(
      buffer,
      log.getOperation() != null ? log.getOperation().name() : null
    );
    appendString(
      buffer,
      log.getConnectorType() != null ? log.getConnectorType().name() : null
    );
    appendString(buffer, log.getConnectorId());

    try {
      appendString(buffer, mapper.writeValueAsString(log.getMessage()));
    } catch (JsonProcessingException e) {
      LOG.error("Unable to process message", e);
    }

    return buffer;
  }
}
