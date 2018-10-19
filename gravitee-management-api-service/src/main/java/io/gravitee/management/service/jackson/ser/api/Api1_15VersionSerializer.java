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
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
            if (apiEntity.getProxy().getLogging() == null) {
                jsonGenerator.writeObjectField("loggingMode", LoggingMode.NONE);
            } else {
                jsonGenerator.writeObjectField("loggingMode", apiEntity.getProxy().getLogging().getMode());
            }
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



        // handle filtered fields list
        List<String> filteredFieldsList = (List<String>) apiEntity.getMetadata().get(METADATA_FILTERED_FIELDS_LIST);

        // members
        if (!filteredFieldsList.contains("members")) {
            Set<MemberEntity> memberEntities = applicationContext.getBean(MembershipService.class).getMembers(MembershipReferenceType.API, apiEntity.getId(), RoleScope.API);
            List<ApiSerializer.Member> members = new ArrayList<>(memberEntities == null ? 0 : memberEntities.size());
            if (memberEntities != null) {
                memberEntities.forEach(m -> {
                    UserEntity userEntity = applicationContext.getBean(UserService.class).findById(m.getId());
                    if (userEntity != null) {
                        Member member = new Member();
                        member.setUsername(userEntity.getSourceId());
                        member.setRole(m.getRole());
                        members.add(member);
                    }
                });
            }
            jsonGenerator.writeObjectField("members", members);
        }

        // must end the writing process
        jsonGenerator.writeEndObject();
    }

    @Override
    public Version version() {
        return Version.V_1_15;
    }
}
