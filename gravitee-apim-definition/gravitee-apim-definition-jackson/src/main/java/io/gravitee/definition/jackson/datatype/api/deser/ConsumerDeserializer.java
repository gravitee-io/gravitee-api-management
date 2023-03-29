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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import java.io.IOException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsumerDeserializer extends StdScalarDeserializer<Consumer> {

    public ConsumerDeserializer(Class<Consumer> vc) {
        super(vc);
    }

    @Override
    public Consumer deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Consumer consumer = new Consumer();

        consumer.setConsumerType(ConsumerType.valueOf(node.path("consumerType").asText(ConsumerType.TAG.name())));

        JsonNode consumerIdNode = node.get("consumerId");
        if (consumerIdNode == null) {
            throw ctxt.mappingException("Consumer ID property is required");
        } else {
            consumer.setConsumerId(consumerIdNode.asText());
        }

        return consumer;
    }
}
