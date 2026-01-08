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
package io.gravitee.apim.reporter.common.formatter.csv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.reporter.api.log.Log;
import io.vertx.core.buffer.Buffer;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class LogFormatter extends SingleValueFormatter<Log> {

    private final ObjectMapper mapper = new ObjectMapper();

    public LogFormatter() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Buffer format0(Log log) {
        final Buffer buffer = Buffer.buffer();

        appendString(buffer, log.getRequestId());
        appendString(buffer, log.getApi());
        appendString(buffer, log.getApiName());

        try {
            appendString(buffer, mapper.writeValueAsString(log.getClientRequest()));
        } catch (JsonProcessingException e) {
            LogFormatter.log.error("Unable to process client request", e);
        }
        try {
            appendString(buffer, mapper.writeValueAsString(log.getClientResponse()));
        } catch (JsonProcessingException e) {
            LogFormatter.log.error("Unable to process client response", e);
        }
        try {
            appendString(buffer, mapper.writeValueAsString(log.getProxyRequest()));
        } catch (JsonProcessingException e) {
            LogFormatter.log.error("Unable to process proxy request", e);
        }
        try {
            appendString(buffer, mapper.writeValueAsString(log.getProxyResponse()));
        } catch (JsonProcessingException e) {
            LogFormatter.log.error("Unable to process proxy response", e);
        }

        return buffer;
    }
}
