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
package io.gravitee.rest.api.service.jackson.ser.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api3_7VersionSerializer extends ApiSerializer {

    public Api3_7VersionSerializer() {
        super(ApiEntity.class);
    }

    @Override
    public void serialize(ApiEntity apiEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(apiEntity, jsonGenerator, serializerProvider);

        if (apiEntity.getId() != null) {
            jsonGenerator.writeStringField("id", apiEntity.getId());
        }

        // path mappings part
        if (apiEntity.getPathMappings() != null) {
            jsonGenerator.writeArrayFieldStart("path_mappings");
            apiEntity
                .getPathMappings()
                .forEach(
                    pathMapping -> {
                        try {
                            jsonGenerator.writeObject(pathMapping);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );
            jsonGenerator.writeEndArray();
        }

        // proxy part
        if (apiEntity.getProxy() != null) {
            Set<EndpointGroup> groups = apiEntity.getProxy().getGroups();
            if (groups != null) {
                groups.forEach(
                    grp -> {
                        if (grp.getEndpoints() != null) {
                            grp.setEndpoints(
                                grp
                                    .getEndpoints()
                                    .stream()
                                    .filter(endpoint -> endpoint.getType() == EndpointType.HTTP)
                                    .collect(Collectors.toSet())
                            );
                        }
                    }
                );
            }
            jsonGenerator.writeObjectField("proxy", apiEntity.getProxy());
        }

        // response templates
        if (apiEntity.getResponseTemplates() != null) {
            jsonGenerator.writeObjectFieldStart("response_templates");
            for (Map.Entry<String, Map<String, ResponseTemplate>> rt : apiEntity.getResponseTemplates().entrySet()) {
                jsonGenerator.writeObjectFieldStart(rt.getKey());
                for (Map.Entry<String, ResponseTemplate> entry : rt.getValue().entrySet()) {
                    jsonGenerator.writeObjectField(entry.getKey(), entry.getValue());
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        }

        if (apiEntity.getPrimaryOwner() != null) {
            jsonGenerator.writeObjectField("primaryOwner", apiEntity.getPrimaryOwner());
        }

        // must end the writing process
        jsonGenerator.writeEndObject();
    }

    @Override
    public Version version() {
        return Version.V_3_7;
    }
}
