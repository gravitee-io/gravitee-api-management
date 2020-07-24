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
import io.gravitee.definition.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer.Member;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
            jsonGenerator.writeObjectFieldStart("proxy");

            // We assume that the API is containing a single virtual host
            Iterator<VirtualHost> virtualHostIterator = apiEntity.getProxy().getVirtualHosts().iterator();
            if (virtualHostIterator.hasNext()) {
                jsonGenerator.writeObjectField("context_path", virtualHostIterator.next().getPath());
            }

            jsonGenerator.writeObjectField("strip_context_path", apiEntity.getProxy().isStripContextPath());
            if (apiEntity.getProxy().getLogging() != null) {
                jsonGenerator.writeObjectField("logging", apiEntity.getProxy().getLogging());
            }

            jsonGenerator.writeArrayFieldStart("groups");
            apiEntity.getProxy().getGroups().forEach(new Consumer<EndpointGroup>() {
                @Override
                public void accept(EndpointGroup endpointGroup) {
                    try {
                        if (endpointGroup.getEndpoints() != null) {
                            endpointGroup.setEndpoints(endpointGroup.getEndpoints()
                                    .stream()
                                    .filter(endpoint -> endpoint.getType() == EndpointType.HTTP)
                                    .collect(Collectors.toSet()));
                        }

                        jsonGenerator.writeObject(endpointGroup);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            jsonGenerator.writeEndArray();

            if (apiEntity.getProxy().getFailover() != null) {
                jsonGenerator.writeObjectField("failover", apiEntity.getProxy().getFailover());
            }

            if (apiEntity.getProxy().getCors() != null) {
                jsonGenerator.writeObjectField("cors", apiEntity.getProxy().getCors());
            }

            jsonGenerator.writeEndObject();
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

        // members
        if (!filteredFieldsList.contains("members")) {
            Set<MemberEntity> memberEntities = applicationContext.getBean(MembershipService.class).getMembersByReference(MembershipReferenceType.API, apiEntity.getId());
            List<Member> members = new ArrayList<>(memberEntities == null ? 0 : memberEntities.size());
            if (memberEntities != null) {
                memberEntities.forEach(m -> {
                    UserEntity userEntity = applicationContext.getBean(UserService.class).findById(m.getId());
                    if (userEntity != null) {
                        Member member = new Member();
                        member.setRole(m.getRoles().get(0).getName());
                        member.setSource(userEntity.getSource());
                        member.setSourceId(userEntity.getSourceId());
                        members.add(member);
                    }
                });
            }
            jsonGenerator.writeObjectField("members", members);
        }
       
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
