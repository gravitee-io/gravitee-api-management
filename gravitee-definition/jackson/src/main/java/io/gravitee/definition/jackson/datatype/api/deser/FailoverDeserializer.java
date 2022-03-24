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
import io.gravitee.definition.model.Failover;
import io.gravitee.definition.model.FailoverCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FailoverDeserializer extends StdScalarDeserializer<Failover> {

    public FailoverDeserializer(Class<Failover> vc) {
        super(vc);
    }

    @Override
    public Failover deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Failover failover = new Failover();

        final JsonNode casesNode = node.get("cases");
        if (casesNode != null) {
            if (casesNode.isArray()) {
                List<FailoverCase> cases = new ArrayList<>();

                casesNode.elements().forEachRemaining(jsonNode ->
                        cases.add(FailoverCase.valueOf(jsonNode.asText().toUpperCase())));

                failover.setCases(cases.toArray(new FailoverCase[cases.size()]));
            } else {
                failover.setCases(new FailoverCase[] {FailoverCase.valueOf(casesNode.asText().toUpperCase())});
            }
        }

        JsonNode maxAttemptsNode = node.get("maxAttempts");
        if (maxAttemptsNode != null) {
            int maxAttempts = maxAttemptsNode.asInt(Failover.DEFAULT_MAX_ATTEMPTS);
            failover.setMaxAttempts(maxAttempts);
        } else {
            failover.setMaxAttempts(Failover.DEFAULT_MAX_ATTEMPTS);
        }

        JsonNode retryTimeoutNode = node.get("retryTimeout");
        if (retryTimeoutNode != null) {
            long retryTimeout = retryTimeoutNode.asLong(Failover.DEFAULT_RETRY_TIMEOUT);
            failover.setRetryTimeout(retryTimeout);
        } else {
            failover.setRetryTimeout(Failover.DEFAULT_RETRY_TIMEOUT);
        }

        return failover;
    }
}