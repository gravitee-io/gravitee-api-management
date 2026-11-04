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
package io.gravitee.apim.reporter.common.formatter.msgpack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.apim.reporter.common.formatter.AbstractFormatter;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.api.jackson.AdditionalMetricDeserialization;
import io.gravitee.reporter.api.jackson.HttpHeadersSerializer;
import io.gravitee.reporter.api.jackson.JacksonUtils;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.vertx.core.buffer.Buffer;
import lombok.CustomLog;
import org.msgpack.jackson.dataformat.MessagePackMapper;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class MsgPackFormatter<T extends Reportable> extends AbstractFormatter<T> {

    // Null without rules. The rules resolve nested paths from the generator's write context, which MessagePackGenerator
    // never updates: with rules, the data is filtered into a tree by the JSON mapper first, then the tree is encoded.
    private final ObjectMapper filteringMapper;

    private final ObjectMapper mapper = new MessagePackMapper();

    public MsgPackFormatter(final Rules rules) {
        filteringMapper = rules != null && rules.containsRules() ? JacksonUtils.mapper(rules) : null;

        SimpleModule module = new SimpleModule();
        module.addSerializer(HttpHeaders.class, new HttpHeadersSerializer(rules));
        module.addDeserializer(AdditionalMetric.class, new AdditionalMetricDeserialization());
        mapper.registerModule(module);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Buffer format0(T data) {
        try {
            if (filteringMapper == null) {
                return Buffer.buffer(mapper.writeValueAsBytes(data));
            }

            JsonNode tree = filteringMapper.valueToTree(data);
            if (tree.isEmpty()) {
                log.trace("Excluding data format in reporter: {}", tree);
                return null;
            }
            return Buffer.buffer(mapper.writeValueAsBytes(tree));
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.error("Unexpected error while formatting data", e);
            return null;
        }
    }
}
