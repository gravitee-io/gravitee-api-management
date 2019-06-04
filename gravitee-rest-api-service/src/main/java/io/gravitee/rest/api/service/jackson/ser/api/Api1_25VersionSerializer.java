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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.ResponseTemplates;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.PlanService;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api1_25VersionSerializer extends ApiSerializer {

    public Api1_25VersionSerializer() {
        super(ApiEntity.class);
    }

    @Override
    public void serialize(ApiEntity apiEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(apiEntity, jsonGenerator, serializerProvider);

        // path mappings part
        if (apiEntity.getPathMappings() != null) {
            jsonGenerator.writeArrayFieldStart("path_mappings");
            apiEntity.getPathMappings().forEach(pathMapping -> {
                try {
                    jsonGenerator.writeObject(pathMapping);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            jsonGenerator.writeEndArray();
        }

        // proxy part
        if (apiEntity.getProxy() != null) {
            jsonGenerator.writeObjectField("proxy", apiEntity.getProxy());
        }

        // response templates
        if (apiEntity.getResponseTemplates() != null) {
            jsonGenerator.writeObjectFieldStart("response_templates");
            for(Map.Entry<String, ResponseTemplates> rt : apiEntity.getResponseTemplates().entrySet()) {
                jsonGenerator.writeObjectFieldStart(rt.getKey());
                for(Map.Entry<String, ResponseTemplate> entry : rt.getValue().getTemplates().entrySet()) {
                    jsonGenerator.writeObjectField(entry.getKey(), entry.getValue());
                }
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        }
        
        // handle filtered fields list
        List<String> filteredFieldsList = (List<String>) apiEntity.getMetadata().get(METADATA_FILTERED_FIELDS_LIST);

        //plans
        if (!filteredFieldsList.contains("plans")) {
            Set<PlanEntity> plans = applicationContext.getBean(PlanService.class).findByApi(apiEntity.getId());
            Set<PlanEntityBefore_3_00> plansToAdd = plans == null
                    ? Collections.emptySet()
                    : plans.stream()
                    .filter(p -> !PlanStatus.CLOSED.equals(p.getStatus()))
                    .map(PlanEntityBefore_3_00::fromNewPlanEntity)
                    .collect(Collectors.toSet());
            jsonGenerator.writeObjectField("plans", plansToAdd);
        }

        // must end the writing process
        jsonGenerator.writeEndObject();
    }

    @Override
    public Version version() {
        return Version.V_1_25;
    }
}
