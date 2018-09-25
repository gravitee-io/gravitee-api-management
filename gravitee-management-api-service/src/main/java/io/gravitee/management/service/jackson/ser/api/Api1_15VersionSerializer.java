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
package io.gravitee.management.service.jackson.ser.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.management.model.api.ApiEntity;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api1_15VersionSerializer extends ApiSerializer {

    public Api1_15VersionSerializer() {
        super(ApiEntity.class);
    }

    @Override
    public void serialize(ApiEntity apiEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(apiEntity, jsonGenerator, serializerProvider);

        // proxy part
        if (apiEntity.getProxy() != null) {
            jsonGenerator.writeObjectFieldStart("proxy");
            jsonGenerator.writeObjectField("context_path", apiEntity.getProxy().getContextPath());
            jsonGenerator.writeObjectField("strip_context_path", apiEntity.getProxy().isStripContextPath());
            jsonGenerator.writeObjectField("loggingMode", apiEntity.getProxy().getLoggingMode());
            jsonGenerator.writeObjectField("endpoints", apiEntity.getProxy().getGroups().stream()
                    .map(endpointGroup -> endpointGroup.getEndpoints())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));

            // load balancing (get load balancing of the first endpoints group)
            jsonGenerator.writeObjectField("load_balancing", apiEntity.getProxy().getGroups().iterator().next().getLoadBalancer());

            if (apiEntity.getProxy().getFailover() != null) {
                jsonGenerator.writeObjectField("failover", apiEntity.getProxy().getFailover());
            }

            if (apiEntity.getProxy().getCors() != null) {
                jsonGenerator.writeObjectField("cors", apiEntity.getProxy().getCors());
            }

            jsonGenerator.writeEndObject();
        }

        // must end the writing process
        jsonGenerator.writeEndObject();
    }

    @Override
    public Version version() {
        return Version.V_1_15;
    }
}
