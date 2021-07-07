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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.gravitee.reporter.api.jackson.FieldFilterMixin;
import io.gravitee.reporter.api.jackson.FieldFilterProvider;
import io.gravitee.reporter.file.formatter.AbstractFormatter;
import io.vertx.core.buffer.Buffer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonFormatter<T extends Reportable> extends AbstractFormatter<T> {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonFormatter(final Rules rules) {
        mapper.addMixIn(Reportable.class, FieldFilterMixin.class);
        mapper.addMixIn(Request.class, FieldFilterMixin.class);
        mapper.addMixIn(Response.class, FieldFilterMixin.class);
        mapper.addMixIn(EndpointStatus.class, FieldFilterMixin.class);
        mapper.addMixIn(Step.class, FieldFilterMixin.class);
        mapper.setFilterProvider(new FieldFilterProvider(rules));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Buffer format0(T data) {
        try {
            return Buffer.buffer(mapper.writeValueAsBytes(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
